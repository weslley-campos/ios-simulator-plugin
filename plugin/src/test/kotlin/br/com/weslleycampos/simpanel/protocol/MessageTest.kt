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
    fun writesTheButtonTheHelperReads() {
        val press = PluginMessage.Button(DeviceButton.VolumeUp, down = true)

        assertEquals(MessageType.Button.code, press.encoded()[4])
        assertEquals("""{"button":"volume-up","down":true}""", json(press))
    }
}
