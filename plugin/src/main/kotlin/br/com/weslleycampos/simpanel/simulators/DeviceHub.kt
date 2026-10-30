package br.com.weslleycampos.simpanel.simulators

import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

private val CommandTimeout = 10.seconds

/** Xcode's DeviceHub app, which creates, boots and shows simulators. */
object DeviceHub {
    /** Opens the DeviceHub of the Xcode `xcode-select` points at; it blocks, so run it off the EDT. */
    fun open() {
        val developerDirectory = execute(CommandTimeout, "xcode-select", "--print-path").trim()
        execute(CommandTimeout, "open", Path.of(developerDirectory, "Applications", "DeviceHub.app").toString())
    }
}
