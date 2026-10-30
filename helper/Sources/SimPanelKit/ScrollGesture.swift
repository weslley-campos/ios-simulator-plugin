import CoreGraphics
import Foundation

/// How far the content scrolls before the finger goes down, so that lifting it never taps.
private let Slop = 12.0
/// How long the finger holds still before it lifts; iOS still flings after a shorter hold.
private let Rest = 0.25
/// How far the finger wobbles while it holds; iOS flings after a hold that sends no moves.
private let Wobble = 0.5
/// How close the finger comes to the screen's edges, where iOS starts its system gestures.
private let EdgeMargin = 40.0

/// Turns scroll deltas into a finger dragging the screen, since an iPhone simulator takes no
/// scroll events. Points and deltas are fractions of the screen, sizes are in points and times
/// are in seconds. `tick` sends at most one touch, so that each one lands a frame apart.
public struct ScrollGesture: Sendable {
    private var screen: CGSize
    private var anchor = CGPoint(x: 0.5, y: 0.5)
    private var pending = CGVector.zero
    private var finger: CGPoint?
    private var lastScroll = 0.0
    private var lastMove = 0.0
    private var held = false

    public init(screen: CGSize) {
        self.screen = screen
    }

    public var isIdle: Bool { finger == nil && pending == .zero }

    public mutating func scroll(_ scroll: Scroll, at time: Double, screen: CGSize) {
        self.screen = screen
        if finger == nil { anchor = inset(CGPoint(x: scroll.x, y: scroll.y)) }
        pending.dx += scroll.dx
        pending.dy += scroll.dy
        lastScroll = time
    }

    /// Lifts the finger and drops what was left to scroll, so that a click can take over.
    public mutating func interrupt(send: (Touch) -> Bool) {
        if let finger { _ = send(Touch(phase: .up, x: finger.x, y: finger.y)) }
        finger = nil
        pending = .zero
    }

    public mutating func tick(at time: Double, send: (Touch) -> Bool) {
        guard let finger else {
            if points(pending) >= Slop {
                if send(Touch(phase: .down, x: anchor.x, y: anchor.y)) { put(anchor, at: time) }
            } else if time - lastScroll >= Rest {
                pending = .zero
            }
            return
        }
        let target = inset(CGPoint(x: finger.x - pending.dx, y: finger.y - pending.dy))
        if target != finger {
            guard send(Touch(phase: .move, x: target.x, y: target.y)) else { return }
            pending.dx -= finger.x - target.x
            pending.dy -= finger.y - target.y
            put(target, at: time)
        } else if time - lastMove >= Rest {
            if send(Touch(phase: .up, x: finger.x, y: finger.y)) { self.finger = nil }
        } else {
            held.toggle()
            let offset = (held ? Wobble : -Wobble) / screen.height
            _ = send(Touch(phase: .move, x: finger.x, y: finger.y + offset))
        }
    }

    private mutating func put(_ point: CGPoint, at time: Double) {
        finger = point
        lastMove = time
    }

    private func points(_ vector: CGVector) -> Double {
        hypot(vector.dx * screen.width, vector.dy * screen.height)
    }

    private func inset(_ point: CGPoint) -> CGPoint {
        CGPoint(
            x: clamp(point.x, margin: EdgeMargin / screen.width),
            y: clamp(point.y, margin: EdgeMargin / screen.height)
        )
    }

    private func clamp(_ value: Double, margin: Double) -> Double {
        guard margin < 0.5 else { return 0.5 }
        return min(1 - margin, max(margin, value))
    }
}
