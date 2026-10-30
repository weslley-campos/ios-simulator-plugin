package br.com.weslleycampos.simpanel.simulators

import br.com.weslleycampos.simpanel.SimPanelJson

private const val IosRuntimePrefix = "com.apple.CoreSimulator.SimRuntime.iOS-"
private const val BootedState = "Booted"

/**
 * The available iOS simulators in `xcrun simctl list -j devices` output: booted ones first,
 * then by newest runtime, keeping simctl's order within a runtime.
 */
fun parseSimulators(json: String): List<Simulator> =
    SimPanelJson.decodeFromString<DeviceListDto>(json).devices.entries
        .filter { it.key.startsWith(IosRuntimePrefix) }
        .sortedByDescending { runtimeOrder(it.key) }
        .flatMap { (runtime, devices) ->
            devices.filter { it.isAvailable }.map {
                Simulator(it.udid, it.name, runtimeName(runtime), it.state == BootedState)
            }
        }
        .sortedByDescending { it.booted }

private fun runtimeVersion(runtime: String): List<Int> =
    runtime.removePrefix(IosRuntimePrefix).split('-').mapNotNull(String::toIntOrNull)

private fun runtimeOrder(runtime: String): Int =
    runtimeVersion(runtime).let { (it.getOrElse(0) { 0 } * 1000) + it.getOrElse(1) { 0 } }

private fun runtimeName(runtime: String): String =
    "iOS " + runtimeVersion(runtime).joinToString(".")
