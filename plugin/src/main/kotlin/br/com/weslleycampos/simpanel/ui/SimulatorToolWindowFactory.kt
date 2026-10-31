package br.com.weslleycampos.simpanel.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** The tool window's id in plugin.xml. */
const val SimulatorToolWindowId = "iOS Simulator"

class SimulatorToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SimulatorPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        content.setDisposer(panel)
        content.preferredFocusableComponent = panel.focusTarget
        toolWindow.contentManager.addContent(content)
    }
}
