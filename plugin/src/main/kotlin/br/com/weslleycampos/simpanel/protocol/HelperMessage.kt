package br.com.weslleycampos.simpanel.protocol

import br.com.weslleycampos.simpanel.SimPanelJson
import kotlinx.serialization.Serializable

/** What the helper sends the plugin. */
sealed interface HelperMessage {
    @Serializable
    data class Hello(
        val udid: String,
        val name: String,
        val width: Int,
        val height: Int,
        val orientation: Orientation,
    ) : HelperMessage

    class Frame(val jpeg: ByteArray) : HelperMessage

    @Serializable
    data class Failure(val code: String, val message: String) : HelperMessage {
        val errorCode: ErrorCode get() = ErrorCode.of(code)
    }

    companion object {
        /** Null for a type this version doesn't handle, so a newer helper doesn't break it. */
        fun decode(message: RawMessage): HelperMessage? = when (MessageType.of(message.type)) {
            MessageType.Hello -> SimPanelJson.decodeFromString<Hello>(message.payload.decodeToString())
            MessageType.Frame -> Frame(message.payload)
            MessageType.Error -> SimPanelJson.decodeFromString<Failure>(message.payload.decodeToString())
            else -> null
        }
    }
}
