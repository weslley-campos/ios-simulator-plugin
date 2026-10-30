package br.com.weslleycampos.simpanel.simulators

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val ListTimeout = 30.seconds
private val BootTimeout = 3.minutes

/** Blocking calls to `xcrun simctl`; run them off the EDT. */
object Simctl {
    fun list(): List<Simulator> = parseSimulators(run(ListTimeout, "list", "-j", "devices"))

    /** Boots the simulator if it isn't, and returns once it has finished booting. */
    fun boot(udid: String) {
        run(BootTimeout, "bootstatus", udid, "-b")
    }

    private fun run(timeout: Duration, vararg arguments: String): String {
        val command = GeneralCommandLine("xcrun", "simctl", *arguments)
        val output = ExecUtil.execAndGetOutput(command, timeout.inWholeMilliseconds.toInt())
        if (output.isTimeout || output.exitCode != 0) {
            throw SimctlException(command.commandLineString, output.stderr.ifBlank { output.stdout })
        }
        return output.stdout
    }
}
