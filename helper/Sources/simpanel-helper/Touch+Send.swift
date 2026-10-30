import SimBridge
import SimPanelKit

extension Touch {
    /// Returns false when SimulatorKit drops a move that comes less than a frame after the last.
    @discardableResult
    func send(to simulator: SPSimulator) -> Bool {
        let phase: SPTouchPhase = switch phase {
        case .down: .down
        case .move: .move
        case .up: .up
        }
        return simulator.touch(phase, x: min(1, max(0, x)), y: min(1, max(0, y)))
    }
}
