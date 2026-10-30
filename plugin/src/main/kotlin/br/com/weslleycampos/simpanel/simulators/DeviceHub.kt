package br.com.weslleycampos.simpanel.simulators

import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

private val CommandTimeout = 10.seconds

/** Xcode's DeviceHub app, which creates, boots and shows simulators. */
object DeviceHub {
    /**
     * Opens the DeviceHub of the Xcode `xcode-select` points at, in `Xcode.app/Contents/Applications`
     * next to its `Developer` directory; it blocks, so run it off the EDT.
     */
    fun open() {
        val developerDirectory = Path.of(execute(CommandTimeout, "xcode-select", "--print-path").trim())
        val app = developerDirectory.resolveSibling("Applications").resolve("DeviceHub.app")
        execute(CommandTimeout, "open", app.toString())
    }
}
