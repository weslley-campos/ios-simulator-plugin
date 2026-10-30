import Foundation
import Testing
@testable import SimPanelKit

struct FramingTests {
    @Test func encodesLengthThenTypeThenPayload() {
        let message = Framing.encode(type: 2, payload: Data([0xff, 0xd8, 0xff]))

        #expect(Array(message) == [0, 0, 0, 3, 2, 0xff, 0xd8, 0xff])
    }

    @Test func decodesAMessageFedOneByteAtATime() throws {
        let message = Framing.encode(type: 17, payload: Data("{\"x\":1}".utf8))
        var decoder = FrameDecoder()

        for byte in message.dropLast() {
            decoder.append(Data([byte]))
            #expect(try decoder.next() == nil)
        }
        decoder.append(Data([message.last!]))
        let decoded = try #require(try decoder.next())

        #expect(decoded.type == 17)
        #expect(decoded.payload == Data("{\"x\":1}".utf8))
    }

    @Test func decodesTwoMessagesFromOneRead() throws {
        var decoder = FrameDecoder()
        decoder.append(Framing.encode(type: 1, payload: Data([1])) + Framing.encode(type: 2, payload: Data()))

        #expect(try decoder.next()?.type == 1)
        #expect(try decoder.next()?.payload == Data())
        #expect(try decoder.next() == nil)
    }

    @Test func rejectsALengthAboveTheLimit() {
        var decoder = FrameDecoder()
        decoder.append(Data([0x7f, 0xff, 0xff, 0xff, 2]))

        #expect(throws: FramingError.payloadTooLarge(0x7fff_ffff)) { try decoder.next() }
    }
}
