package br.com.weslleycampos.simpanel.runs

private const val XcodeSimulatorPrefix = "iphonesimulator_"
private val Udid = Regex("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}", RegexOption.IGNORE_CASE)

/**
 * The UDID of the simulator a run starts on, from its target's id: `iphonesimulator_<UDID>` for an
 * Xcode Application and the UDID itself for a Kotlin Multiplatform iOS Application. Null for any
 * other target, such as a device or an Android emulator.
 */
fun simulatorUdid(targetId: String): String? =
    Udid.matchEntire(targetId.removePrefix(XcodeSimulatorPrefix))?.value?.uppercase()
