// simpanel-helper --udid <udid> --socket <path> [--verbose]
//
// Listens on the socket, accepts one plugin connection, attaches to the simulator, then
// streams frames up and applies input coming down until the plugin or its parent goes away.

import Foundation
import SimBridge
import SimPanelKit

extension SPSimulator: @retroactive @unchecked Sendable {}

func log(_ message: String) {
    FileHandle.standardError.write(Data("simpanel-helper: \(message)\n".utf8))
}

func option(_ name: String) -> String? {
    let arguments = CommandLine.arguments
    guard let index = arguments.firstIndex(of: name), index + 1 < arguments.count else {
        return nil
    }
    return arguments[index + 1]
}

guard let udid = option("--udid"), let socketPath = option("--socket") else {
    log("usage: simpanel-helper --udid <udid> --socket <path> [--verbose]")
    exit(2)
}

@Sendable func finish(_ status: Int32) -> Never {
    unlink(socketPath)
    exit(status)
}

let parent = getppid()
let parentWatch = DispatchSource.makeProcessSource(
    identifier: parent,
    eventMask: .exit,
    queue: .global()
)
parentWatch.setEventHandler {
    log("parent \(parent) exited")
    finish(0)
}
parentWatch.resume()

let connection: Connection
do {
    let listener = try Connection.listen(at: socketPath)
    log("listening on \(socketPath)")
    connection = try Connection.accept(listener)
    close(listener)
} catch {
    log("\(error)")
    finish(1)
}

let simulator: SPSimulator
do {
    simulator = try SPSimulator(udid: udid, developerDir: developerDirectory())
    try simulator.connectInput()
    guard let surface = simulator.framebuffer else {
        throw HelperError(code: .framebufferUnavailable, message: "The display hasn't drawn yet")
    }
    let hello = Hello(
        udid: simulator.udid,
        name: simulator.name,
        width: surface.width,
        height: surface.height
    )
    try connection.write(HelperMessage.hello(hello).encoded())
    log("attached to \(simulator.name), \(surface.width) x \(surface.height)")
} catch {
    let failure = error as? HelperError
        ?? HelperError(code: ErrorCode(error), message: error.localizedDescription)
    log("\(failure.code.rawValue): \(failure.message)")
    try? connection.write(HelperMessage.error(failure).encoded())
    finish(1)
}

let streamer = FrameStreamer(
    simulator: simulator,
    connection: connection,
    verbose: CommandLine.arguments.contains("--verbose")
) {
    log("plugin stopped reading")
    finish(0)
}
streamer.start()

func clamp(_ value: Double) -> Double {
    min(1, max(0, value))
}

func handle(_ message: PluginMessage) {
    switch message {
    case .viewport(let viewport):
        streamer.setViewport(viewport)
    case .touch(let touch):
        let phase: SPTouchPhase = switch touch.phase {
        case .down: .down
        case .move: .move
        case .up: .up
        }
        simulator.touch(phase, x: clamp(touch.x), y: clamp(touch.y))
    case .button(let press):
        press.button.send(down: press.down, to: simulator)
    }
}

var decoder = FrameDecoder()
do {
    while let data = try connection.read() {
        decoder.append(data)
        while let (type, payload) = try decoder.next() {
            if let message = try PluginMessage.decode(type: type, payload: payload) {
                handle(message)
            }
        }
    }
    log("plugin disconnected")
    finish(0)
} catch {
    log("\(error)")
    finish(1)
}
