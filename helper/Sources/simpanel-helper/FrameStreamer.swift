import Foundation
import SimBridge
import SimPanelKit

private let MaxFps = 30
private let JPEGQuality = 0.7

/// Sends a frame whenever the framebuffer changed, at most `MaxFps` a second.
final class FrameStreamer: @unchecked Sendable {
    private struct FrameKey: Equatable {
        let surface: IOSurfaceID
        let seed: UInt32
        let scale: Double
    }

    private let simulator: SPSimulator
    private let connection: Connection
    private let onClosed: @Sendable () -> Void
    private let queue = DispatchQueue(label: "simpanel.frames")
    private let encoder = JPEGEncoder()
    private var timer: DispatchSourceTimer?
    private var viewport: Viewport?
    private var lastSent: FrameKey?
    private let verbose: Bool
    private var stats = Stats()

    private struct Stats {
        var started = DispatchTime.now()
        var ticks = 0
        var sent = 0
        var encodeNanoseconds: UInt64 = 0
        var writeNanoseconds: UInt64 = 0
    }

    init(
        simulator: SPSimulator,
        connection: Connection,
        verbose: Bool,
        onClosed: @escaping @Sendable () -> Void
    ) {
        self.simulator = simulator
        self.connection = connection
        self.verbose = verbose
        self.onClosed = onClosed
    }

    func start() {
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.schedule(deadline: .now(), repeating: .milliseconds(1000 / MaxFps))
        timer.setEventHandler { [weak self] in self?.tick() }
        timer.resume()
        self.timer = timer
    }

    func setViewport(_ viewport: Viewport) {
        queue.async { self.viewport = viewport }
    }

    private func tick() {
        stats.ticks += 1
        if verbose { logStats() }
        guard let surface = simulator.framebuffer else { return }
        let scale = frameScale(width: surface.width, height: surface.height, viewport: viewport)
        let key = FrameKey(surface: IOSurfaceGetID(surface), seed: surface.seed, scale: scale)
        guard key != lastSent else { return }
        let encodeStart = DispatchTime.now().uptimeNanoseconds
        guard let jpeg = encoder.encode(surface, scale: scale, quality: JPEGQuality) else { return }
        stats.encodeNanoseconds += DispatchTime.now().uptimeNanoseconds - encodeStart
        let writeStart = DispatchTime.now().uptimeNanoseconds
        do {
            try connection.write(HelperMessage.frame(jpeg).encoded())
            stats.writeNanoseconds += DispatchTime.now().uptimeNanoseconds - writeStart
            lastSent = key
            stats.sent += 1
        } catch {
            timer?.cancel()
            onClosed()
        }
    }

    private func logStats() {
        let seconds = Double(DispatchTime.now().uptimeNanoseconds - stats.started.uptimeNanoseconds) / 1e9
        guard seconds >= 0.5 else { return }
        let sent = Double(max(stats.sent, 1))
        log(String(
            format: "%.1f ticks/s, %.1f frames/s, %.1f ms encode, %.1f ms write",
            Double(stats.ticks) / seconds, Double(stats.sent) / seconds,
            Double(stats.encodeNanoseconds) / sent / 1e6, Double(stats.writeNanoseconds) / sent / 1e6
        ))
        stats = Stats()
    }
}
