package br.com.weslleycampos.simpanel.shutdown

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.simulators.Simctl
import br.com.weslleycampos.simpanel.simulators.Simulator
import br.com.weslleycampos.simpanel.ui.SimPanelService
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseHandler
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val ShutDownSettingId = "simpanel.shutDownOnClose"
private val Log = logger<ShutDownOnClose>()

/** As a project closes, offers to shut down the simulators its panel opened that are still booted. */
class ShutDownOnClose : ProjectCloseHandler {
    override fun canClose(project: Project): Boolean {
        val opened = project.serviceIfCreated<SimPanelService>()?.opened?.values?.toList().orEmpty()
        val choice = AdvancedSettings.getEnum(ShutDownSettingId, ShutDownChoice::class.java)
        if (opened.isEmpty() || choice == ShutDownChoice.KeepRunning) return true
        val booted = stillBooted(opened, listed(project))
        if (booted.isNotEmpty() && (choice == ShutDownChoice.ShutDown || ask(project, booted))) {
            booted.forEach { Simctl.startShutdown(it.udid) }
        }
        return true
    }

    private fun listed(project: Project): List<Simulator>? =
        runWithModalProgressBlocking(project, message("shutdown.checking")) {
            withContext(Dispatchers.IO) {
                try {
                    Simctl.list()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.warn(e)
                    null
                }
            }
        }

    private fun ask(project: Project, simulators: List<Simulator>): Boolean =
        MessageDialogBuilder.yesNo(
            message("shutdown.title"),
            message("shutdown.message", simulators.joinToString { it.name }),
        )
            .yesText(message("shutdown.yes"))
            .noText(message("shutdown.no"))
            .doNotAsk(object : DoNotAskOption.Adapter() {
                override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
                    if (!isSelected) return
                    val shutDown = exitCode == Messages.YES
                    AdvancedSettings.setEnum(
                        ShutDownSettingId,
                        if (shutDown) ShutDownChoice.ShutDown else ShutDownChoice.KeepRunning,
                    )
                }
            })
            .ask(project)
}

/** The [opened] simulators that [listed] still shows booted, or all of them when it couldn't be read. */
fun stillBooted(opened: List<Simulator>, listed: List<Simulator>?): List<Simulator> {
    val booted = listed?.filter { it.booted }?.map { it.udid }?.toSet() ?: return opened
    return opened.filter { it.udid in booted }
}
