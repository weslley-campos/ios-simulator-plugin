package br.com.weslleycampos.simpanel.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TouchPhase {
    @SerialName("down") Down,
    @SerialName("move") Move,
    @SerialName("up") Up,
}
