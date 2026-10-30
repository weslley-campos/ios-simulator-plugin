// Milestone 1: proves the framebuffer and HID routes on this Xcode before the real helper.
//
// simpanel-spike --udid <udid> --out <dir> [--setup-tap x,y] [--tap x,y] [--frames 60]

import CoreImage
import Foundation
import SimBridge

func option(_ name: String) -> String? {
    let arguments = CommandLine.arguments
    guard let index = arguments.firstIndex(of: name), index + 1 < arguments.count else {
        return nil
    }
    return arguments[index + 1]
}

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data("error: \(message)\n".utf8))
    exit(1)
}

func developerDir() -> String {
    if let dir = ProcessInfo.processInfo.environment["DEVELOPER_DIR"] { return dir }
    let process = Process()
    let pipe = Pipe()
    process.executableURL = URL(fileURLWithPath: "/usr/bin/xcode-select")
    process.arguments = ["-p"]
    process.standardOutput = pipe
    try? process.run()
    process.waitUntilExit()
    let output = pipe.fileHandleForReading.readDataToEndOfFile()
    return String(decoding: output, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines)
}

func fourCC(_ value: OSType) -> String {
    let bytes = [24, 16, 8, 0].map { UInt8((value >> $0) & 0xff) }
    return String(decoding: bytes, as: UTF8.self)
}

func milliseconds(since start: UInt64) -> Double {
    Double(DispatchTime.now().uptimeNanoseconds - start) / 1_000_000
}

final class Encoder {
    private let context = CIContext(options: [.cacheIntermediates: false])
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!

    func jpeg(_ surface: IOSurface, scale: CGFloat, quality: CGFloat) -> Data? {
        var image = CIImage(ioSurface: surface)
        if scale != 1 {
            image = image.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
        }
        let options = [
            CIImageRepresentationOption(
                rawValue: kCGImageDestinationLossyCompressionQuality as String
            ): quality,
        ]
        return context.jpegRepresentation(of: image, colorSpace: colorSpace, options: options)
    }
}

struct InputStep {
    let at: Double
    let send: (SPSimulator) -> Void
}

func swipe(from start: CGPoint, to end: CGPoint, at: Double, duration: Double) -> [InputStep] {
    let steps = 20
    var events = [InputStep(at: at) { _ = $0.touch(.down, x: start.x, y: start.y) }]
    for step in 1...steps {
        let t = Double(step) / Double(steps)
        let x = start.x + (end.x - start.x) * t
        let y = start.y + (end.y - start.y) * t
        events.append(InputStep(at: at + duration * t) { _ = $0.touch(.move, x: x, y: y) })
    }
    events.append(InputStep(at: at + duration + 0.02) { _ = $0.touch(.up, x: end.x, y: end.y) })
    return events
}

guard let udid = option("--udid") else { fail("--udid is required") }
let out = URL(fileURLWithPath: option("--out") ?? "frames")
let frameLimit = Int(option("--frames") ?? "60") ?? 60
func point(_ value: String?) -> [Double] {
    (value ?? "").split(separator: ",").compactMap { Double($0) }
}

let setupTap = point(option("--setup-tap"))
let finalTap = point(option("--tap") ?? "0.091,0.915")
try? FileManager.default.createDirectory(at: out, withIntermediateDirectories: true)

let attachStart = DispatchTime.now().uptimeNanoseconds
let simulator: SPSimulator
do {
    simulator = try SPSimulator(udid: udid, developerDir: developerDir())
} catch {
    fail("attach failed: \(error.localizedDescription) (\((error as NSError).code))")
}
guard let first = simulator.framebuffer else { fail("no framebuffer surface yet") }
print("attached to \(simulator.name) in \(String(format: "%.0f", milliseconds(since: attachStart))) ms")
print("framebuffer \(first.width) x \(first.height), \(fourCC(first.pixelFormat)), seed \(first.seed)")

