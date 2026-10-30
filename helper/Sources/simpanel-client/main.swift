// A stand-in for the plugin: connects to a running helper, saves the frames it sends and
// taps the screen.
//
// simpanel-client --socket <path> --out <dir> [--viewport 400x870] [--seconds 3] [--tap x,y]
//                 [--swipe x1,y1,x2,y2] [--button home|power|volume-up|volume-down|action|siri]
//                 [--presses 1] [--gap 0] [--scroll x,y,dx,dy] [--scrolls 1]
//                 [--keys S11,A8,C25] [--keep]

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
let scroll = numbers(option("--scroll"), separator: ",")
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

let keep = CommandLine.arguments.contains("--keep")
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

/// Writes `--scrolls` scrolls a frame apart, each by the whole of `--scroll`'s distance.
func sendScrolls(_ values: [Double]) throws {
    let count = Int(option("--scrolls") ?? "1") ?? 1
    for _ in 0..<count {
        let scroll = Scroll(x: values[0], y: values[1], dx: values[2], dy: values[3])
        try connection.write(PluginMessage.scroll(scroll).encoded())
        usleep(16_000)
    }
    print("scrolled \(count) times by \(values[2]), \(values[3])")
}

/// Reads what the helper sent within `timeout` seconds, saving the frames; false once it closes.
@MainActor func receive(timeout: Double) throws -> Bool {
    guard connection.waitForData(timeout: timeout) else { return true }
    guard let data = try connection.read() else {
        print("helper closed the connection")
        return false
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
            if keep {
                let name = String(format: "frame-%03d-%.2fs.jpg", frames, elapsed())
                try jpeg.write(to: out.appendingPathComponent(name))
            }
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
    return true
}

/// Presses each of `--keys`' usages `--gap` milliseconds apart, holding Shift around those
/// written with an S, Option around those written with an A and Command around those written
/// with a C; one written with a D only goes down, and one with a U only comes up. Frames keep
/// arriving between the presses.
@MainActor func sendKeys(_ list: String) throws {
    for item in list.split(separator: ",") {
        let prefix = item.prefix(while: \.isLetter)
        let modifiers = prefix.compactMap { ["S": UInt32(0xe1), "A": 0xe2, "C": 0xe3][$0] }
        guard let usage = UInt32(item.drop(while: \.isLetter)) else { fail("bad key \(item)") }
        let downs = modifiers.map { ($0, true) } + [(usage, true)]
        let ups = [(usage, false)] + modifiers.reversed().map { ($0, false) }
        let presses = prefix.contains("D") ? downs : prefix.contains("U") ? ups : downs + ups
        for (usage, down) in presses {
            try connection.write(PluginMessage.key(Key(usage: usage, down: down)).encoded())
        }
        let resume = elapsed() + Double(gap) / 1_000_000
        while elapsed() < resume, try receive(timeout: 0.005) {}
    }
    print("typed \(list)")
}

let keys = option("--keys")
let acts = tap.count == 2 || button != nil || scroll.count == 4 || keys != nil
var deadline = seconds + (acts ? 1 : 0)
while elapsed() < deadline {
    if !tapped, acts, elapsed() >= seconds {
        tapped = true
        if tap.count == 2 { try sendTap(tap) }
        if let button { try press(button) }
        if scroll.count == 4 { try sendScrolls(scroll) }
        if let keys { try sendKeys(keys) }
        deadline = elapsed() + 1
    }
    while let next = touches.first, next.at <= elapsed() {
        try connection.write(PluginMessage.touch(next.touch).encoded())
        touches.removeFirst()
    }
    guard try receive(timeout: 0.005) else { break }
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
