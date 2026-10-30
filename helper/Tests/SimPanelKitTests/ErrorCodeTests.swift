import Foundation
import SimBridge
import Testing
@testable import SimPanelKit

struct ErrorCodeTests {
    @Test(arguments: [
        (SPError.Code.xcodeNotFound, ErrorCode.xcodeNotFound),
        (.unsupportedXcode, .unsupportedXcode),
        (.deviceNotFound, .deviceNotFound),
        (.deviceNotBooted, .deviceNotBooted),
        (.framebufferUnavailable, .framebufferUnavailable),
        (.hidUnavailable, .hidUnavailable),
    ])
    func mapsEveryBridgeError(code: SPError.Code, expected: ErrorCode) {
        let error = NSError(domain: SPErrorDomain, code: code.rawValue)

        #expect(ErrorCode(error) == expected)
    }

    @Test func mapsAnyOtherErrorToInternal() {
        #expect(ErrorCode(POSIXError(.EPIPE)) == .internalError)
        #expect(ErrorCode(NSError(domain: SPErrorDomain, code: 999)) == .internalError)
    }
}
