import Foundation

/// The panel's size in pixels; the helper scales frames down to fit it.
public struct Viewport: Codable, Equatable, Sendable {
    public let width: Int
    public let height: Int

    public init(width: Int, height: Int) {
        self.width = width
        self.height = height
    }
}

/// A touch at a point given as fractions of the screen, from 0 to 1.
public struct Touch: Codable, Equatable, Sendable {
    public let phase: Phase
    public let x: Double
    public let y: Double

    public init(phase: Phase, x: Double, y: Double) {
        self.phase = phase
        self.x = x
        self.y = y
    }

    public enum Phase: String, Codable, Sendable {
        case down
        case move
        case up
    }
}

/// What the plugin sends the helper.
public enum PluginMessage: Equatable, Sendable {
    case viewport(Viewport)
    case touch(Touch)

    public func encoded() throws -> Data {
        switch self {
        case .viewport(let viewport):
            Framing.encode(
                type: MessageType.viewport.rawValue,
                payload: try JSONEncoder().encode(viewport)
            )
        case .touch(let touch):
            Framing.encode(type: MessageType.touch.rawValue, payload: try JSONEncoder().encode(touch))
        }
    }

    /// Nil for a type this version doesn't handle yet, which the helper ignores.
    public static func decode(type: UInt8, payload: Data) throws -> PluginMessage? {
        switch MessageType(rawValue: type) {
        case .viewport: .viewport(try JSONDecoder().decode(Viewport.self, from: payload))
        case .touch: .touch(try JSONDecoder().decode(Touch.self, from: payload))
        default: nil
        }
    }
}
