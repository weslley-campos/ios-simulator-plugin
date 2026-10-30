package br.com.weslleycampos.simpanel.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MessageTest {
    private fun raw(type: MessageType, payload: String) = RawMessage(type.code, payload.encodeToByteArray())

    private fun json(message: PluginMessage) = message.encoded().drop(Framing.HeaderSize).toByteArray().decodeToString()

    @Test
    fun decodesTheHelloTheHelperWrites() {
        val payload = """{"width":1206,"orientation":"portrait","udid":"B849","height":2622,"name":"iPhone 18 Pro"}"""

        val hello = HelperMessage.decode(raw(MessageType.Hello, payload))

        assertEquals(HelperMessage.Hello("B849", "iPhone 18 Pro", 1206, 2622, Orientation.Portrait), hello)
    }

    @Test
    fun keepsTheFrameBytesUnchanged() {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xe0.toByte(), 0)

        val frame = assertIs<HelperMessage.Frame>(HelperMessage.decode(RawMessage(MessageType.Frame.code, jpeg)))

        assertContentEquals(jpeg, frame.jpeg)
    }

    @Test
    fun decodesTheChromeWithItsBase64Images() {
        val payload = """{"width":494,"height":992,"scale":3,"screen":{"x":27,"y":18,"width":440,"height":956},""" +
            """"screenCornerRadius":63.16,""" +
            """"bezel":"iVBO\/w==","buttons":[{"button":"power","frame":{"x":477,"y":293,"width":16,"height":101},""" +
            """"onTop":false,"image":"AQI=","pressedImage":"Aw=="}]}"""

        val chrome = assertIs<HelperMessage.Chrome>(HelperMessage.decode(raw(MessageType.Chrome, payload)))

        assertEquals(ChromeRect(27.0, 18.0, 440.0, 956.0), chrome.screen)
        assertEquals(63.16, chrome.screenCornerRadius)
        assertContentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0xff.toByte()), chrome.bezel)
        assertEquals(DeviceButton.Power, chrome.buttons.single().button)
        assertContentEquals(byteArrayOf(1, 2), chrome.buttons.single().image)
    }

    @Test
    fun decodesAnErrorWithItsCode() {
        val failure = HelperMessage.decode(raw(MessageType.Error, """{"code":"device_not_booted","message":"B849"}"""))

        assertEquals(ErrorCode.DeviceNotBooted, assertIs<HelperMessage.Failure>(failure).errorCode)
        assertEquals("B849", failure.message)
    }

    @Test
    fun ignoresTypesItDoesNotHandle() {
        assertNull(HelperMessage.decode(raw(MessageType.Display, "{}")))
        assertNull(HelperMessage.decode(RawMessage(99, byteArrayOf())))
    }

    @Test
    fun writesTheViewportAndTouchTheHelperReads() {
        val viewport = PluginMessage.Viewport(400, 870)
        val touch = PluginMessage.Touch(TouchPhase.Down, 0.5, 0.125)

        assertEquals(MessageType.Viewport.code, viewport.encoded()[4])
        assertEquals("""{"width":400,"height":870}""", json(viewport))
        assertEquals(MessageType.Touch.code, touch.encoded()[4])
        assertEquals("""{"phase":"down","x":0.5,"y":0.125}""", json(touch))
    }

    @Test
    fun writesTheScrollTheHelperReads() {
        val scroll = PluginMessage.Scroll(0.5, 0.25, dx = 0.0, dy = -0.125)

        assertEquals(MessageType.Scroll.code, scroll.encoded()[4])
        assertEquals("""{"x":0.5,"y":0.25,"dx":0.0,"dy":-0.125}""", json(scroll))
    }

    @Test
    fun writesTheKeyTheHelperReads() {
        val key = PluginMessage.Key(0xe1, down = false)

        assertEquals(MessageType.Key.code, key.encoded()[4])
        assertEquals("""{"usage":225,"down":false}""", json(key))
    }

    @Test
    fun writesTheButtonTheHelperReads() {
        val press = PluginMessage.Button(DeviceButton.VolumeUp, down = true)

        assertEquals(MessageType.Button.code, press.encoded()[4])
        assertEquals("""{"button":"volume-up","down":true}""", json(press))
    }
}
