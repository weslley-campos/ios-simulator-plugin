import Foundation
import SimBridge

public enum ErrorCode: String, Codable, Sendable {
    case deviceNotFound = "device_not_found"
    case deviceNotBooted = "device_not_booted"
    case framebufferUnavailable = "framebuffer_unavailable"
    case hidUnavailable = "hid_unavailable"
    case xcodeNotFound = "xcode_not_found"
    case unsupportedXcode = "unsupported_xcode"
    case internalError = "internal"

    public init(_ error: Error) {
        let error = error as NSError
        guard error.domain == SPErrorDomain, let code = SPError.Code(rawValue: error.code) else {
            self = .internalError
            return
        }
        self = switch code {
        case .xcodeNotFound: .xcodeNotFound
        case .unsupportedXcode: .unsupportedXcode
        case .deviceNotFound: .deviceNotFound
        case .deviceNotBooted: .deviceNotBooted
        case .framebufferUnavailable: .framebufferUnavailable
        case .hidUnavailable: .hidUnavailable
        @unknown default: .internalError
        }
    }
}
