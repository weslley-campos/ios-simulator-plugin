import Foundation
import SimBridge
import SimPanelKit

private let FrameInterval = DispatchTimeInterval.milliseconds(17)

/// Plays a `ScrollGesture` one frame at a time, ticking only while there is something to scroll.
final class Scroller: @unchecked Sendable {
    private let simulator: SPSimulator
    private let queue = DispatchQueue(label: "simpanel.scroll")
    private var gesture: ScrollGesture
    private var timer: DispatchSourceTimer?

    init(simulator: SPSimulator) {
        self.simulator = simulator
        gesture = ScrollGesture(screen: .zero)
    }

    func scroll(_ scroll: Scroll) {
        queue.async { [self] in
            gesture.scroll(scroll, at: now(), screen: screen())
            guard timer == nil else { return }
            let timer = DispatchSource.makeTimerSource(queue: queue)
            timer.schedule(deadline: .now(), repeating: FrameInterval)
            timer.setEventHandler { [weak self] in self?.tick() }
            timer.resume()
            self.timer = timer
        }
    }

    /// Lifts a scrolling finger, so that a touch of the plugin's own can take over.
    func interrupt() {
        queue.sync { gesture.interrupt { $0.send(to: simulator) } }
    }

    private func tick() {
        gesture.tick(at: now()) { $0.send(to: simulator) }
        guard gesture.isIdle else { return }
        timer?.cancel()
        timer = nil
    }

    private func now() -> Double {
        Double(DispatchTime.now().uptimeNanoseconds) / 1e9
    }

    private func screen() -> CGSize {
        guard let surface = simulator.framebuffer, simulator.screenScale > 0 else { return .zero }
        return CGSize(
            width: Double(surface.width) / simulator.screenScale,
            height: Double(surface.height) / simulator.screenScale
        )
    }
}
