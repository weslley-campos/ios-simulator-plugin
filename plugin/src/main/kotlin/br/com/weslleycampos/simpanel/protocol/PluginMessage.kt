package br.com.weslleycampos.simpanel.protocol

import br.com.weslleycampos.simpanel.SimPanelJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** What the plugin sends the helper. */
sealed interface PluginMessage {
    fun encoded(): ByteArray

    /** The panel's size in pixels; the helper scales frames down to fit it. */
    @Serializable
    data class Viewport(val width: Int, val height: Int) : PluginMessage {
        override fun encoded() =
            Framing.encode(MessageType.Viewport, SimPanelJson.encodeToString(this).encodeToByteArray())
    }

    /** A touch at a point given as fractions of the screen, from 0 to 1. */
    @Serializable
    data class Touch(val phase: TouchPhase, val x: Double, val y: Double) : PluginMessage {
        override fun encoded() =
            Framing.encode(MessageType.Touch, SimPanelJson.encodeToString(this).encodeToByteArray())
    }

    /**
     * Scrolls the content under a point by a distance, both given as fractions of the screen; a positive
     * [dy] brings up what lies further down.
     */
    @Serializable
    data class Scroll(val x: Double, val y: Double, val dx: Double, val dy: Double) : PluginMessage {
        override fun encoded() =
            Framing.encode(MessageType.Scroll, SimPanelJson.encodeToString(this).encodeToByteArray())
    }

    /** A key of the hardware keyboard, given as its HID usage, going down or up. */
    @Serializable
    data class Key(val usage: Int, val down: Boolean) : PluginMessage {
        override fun encoded() =
            Framing.encode(MessageType.Key, SimPanelJson.encodeToString(this).encodeToByteArray())
    }

    /** A hardware button going down or up. */
    @Serializable
    data class Button(val button: DeviceButton, val down: Boolean) : PluginMessage {
        override fun encoded() =
            Framing.encode(MessageType.Button, SimPanelJson.encodeToString(this).encodeToByteArray())
    }

    /** How often the helper sends a frame, at most, and the JPEG quality it encodes them at, from 0 to 1. */
    @Serializable
    data class Settings(val maxFps: Int, val jpegQuality: Double) : PluginMessage {
        override fun encoded() =
            Framing.encode(MessageType.Settings, SimPanelJson.encodeToString(this).encodeToByteArray())
    }
}
