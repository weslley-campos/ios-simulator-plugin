/// The type byte that follows every message's length.
public enum MessageType: UInt8, Sendable {
    case hello = 1
    case frame = 2
    case display = 3
    case error = 4
    case chrome = 5

    case viewport = 16
    case touch = 17
    case scroll = 18
    case key = 19
    case text = 20
    case button = 21
    case settings = 22
}
