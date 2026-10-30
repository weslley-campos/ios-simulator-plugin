import Foundation

public struct Hello: Codable, Equatable, Sendable {
    public let udid: String
    public let name: String
    public let width: Int
    public let height: Int
    public let orientation: Orientation

    public init(udid: String, name: String, width: Int, height: Int) {
        self.udid = udid
        self.name = name
        self.width = width
        self.height = height
        self.orientation = width > height ? .landscape : .portrait
    }
}

public enum Orientation: String, Codable, Sendable {
    case portrait
    case landscape
}

public struct HelperError: Codable, Equatable, Sendable, Error {
    public let code: ErrorCode
    public let message: String

    public init(code: ErrorCode, message: String) {
        self.code = code
        self.message = message
    }
}

/// What the helper sends the plugin.
public enum HelperMessage: Equatable, Sendable {
    case hello(Hello)
    case frame(Data)
    case error(HelperError)
    case chrome(Chrome)

    public func encoded() throws -> Data {
        switch self {
        case .hello(let hello):
            Framing.encode(type: MessageType.hello.rawValue, payload: try JSONEncoder().encode(hello))
        case .frame(let jpeg):
            Framing.encode(type: MessageType.frame.rawValue, payload: jpeg)
        case .error(let error):
            Framing.encode(type: MessageType.error.rawValue, payload: try JSONEncoder().encode(error))
        case .chrome(let chrome):
            Framing.encode(type: MessageType.chrome.rawValue, payload: try JSONEncoder().encode(chrome))
        }
    }

    /// Nil for a type this version doesn't know, so a newer helper doesn't break an older client.
    public static func decode(type: UInt8, payload: Data) throws -> HelperMessage? {
        switch MessageType(rawValue: type) {
        case .hello: .hello(try JSONDecoder().decode(Hello.self, from: payload))
        case .frame: .frame(payload)
        case .error: .error(try JSONDecoder().decode(HelperError.self, from: payload))
        case .chrome: .chrome(try JSONDecoder().decode(Chrome.self, from: payload))
        default: nil
        }
    }
}
