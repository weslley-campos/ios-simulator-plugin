package br.com.weslleycampos.simpanel.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Orientation {
    @SerialName("portrait") Portrait,
    @SerialName("landscape") Landscape,
}
