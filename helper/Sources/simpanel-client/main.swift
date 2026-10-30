// A stand-in for the plugin: connects to a running helper, saves the frames it sends and
// taps the screen.
//
// simpanel-client --socket <path> --out <dir> [--viewport 400x870] [--seconds 3] [--tap x,y]
//                 [--swipe x1,y1,x2,y2] [--button home|power|volume-up|volume-down|action|siri]
//                 [--presses 1] [--gap 0]

import Foundation
import SimPanelKit

func option(_ name: String) -> String? {
    let arguments = CommandLine.arguments
    guard let index = arguments.firstIndex(of: name), index + 1 < arguments.count else {
        return nil
    }
    return arguments[index + 1]
}

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data("simpanel-client: \(message)\n".utf8))
    exit(1)
}

func numbers(_ value: String?, separator: Character) -> [Double] {
    (value ?? "").split(separator: separator).compactMap { Double($0) }
}

func connect(to path: String, within seconds: Double) throws -> Connection {
    let deadline = Date().addingTimeInterval(seconds)
    while true {
        do {
            return try Connection.connect(to: path)
        } catch {
            if Date() > deadline { throw error }
            usleep(50_000)
        }
    }
}

guard let socketPath = option("--socket") else { fail("--socket is required") }
let out = URL(fileURLWithPath: option("--out") ?? "frames")
let viewport = numbers(option("--viewport") ?? "400x870", separator: "x")
let seconds = Double(option("--seconds") ?? "3") ?? 3
let tap = numbers(option("--tap"), separator: ",")
let swipe = numbers(option("--swipe"), separator: ",")
let button = option("--button").map { name -> DeviceButton in
    guard let button = DeviceButton(rawValue: name) else { fail("unknown button \(name)") }
    return button
}
let swipeStart = 0.5
let swipeDuration = 0.4
try? FileManager.default.createDirectory(at: out, withIntermediateDirectories: true)

let connection: Connection
do {
    connection = try connect(to: socketPath, within: 5)
} catch {
    fail("\(error)")
}

if viewport.count == 2 {
    let message = PluginMessage.viewport(Viewport(width: Int(viewport[0]), height: Int(viewport[1])))
    try connection.write(message.encoded())
}

let start = Date()
var frames = 0
var bytes = 0
var tapped = false
var swipeFrameTimes: [Double] = []
var touches: [(at: Double, touch: Touch)] = []
if swipe.count == 4 {
    let steps = 24
    touches.append((swipeStart, Touch(phase: .down, x: swipe[0], y: swipe[1])))
    for step in 1...steps {
        let t = Double(step) / Double(steps)
        let x = swipe[0] + (swipe[2] - swipe[0]) * t
        let y = swipe[1] + (swipe[3] - swipe[1]) * t
        touches.append((swipeStart + swipeDuration * t, Touch(phase: .move, x: x, y: y)))
    }
    touches.append((swipeStart + swipeDuration + 0.02, Touch(phase: .up, x: swipe[2], y: swipe[3])))
}
var decoder = FrameDecoder()

func elapsed() -> Double { Date().timeIntervalSince(start) }

let presses = Int(option("--presses") ?? "1") ?? 1
let gap = useconds_t((Double(option("--gap") ?? "0") ?? 0) * 1000)

/// Writes the presses `--gap` milliseconds apart; with no gap, back to back.
func press(_ button: DeviceButton) throws {
    for _ in 0..<presses {
        for down in [true, false] {
            try connection.write(PluginMessage.button(ButtonPress(button: button, down: down)).encoded())
            usleep(gap)
        }
    }
    print("pressed \(button.rawValue) \(presses) times")
}

func sendTap(_ point: [Double]) throws {
    try connection.write(PluginMessage.touch(Touch(phase: .down, x: point[0], y: point[1])).encoded())
    usleep(60_000)
    try connection.write(PluginMessage.touch(Touch(phase: .up, x: point[0], y: point[1])).encoded())
    print("tapped at \(point[0]), \(point[1])")
}

let acts = tap.count == 2 || button != nil
while elapsed() < seconds + (acts ? 1 : 0) {
    if !tapped, acts, elapsed() >= seconds {
        if tap.count == 2 { try sendTap(tap) }
        if let button { try press(button) }
        tapped = true
    }
    while let next = touches.first, next.at <= elapsed() {
        try connection.write(PluginMessage.touch(next.touch).encoded())
        touches.removeFirst()
    }
    guard connection.waitForData(timeout: 0.005) else { continue }
    guard let data = try connection.read() else {
        print("helper closed the connection")
        break
    }
    decoder.append(data)
    while let (type, payload) = try decoder.next() {
        switch try HelperMessage.decode(type: type, payload: payload) {
        case .hello(let hello):
            print("hello: \(hello.name) \(hello.width) x \(hello.height) \(hello.orientation)")
        case .frame(let jpeg):
            frames += 1
            if elapsed() >= swipeStart { swipeFrameTimes.append(elapsed()) }
            bytes += jpeg.count
            try jpeg.write(to: out.appendingPathComponent(tapped ? "after-tap.jpg" : "latest.jpg"))
        case .error(let error):
            fail("\(error.code.rawValue): \(error.message)")
        case .chrome(let chrome):
            let buttons = chrome.buttons.map(\.button.rawValue).joined(separator: ", ")
            print(
                "chrome: \(chrome.width) x \(chrome.height) pt at \(chrome.scale)x,",
                "corners \(chrome.screenCornerRadius) pt, \(chrome.bezel.count / 1024) KB, \(buttons)"
            )
            try chrome.bezel.write(to: out.appendingPathComponent("bezel.png"))
        case nil:
            break
        }
    }
}

let average = frames > 0 ? bytes / frames / 1024 : 0
print(String(
    format: "%d frames in %.1f s, %.1f fps, %d KB average",
    frames, elapsed(), Double(frames) / elapsed(), average
))
if swipe.count == 4, let first = swipeFrameTimes.first, let last = swipeFrameTimes.last, last > first {
    let gaps = zip(swipeFrameTimes.dropFirst(), swipeFrameTimes).map { ($0 - $1) * 1000 }.sorted()
    print(String(
        format: "scroll: %d frames over %.2f s, %.1f fps, median gap %.0f ms, worst %.0f ms",
        swipeFrameTimes.count, last - first, Double(swipeFrameTimes.count - 1) / (last - first),
        gaps[gaps.count / 2], gaps.last ?? 0
    ))
}
