import Testing
@testable import SimPanelKit

struct FrameScaleTests {
    @Test func fitsATallFrameToTheViewportHeight() {
        let scale = frameScale(width: 1206, height: 2622, viewport: Viewport(width: 1000, height: 1311))

        #expect(scale == 0.5)
    }

    @Test func fitsAWideFrameToTheViewportWidth() {
        let scale = frameScale(width: 2622, height: 1206, viewport: Viewport(width: 1311, height: 1000))

        #expect(scale == 0.5)
    }

    @Test func neverEnlargesTheFrame() {
        #expect(frameScale(width: 1206, height: 2622, viewport: Viewport(width: 5000, height: 5000)) == 1)
    }

    @Test func keepsFullSizeWithoutAUsableViewport() {
        #expect(frameScale(width: 1206, height: 2622, viewport: nil) == 1)
        #expect(frameScale(width: 1206, height: 2622, viewport: Viewport(width: 0, height: 870)) == 1)
    }
}
