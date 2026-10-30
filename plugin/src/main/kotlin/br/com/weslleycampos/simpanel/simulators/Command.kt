package br.com.weslleycampos.simpanel.simulators

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import kotlin.time.Duration

/** Runs [command], writing [input] to it, and returns what it printed; it blocks, so run it off the EDT. */
internal fun execute(timeout: Duration, vararg command: String, input: String? = null): String {
    val line = GeneralCommandLine(*command)
    val handler = CapturingProcessHandler(line)
    input?.let { text -> handler.processInput.use { it.write(text.encodeToByteArray()) } }
    val output = handler.runProcess(timeout.inWholeMilliseconds.toInt())
    if (output.isTimeout || output.exitCode != 0) {
        throw CommandException(line.commandLineString, output.stderr.ifBlank { output.stdout })
    }
    return output.stdout
}
