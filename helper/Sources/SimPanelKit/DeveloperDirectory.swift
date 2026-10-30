import Foundation

/// The Xcode developer directory: `DEVELOPER_DIR` when set, otherwise `xcode-select -p`.
public func developerDirectory() -> String {
    if let directory = ProcessInfo.processInfo.environment["DEVELOPER_DIR"] { return directory }
    let process = Process()
    let pipe = Pipe()
    process.executableURL = URL(fileURLWithPath: "/usr/bin/xcode-select")
    process.arguments = ["-p"]
    process.standardOutput = pipe
    process.standardError = FileHandle.nullDevice
    guard (try? process.run()) != nil else { return "" }
    process.waitUntilExit()
    let output = pipe.fileHandleForReading.readDataToEndOfFile()
    return String(decoding: output, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines)
}
