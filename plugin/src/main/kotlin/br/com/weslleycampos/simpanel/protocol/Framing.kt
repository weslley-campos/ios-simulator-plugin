package br.com.weslleycampos.simpanel.protocol

import java.nio.ByteBuffer

/** A 4-byte big-endian payload length, a 1-byte type, then the payload. */
object Framing {
    const val HeaderSize = 5
    const val MaxPayloadSize = 32 * 1024 * 1024

    fun encode(type: MessageType, payload: ByteArray): ByteArray =
        ByteBuffer.allocate(HeaderSize + payload.size)
            .putInt(payload.size)
            .put(type.code)
            .put(payload)
            .array()
}
