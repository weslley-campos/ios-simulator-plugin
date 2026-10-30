package br.com.weslleycampos.simpanel.simulators

import kotlin.time.Duration.Companion.seconds

private val OrientationTimeout = 10.seconds

/** Blocking calls to `xcrun devicectl`; run them off the EDT. */
object Devicectl {
    /** Which way up the simulator is held. */
    fun orientation(udid: String): Orientation = parseOrientation(orientation("get", "--device", udid))

    /** Turns the simulator a quarter turn, as its Rotate menu items do, and returns which way up it ends. */
    fun rotate(udid: String, rotation: Rotation): Orientation =
        parseOrientation(orientation("rotate", "--device", udid, rotation.devicectlName))

    /** Runs an orientation subcommand, its JSON on stdout. */
    private fun orientation(vararg arguments: String): String =
        xcrun(OrientationTimeout, "devicectl", "device", "orientation", *arguments, "--quiet", "--json-output", "-")
}
