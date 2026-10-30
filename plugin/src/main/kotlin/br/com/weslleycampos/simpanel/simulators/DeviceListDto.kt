package br.com.weslleycampos.simpanel.simulators

import kotlinx.serialization.Serializable

@Serializable
internal class DeviceListDto(val devices: Map<String, List<DeviceDto>>)
