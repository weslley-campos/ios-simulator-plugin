package br.com.weslleycampos.simpanel.protocol

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

class MessageReader(private val channel: ReadableByteChannel) {
    private val header = ByteBuffer.allocate(Framing.HeaderSize)

    /** The next message, or null once the other side closed the connection between messages. */
    fun next(): RawMessage? {
        header.clear()
        if (!readFully(header)) return null
        header.flip()
        val size = header.int.toLong() and 0xffff_ffffL
        if (size > Framing.MaxPayloadSize) throw PayloadTooLargeException(size)
        val type = header.get()
        val payload = ByteBuffer.allocate(size.toInt())
        if (!readFully(payload)) throw EOFException("The connection closed inside a message")
        return RawMessage(type, payload.array())
    }

    /** False when the channel ended before the first byte of [buffer]. */
    private fun readFully(buffer: ByteBuffer): Boolean {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                if (buffer.position() == 0) return false
                throw EOFException("The connection closed inside a message")
            }
        }
        return true
    }
}
