import Foundation
import Testing
@testable import SimPanelKit

struct MessageTests {
    private func decodeHelper(_ message: HelperMessage) throws -> HelperMessage? {
        var decoder = FrameDecoder()
        decoder.append(try message.encoded())
        let (type, payload) = try #require(try decoder.next())
        return try HelperMessage.decode(type: type, payload: payload)
    }

    private func decodePlugin(_ message: PluginMessage) throws -> PluginMessage? {
        var decoder = FrameDecoder()
        decoder.append(try message.encoded())
        let (type, payload) = try #require(try decoder.next())
        return try PluginMessage.decode(type: type, payload: payload)
    }

    @Test func helloRoundTrips() throws {
        let hello = HelperMessage.hello(Hello(udid: "B849", name: "iPhone 18 Pro", width: 1206, height: 2622))

        #expect(try decodeHelper(hello) == hello)
    }

    @Test func helloDerivesOrientationFromItsSize() {
        #expect(Hello(udid: "", name: "", width: 1206, height: 2622).orientation == .portrait)
        #expect(Hello(udid: "", name: "", width: 2622, height: 1206).orientation == .landscape)
    }

    @Test func helloUsesTheSpecKeys() throws {
        let json = try JSONEncoder().encode(Hello(udid: "B849", name: "iPhone", width: 2, height: 4))
        let object = try #require(try JSONSerialization.jsonObject(with: json) as? [String: Any])

        #expect(Set(object.keys) == ["udid", "name", "width", "height", "orientation"])
        #expect(object["orientation"] as? String == "portrait")
    }

    @Test func frameCarriesTheJPEGBytesUnchanged() throws {
        let jpeg = Data([0xff, 0xd8, 0xff, 0xe0, 0x00])

        #expect(try decodeHelper(.frame(jpeg)) == .frame(jpeg))
    }

    @Test func errorRoundTripsWithItsWireCode() throws {
        let error = HelperMessage.error(HelperError(code: .deviceNotBooted, message: "B849"))
        let payload = try error.encoded().dropFirst(Framing.headerSize)

        #expect(try decodeHelper(error) == error)
        #expect(String(decoding: payload, as: UTF8.self).contains("\"device_not_booted\""))
    }

    @Test func viewportAndTouchRoundTrip() throws {
        let viewport = PluginMessage.viewport(Viewport(width: 400, height: 870))
        let touch = PluginMessage.touch(Touch(phase: .move, x: 0.25, y: 0.75))

        #expect(try decodePlugin(viewport) == viewport)
        #expect(try decodePlugin(touch) == touch)
    }

    @Test func decodesATouchAsThePluginWritesIt() throws {
        let payload = Data(#"{"phase":"down","x":0.5,"y":0.125}"#.utf8)

        let message = try PluginMessage.decode(type: MessageType.touch.rawValue, payload: payload)

        #expect(message == .touch(Touch(phase: .down, x: 0.5, y: 0.125)))
    }

    @Test func decodesAButtonAsThePluginWritesIt() throws {
        let payload = Data(#"{"button":"volume-up","down":true}"#.utf8)

        let message = try PluginMessage.decode(type: MessageType.button.rawValue, payload: payload)

        #expect(message == .button(ButtonPress(button: .volumeUp, down: true)))
    }

    @Test func buttonRoundTrips() throws {
        let press = PluginMessage.button(ButtonPress(button: .power, down: false))

        #expect(try decodePlugin(press) == press)
    }

    @Test func ignoresTypesItDoesNotHandle() throws {
        #expect(try PluginMessage.decode(type: MessageType.settings.rawValue, payload: Data("{}".utf8)) == nil)
        #expect(try PluginMessage.decode(type: 99, payload: Data()) == nil)
        #expect(try HelperMessage.decode(type: MessageType.display.rawValue, payload: Data("{}".utf8)) == nil)
    }
}
