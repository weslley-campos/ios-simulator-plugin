package br.com.weslleycampos.simpanel.simulators

import com.intellij.execution.configurations.GeneralCommandLine
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val ListTimeout = 30.seconds
private val BootTimeout = 3.minutes
private val ScreenshotTimeout = 10.seconds
private val PasteTimeout = 10.seconds
private val ShutdownTimeout = 1.minutes

/** Blocking calls to `xcrun simctl`; run them off the EDT. */
object Simctl {
    fun list(): List<Simulator> = parseSimulators(run(ListTimeout, "list", "-j", "devices"))

    /** Boots the simulator if it isn't, and returns once it has finished booting. */
    fun boot(udid: String) {
        run(BootTimeout, "bootstatus", udid, "-b")
    }

    /** Shuts the simulator down and returns once it has. */
    fun shutdown(udid: String) {
        run(ShutdownTimeout, "shutdown", udid)
    }

    /** Asks CoreSimulator to shut the simulator down without waiting, so it carries on after the IDE quits. */
    fun startShutdown(udid: String) {
        GeneralCommandLine("xcrun", "simctl", "shutdown", udid).createProcess()
    }

    /** Saves the main display as a PNG at [file]. */
    fun screenshot(udid: String, file: Path) {
        run(ScreenshotTimeout, "io", udid, "screenshot", "--type=png", file.toString())
    }

    /** Replaces the simulator's pasteboard with [text]. */
    fun pbcopy(udid: String, text: String) {
        run(PasteTimeout, "pbcopy", udid, input = text)
    }

    private fun run(timeout: Duration, vararg arguments: String, input: String? = null): String =
        xcrun(timeout, "simctl", *arguments, input = input)
}
