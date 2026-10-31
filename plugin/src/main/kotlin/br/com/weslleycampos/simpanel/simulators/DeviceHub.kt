package br.com.weslleycampos.simpanel.simulators

import com.intellij.ui.mac.foundation.Foundation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

/** DeviceHub's, then Simulator's, which shows the simulators before Xcode 27. */
private val BundleIds = listOf("com.apple.dt.Devices", "com.apple.iphonesimulator")
private val CommandTimeout = 10.seconds

/**
 * Xcode's DeviceHub app, which creates, boots and shows simulators, or Simulator, which does
 * before Xcode 27.
 */
object DeviceHub {
    /**
     * Opens the DeviceHub of the Xcode `xcode-select` points at, or its Simulator before Xcode 27;
     * it blocks, so run it off the EDT.
     */
    fun open() {
        val developerDirectory = Path.of(execute(CommandTimeout, "xcode-select", "--print-path").trim())
        execute(CommandTimeout, "open", appIn(developerDirectory).toString())
    }

    /**
     * Hides DeviceHub, or Simulator, when it's running and shown, which takes no permission, and
     * returns whether it was shown. It never quits them, which would shut down the simulators they booted.
     */
    fun hide(): Boolean {
        val pool = Foundation.NSAutoreleasePool()
        try {
            val apps = BundleIds.flatMap { bundleId ->
                Foundation.NSArray(
                    Foundation.invoke(
                        "NSRunningApplication",
                        "runningApplicationsWithBundleIdentifier:",
                        Foundation.nsString(bundleId),
                    ),
                ).list
            }
            val shown = apps.filter { !Foundation.invoke(it, "isHidden").booleanValue() }
            shown.forEach { Foundation.invoke(it, "hide") }
            return shown.isNotEmpty()
        } finally {
            pool.drain()
        }
    }
}

/**
 * The app that shows the simulators of the Xcode whose `Developer` directory is [developerDirectory]:
 * DeviceHub in `Xcode.app/Contents/Applications` from Xcode 27, Simulator in
 * `Developer/Applications` before it.
 */
internal fun appIn(developerDirectory: Path): Path {
    val deviceHub = developerDirectory.resolveSibling("Applications").resolve("DeviceHub.app")
    return if (Files.isDirectory(deviceHub)) deviceHub else developerDirectory.resolve("Applications/Simulator.app")
}
