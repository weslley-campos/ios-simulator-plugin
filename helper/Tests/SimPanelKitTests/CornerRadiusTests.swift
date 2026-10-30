import Testing
@testable import SimPanelKit

struct CornerRadiusTests {
    @Test func findsTheRadiusOfARoundCorner() {
        let radius = 55.0
        let scale = 3.0
        let coverage = (0..<200).map { pixel -> UInt8 in
            let point = (Double(pixel) + 0.5) / scale
            return (radius - point) * 2.squareRoot() <= radius ? 255 : 0
        }

        #expect(abs(cornerRadius(diagonal: coverage, scale: scale) - radius) < 1)
    }

    @Test func findsNoRadiusWhereNothingIsCovered() {
        #expect(cornerRadius(diagonal: [0, 0, 0], scale: 3) == 0)
    }
}
