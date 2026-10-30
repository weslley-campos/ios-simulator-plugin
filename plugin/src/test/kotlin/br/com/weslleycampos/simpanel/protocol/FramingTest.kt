package br.com.weslleycampos.simpanel.protocol

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FramingTest {
    private fun channelOf(bytes: ByteArray): ReadableByteChannel = Channels.newChannel(bytes.inputStream())

    /** Hands out one byte per read, the worst split a socket can produce. */
    private fun tricklingChannelOf(bytes: ByteArray) = object : ReadableByteChannel {
        private var position = 0
        override fun read(dst: ByteBuffer): Int {
            if (position == bytes.size) return -1
            dst.put(bytes[position++])
            return 1
        }
        override fun isOpen() = true
        override fun close() = Unit
    }

    @Test
    fun encodesLengthThenTypeThenPayload() {
        val message = Framing.encode(MessageType.Frame, byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()))

        assertContentEquals(byteArrayOf(0, 0, 0, 3, 2, 0xff.toByte(), 0xd8.toByte(), 0xff.toByte()), message)
    }

    @Test
    fun readsAMessageThatArrivesOneByteAtATime() {
        val payload = """{"x":1}""".encodeToByteArray()
        val reader = MessageReader(tricklingChannelOf(Framing.encode(MessageType.Touch, payload)))

        val message = assertNotNull(reader.next())

        assertEquals(MessageType.Touch.code, message.type)
        assertContentEquals(payload, message.payload)
    }

    @Test
    fun readsTwoMessagesInARowThenNullAtTheEnd() {
        val bytes = Framing.encode(MessageType.Hello, byteArrayOf(1)) + Framing.encode(MessageType.Frame, byteArrayOf())
        val reader = MessageReader(channelOf(bytes))

        assertEquals(MessageType.Hello.code, reader.next()?.type)
        assertContentEquals(byteArrayOf(), reader.next()?.payload)
        assertNull(reader.next())
    }

    @Test
    fun failsWhenTheConnectionClosesInsideAMessage() {
        val reader = MessageReader(channelOf(Framing.encode(MessageType.Frame, byteArrayOf(1, 2, 3)).copyOf(6)))

        assertFailsWith<EOFException> { reader.next() }
    }

    @Test
    fun rejectsALengthAboveTheLimit() {
        val reader = MessageReader(channelOf(byteArrayOf(0x7f, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 2)))

        val error = assertFailsWith<PayloadTooLargeException> { reader.next() }

        assertEquals(0x7fff_ffffL, error.size)
    }
}
