package br.com.weslleycampos.simpanel.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A hardware button, named as Xcode's device chrome names it. */
@Serializable
enum class DeviceButton {
    @SerialName("home") Home,
    @SerialName("power") Power,
    @SerialName("volume-up") VolumeUp,
    @SerialName("volume-down") VolumeDown,
    @SerialName("action") Action,
    @SerialName("siri") Siri,
}
