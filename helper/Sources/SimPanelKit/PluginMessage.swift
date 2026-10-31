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

/// Scrolls the content under a point by a distance, both given as fractions of the screen;
/// a positive `dy` brings up what lies further down.
public struct Scroll: Codable, Equatable, Sendable {
    public let x: Double
    public let y: Double
    public let dx: Double
    public let dy: Double

    public init(x: Double, y: Double, dx: Double, dy: Double) {
        self.x = x
        self.y = y
        self.dx = dx
        self.dy = dy
    }
}

/// A keyboard key going down or up, by its HID usage on the keyboard page.
public struct Key: Codable, Equatable, Sendable {
    public let usage: UInt32
    public let down: Bool

    public init(usage: UInt32, down: Bool) {
        self.usage = usage
        self.down = down
    }
}

/// Whether the simulator shows its on-screen keyboard, or hides it as though a hardware keyboard
/// were connected.
public struct SoftwareKeyboard: Codable, Equatable, Sendable {
    public let shown: Bool

    public init(shown: Bool) {
        self.shown = shown
    }
}

/// A hardware button going down or up, named as the device chrome names it.
public struct ButtonPress: Codable, Equatable, Sendable {
    public let button: DeviceButton
    public let down: Bool

    public init(button: DeviceButton, down: Bool) {
        self.button = button
        self.down = down
    }
}

public enum DeviceButton: String, Codable, Sendable {
    case home
    case power
    case volumeUp = "volume-up"
    case volumeDown = "volume-down"
    case action
    case siri
}

/// How often the helper sends a frame, at most, and the JPEG quality it encodes them at, from 0 to 1.
public struct StreamSettings: Codable, Equatable, Sendable {
    public static let standard = StreamSettings(maxFps: 60, jpegQuality: 0.7)

    public let maxFps: Int
    public let jpegQuality: Double

    public init(maxFps: Int, jpegQuality: Double) {
        self.maxFps = maxFps
        self.jpegQuality = jpegQuality
    }

    /// The settings brought within what the helper streams: 1 to 60 frames a second, quality 0.1 to 1.
    public var clamped: StreamSettings {
        StreamSettings(maxFps: min(max(maxFps, 1), 60), jpegQuality: min(max(jpegQuality, 0.1), 1))
    }
}

/// What the plugin sends the helper.
public enum PluginMessage: Equatable, Sendable {
    case viewport(Viewport)
    case touch(Touch)
    case scroll(Scroll)
    case key(Key)
    case button(ButtonPress)
    case settings(StreamSettings)
    case softwareKeyboard(SoftwareKeyboard)

    public func encoded() throws -> Data {
        switch self {
        case .viewport(let viewport):
            Framing.encode(
                type: MessageType.viewport.rawValue,
                payload: try JSONEncoder().encode(viewport)
            )
        case .touch(let touch):
            Framing.encode(type: MessageType.touch.rawValue, payload: try JSONEncoder().encode(touch))
        case .scroll(let scroll):
            Framing.encode(type: MessageType.scroll.rawValue, payload: try JSONEncoder().encode(scroll))
        case .key(let key):
            Framing.encode(type: MessageType.key.rawValue, payload: try JSONEncoder().encode(key))
        case .button(let press):
            Framing.encode(type: MessageType.button.rawValue, payload: try JSONEncoder().encode(press))
        case .settings(let settings):
            Framing.encode(type: MessageType.settings.rawValue, payload: try JSONEncoder().encode(settings))
        case .softwareKeyboard(let keyboard):
            Framing.encode(
                type: MessageType.softwareKeyboard.rawValue,
                payload: try JSONEncoder().encode(keyboard)
            )
        }
    }

    /// Nil for a type this version doesn't handle yet, which the helper ignores.
    public static func decode(type: UInt8, payload: Data) throws -> PluginMessage? {
        switch MessageType(rawValue: type) {
        case .viewport: .viewport(try JSONDecoder().decode(Viewport.self, from: payload))
        case .touch: .touch(try JSONDecoder().decode(Touch.self, from: payload))
        case .scroll: .scroll(try JSONDecoder().decode(Scroll.self, from: payload))
        case .key: .key(try JSONDecoder().decode(Key.self, from: payload))
        case .button: .button(try JSONDecoder().decode(ButtonPress.self, from: payload))
        case .settings: .settings(try JSONDecoder().decode(StreamSettings.self, from: payload))
        case .softwareKeyboard:
            .softwareKeyboard(try JSONDecoder().decode(SoftwareKeyboard.self, from: payload))
        default: nil
        }
    }
}
