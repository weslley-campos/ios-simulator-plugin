package br.com.weslleycampos.simpanel.simulators

import kotlinx.serialization.Serializable

@Serializable
internal class DeviceDto(
    val udid: String,
    val name: String,
    val state: String,
    val isAvailable: Boolean = false,
)