let encoder = Encoder()
for scale in [1.0, 0.5] {
    let runs = 30
    let start = DispatchTime.now().uptimeNanoseconds
    var bytes = 0
    for _ in 0..<runs {
        bytes += encoder.jpeg(first, scale: scale, quality: 0.7)?.count ?? 0
    }
    let perFrame = milliseconds(since: start) / Double(runs)
    print(String(
        format: "encode at %.1fx: %.1f ms a frame (%.0f fps ceiling), %d KB",
        scale, perFrame, 1000 / perFrame, bytes / runs / 1024
    ))
}

do {
    try simulator.connectInput()
    print("HID client connected")
} catch {
    fail("input failed: \(error.localizedDescription)")
}

func tap(_ point: [Double], on simulator: SPSimulator) {
    guard point.count == 2 else { return }
    _ = simulator.touch(.down, x: point[0], y: point[1])
    usleep(60_000)
    _ = simulator.touch(.up, x: point[0], y: point[1])
    print("tapped at \(point[0]), \(point[1])")
}

if !setupTap.isEmpty {
    tap(setupTap, on: simulator)
    usleep(800_000)
}

// Scroll the list up and back down while counting the frames the simulator draws.
var steps = swipe(from: CGPoint(x: 0.5, y: 0.7), to: CGPoint(x: 0.5, y: 0.3), at: 0.3, duration: 0.4)
steps += swipe(from: CGPoint(x: 0.5, y: 0.3), to: CGPoint(x: 0.5, y: 0.7), at: 1.6, duration: 0.4)

let liveStart = DispatchTime.now().uptimeNanoseconds
var lastSeed = first.seed
var lastSurface = IOSurfaceGetID(first)
var changes: [Double] = []
var encodeTimes: [Double] = []
var saved = 0
var checkpoints = [0.29, 1.5, 2.95]
var latest: Data?
while milliseconds(since: liveStart) < 3000 {
    let now = milliseconds(since: liveStart) / 1000
    if let checkpoint = checkpoints.first, now >= checkpoint, let latest {
        try? latest.write(to: out.appendingPathComponent(String(format: "at-%.2fs.jpg", checkpoint)))
        checkpoints.removeFirst()
    }
    while let step = steps.first, step.at <= now {
        step.send(simulator)
        steps.removeFirst()
    }
    if let surface = simulator.framebuffer,
       surface.seed != lastSeed || IOSurfaceGetID(surface) != lastSurface {
        lastSeed = surface.seed
        lastSurface = IOSurfaceGetID(surface)
        changes.append(now)
        let start = DispatchTime.now().uptimeNanoseconds
        let jpeg = encoder.jpeg(surface, scale: 0.5, quality: 0.7)
        encodeTimes.append(milliseconds(since: start))
        latest = jpeg
        if saved < frameLimit, let jpeg {
            try? jpeg.write(to: out.appendingPathComponent(String(format: "frame-%03d.jpg", saved)))
            saved += 1
        }
    }
    usleep(1000)
}

let moving = changes.filter { ($0 >= 0.3 && $0 <= 0.9) || ($0 >= 1.6 && $0 <= 2.2) }
print("frames drawn in 3 s: \(changes.count), \(String(format: "%.0f", Double(moving.count) / 1.2)) fps while scrolling")
if !encodeTimes.isEmpty {
    let average = encodeTimes.reduce(0, +) / Double(encodeTimes.count)
    print(String(format: "live encode at 0.5x: %.1f ms average, %d frames saved", average, saved))
}

if finalTap.count == 2 {
    tap(finalTap, on: simulator)
    sleep(1)
    if let surface = simulator.framebuffer, let jpeg = encoder.jpeg(surface, scale: 0.5, quality: 0.8) {
        try? jpeg.write(to: out.appendingPathComponent("after-tap.jpg"))
        print("saved after-tap.jpg")
    }
}
