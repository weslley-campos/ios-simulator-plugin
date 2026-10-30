import CoreGraphics
import Testing
@testable import SimPanelKit

struct ScrollGestureTests {
    private let screen = CGSize(width: 440, height: 956)

    private func gesture(scrolling scroll: Scroll, at time: Double = 0) -> ScrollGesture {
        var gesture = ScrollGesture(screen: screen)
        gesture.scroll(scroll, at: time, screen: screen)
        return gesture
    }

    private func tick(_ gesture: inout ScrollGesture, at time: Double, accept: Bool = true) -> [Touch] {
        var touches: [Touch] = []
        gesture.tick(at: time) { touches.append($0); return accept }
        return touches
    }

    @Test func dragsTheFingerAgainstTheScroll() {
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: 0.125))

        #expect(tick(&gesture, at: 0) == [Touch(phase: .down, x: 0.5, y: 0.5)])
        #expect(tick(&gesture, at: 0.016) == [Touch(phase: .move, x: 0.5, y: 0.375)])
    }

    @Test func waitsForTheSlopBeforeTheFingerGoesDown() {
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: 7 / 956))

        #expect(tick(&gesture, at: 0).isEmpty)

        gesture.scroll(Scroll(x: 0.5, y: 0.5, dx: 0, dy: 7 / 956), at: 0.01, screen: screen)

        #expect(tick(&gesture, at: 0.016) == [Touch(phase: .down, x: 0.5, y: 0.5)])
    }

    @Test func forgetsAScrollTooSmallToDrag() {
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: 0.005))

        #expect(tick(&gesture, at: 0.3).isEmpty)
        #expect(gesture.isIdle)
    }

    @Test func holdsTheFingerWithAWobbleThenLiftsItOnceTheScrollRests() {
        let wobble = 0.5 / 956
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: 0.125))
        _ = tick(&gesture, at: 0)
        _ = tick(&gesture, at: 0.016)

        #expect(tick(&gesture, at: 0.032) == [Touch(phase: .move, x: 0.5, y: 0.375 + wobble)])
        #expect(tick(&gesture, at: 0.048) == [Touch(phase: .move, x: 0.5, y: 0.375 - wobble)])
        #expect(tick(&gesture, at: 0.27) == [Touch(phase: .up, x: 0.5, y: 0.375)])
        #expect(gesture.isIdle)
    }

    @Test func sendsADroppedMoveAgain() {
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: 0.125))
        _ = tick(&gesture, at: 0)

        #expect(tick(&gesture, at: 0.016, accept: false) == [Touch(phase: .move, x: 0.5, y: 0.375)])
        #expect(tick(&gesture, at: 0.032) == [Touch(phase: .move, x: 0.5, y: 0.375)])
    }

    @Test func keepsTheFingerOffTheEdges() {
        var gesture = gesture(scrolling: Scroll(x: 0.01, y: 0.99, dx: 0, dy: -0.1))

        #expect(tick(&gesture, at: 0) == [Touch(phase: .down, x: 40 / 440, y: 1 - 40 / 956)])
    }

    @Test func startsAgainFromTheAnchorWhenTheFingerReachesTheEdge() {
        let bottom = 1 - 40 / 956.0
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: -0.75))
        _ = tick(&gesture, at: 0)

        #expect(tick(&gesture, at: 0.016) == [Touch(phase: .move, x: 0.5, y: bottom)])
        #expect(tick(&gesture, at: 0.27) == [Touch(phase: .up, x: 0.5, y: bottom)])
        #expect(tick(&gesture, at: 0.286) == [Touch(phase: .down, x: 0.5, y: 0.5)])
    }

    @Test func liftsTheFingerWhenInterrupted() {
        var gesture = gesture(scrolling: Scroll(x: 0.5, y: 0.5, dx: 0, dy: 0.125))
        _ = tick(&gesture, at: 0)
        var touches: [Touch] = []

        gesture.interrupt { touches.append($0); return true }

        #expect(touches == [Touch(phase: .up, x: 0.5, y: 0.5)])
        #expect(gesture.isIdle)
    }
}
