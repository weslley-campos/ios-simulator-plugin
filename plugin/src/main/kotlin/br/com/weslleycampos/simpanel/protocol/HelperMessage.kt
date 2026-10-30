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

    /** The device drawn around the screen, in points, with its images as PNGs at [scale] pixels a point. */
    @Serializable
    class Chrome(
        val width: Double,
        val height: Double,
        val scale: Double,
        val screen: ChromeRect,
        val screenCornerRadius: Double = 0.0,
        /** The body, with the screen cut out of it. */
        @Serializable(with = Base64Serializer::class) val bezel: ByteArray,
        val buttons: List<ChromeButton>,
    ) : HelperMessage

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
            MessageType.Chrome -> SimPanelJson.decodeFromString<Chrome>(message.payload.decodeToString())
            else -> null
        }
    }
}
