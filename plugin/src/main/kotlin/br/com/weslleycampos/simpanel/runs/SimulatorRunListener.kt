package br.com.weslleycampos.simpanel.runs

import br.com.weslleycampos.simpanel.ui.SimPanelService
import br.com.weslleycampos.simpanel.ui.SimulatorToolWindowId
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.service
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

const val DeviceHubOnRunSettingId = "simpanel.deviceHubOnRun"

/**
 * Shows the simulator an iOS run starts on in the panel, opening the tool window, and hides the
 * DeviceHub window the run opens on it, unless Advanced Settings leaves it open.
 */
class SimulatorRunListener(private val project: Project) : ExecutionListener {
    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        val udid = simulatorUdid(env.executionTarget.id) ?: return
        project.service<SimPanelService>().runs.tryEmit(udid)
        val toolWindows = ToolWindowManager.getInstance(project)
        toolWindows.invokeLater { toolWindows.getToolWindow(SimulatorToolWindowId)?.show() }
        if (AdvancedSettings.getEnum(DeviceHubOnRunSettingId, DeviceHubOnRun::class.java) == DeviceHubOnRun.Hide) {
            project.service<DeviceHubHider>().hideWhileLaunching(env.executionId, udid)
        }
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
        project.service<DeviceHubHider>().stop(env.executionId)
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int,
    ) {
        project.service<DeviceHubHider>().stop(env.executionId)
    }
}
