package br.com.weslleycampos.simpanel.simulators

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import kotlin.time.Duration

/**
 * Runs the Xcode tool `xcrun` finds, writing [input] to it, and returns what it printed; it blocks, so
 * run it off the EDT.
 */
internal fun xcrun(timeout: Duration, vararg arguments: String, input: String? = null): String {
    val command = GeneralCommandLine("xcrun", *arguments)
    val handler = CapturingProcessHandler(command)
    input?.let { text -> handler.processInput.use { it.write(text.encodeToByteArray()) } }
    val output = handler.runProcess(timeout.inWholeMilliseconds.toInt())
    if (output.isTimeout || output.exitCode != 0) {
        throw XcrunException(command.commandLineString, output.stderr.ifBlank { output.stdout })
    }
    return output.stdout
}
