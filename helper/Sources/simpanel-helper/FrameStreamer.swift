import Foundation
import SimBridge
import SimPanelKit

private let BootCheckInterval = DispatchTimeInterval.seconds(1)

/// Sends a frame whenever the framebuffer changed, at most as often as the settings allow, and
/// stops once the simulator shuts down.
final class FrameStreamer: @unchecked Sendable {
    private struct FrameKey: Equatable {
        let surface: IOSurfaceID
        let seed: UInt32
        let scale: Double
    }

    private let simulator: SPSimulator
    private let connection: Connection
    private let onClosed: @Sendable () -> Void
    private let onShutDown: @Sendable () -> Void
    private let queue = DispatchQueue(label: "simpanel.frames")
    private let encoder = JPEGEncoder()
    private var timer: DispatchSourceTimer?
    private var bootCheck: DispatchSourceTimer?
    private var settings = StreamSettings.standard
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
        onClosed: @escaping @Sendable () -> Void,
        onShutDown: @escaping @Sendable () -> Void
    ) {
        self.simulator = simulator
        self.connection = connection
        self.verbose = verbose
        self.onClosed = onClosed
        self.onShutDown = onShutDown
    }

    /// Call once, before the first `apply`.
    func start() {
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.setEventHandler { [weak self] in self?.tick() }
        self.timer = timer
        scheduleFrames()
        timer.resume()
        let bootCheck = DispatchSource.makeTimerSource(queue: queue)
        bootCheck.schedule(deadline: .now() + BootCheckInterval, repeating: BootCheckInterval)
        bootCheck.setEventHandler { [weak self] in self?.checkBooted() }
        bootCheck.resume()
        self.bootCheck = bootCheck
    }

    func setViewport(_ viewport: Viewport) {
        queue.async { self.viewport = viewport }
    }

    func apply(_ settings: StreamSettings) {
        queue.async {
            let settings = settings.clamped
            let previous = self.settings
            self.settings = settings
            // Sends the screen again at the new quality without waiting for it to change.
            if settings.jpegQuality != previous.jpegQuality { self.lastSent = nil }
            if settings.maxFps != previous.maxFps { self.scheduleFrames() }
        }
    }

    private func scheduleFrames() {
        timer?.schedule(deadline: .now(), repeating: .milliseconds(1000 / settings.maxFps))
    }

    private func checkBooted() {
        guard !simulator.isBooted else { return }
        stop()
        onShutDown()
    }

    private func stop() {
        timer?.cancel()
        bootCheck?.cancel()
    }

    private func tick() {
        stats.ticks += 1
        if verbose { logStats() }
        guard let surface = simulator.framebuffer else { return }
        let scale = frameScale(width: surface.width, height: surface.height, viewport: viewport)
        let key = FrameKey(surface: IOSurfaceGetID(surface), seed: surface.seed, scale: scale)
        guard key != lastSent else { return }
        let encodeStart = DispatchTime.now().uptimeNanoseconds
        guard let jpeg = encoder.encode(surface, scale: scale, quality: settings.jpegQuality) else { return }
        stats.encodeNanoseconds += DispatchTime.now().uptimeNanoseconds - encodeStart
        let writeStart = DispatchTime.now().uptimeNanoseconds
        do {
            try connection.write(HelperMessage.frame(jpeg).encoded())
            stats.writeNanoseconds += DispatchTime.now().uptimeNanoseconds - writeStart
            lastSent = key
            stats.sent += 1
        } catch {
            stop()
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
