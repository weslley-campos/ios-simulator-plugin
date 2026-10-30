import Foundation

/// Every message is a 4-byte big-endian payload length, a 1-byte type, then the payload.
public enum Framing {
    public static let headerSize = 5
    public static let maxPayloadSize = 32 * 1024 * 1024

    public static func encode(type: UInt8, payload: Data) -> Data {
        var message = Data(capacity: headerSize + payload.count)
        let length = UInt32(payload.count).bigEndian
        withUnsafeBytes(of: length) { message.append(contentsOf: $0) }
        message.append(type)
        message.append(payload)
        return message
    }
}

public enum FramingError: Error, Equatable {
    case payloadTooLarge(Int)
}

/// Splits a byte stream back into messages, however the reads happen to cut it.
public struct FrameDecoder: Sendable {
    private var buffer = Data()

    public init() {}

    public mutating func append(_ data: Data) {
        buffer.append(data)
    }

    /// The next whole message, or nil until more bytes arrive.
    public mutating func next() throws -> (type: UInt8, payload: Data)? {
        guard buffer.count >= Framing.headerSize else { return nil }
        let start = buffer.startIndex
        let length = buffer[start..<start + 4].reduce(0) { $0 << 8 | Int($1) }
        guard length <= Framing.maxPayloadSize else {
            throw FramingError.payloadTooLarge(length)
        }
        guard buffer.count >= Framing.headerSize + length else { return nil }
        let type = buffer[start + 4]
        let payloadStart = start + Framing.headerSize
        let payload = Data(buffer[payloadStart..<payloadStart + length])
        buffer = Data(buffer[(payloadStart + length)...])
        return (type, payload)
    }
}
