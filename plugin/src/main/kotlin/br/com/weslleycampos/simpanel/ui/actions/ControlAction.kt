package br.com.weslleycampos.simpanel.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

/** A toolbar button that does [perform] to the simulator, enabled while one is attached. */
class ControlAction(
    text: String,
    icon: Icon,
    private val controls: SimulatorControls,
    private val perform: SimulatorControls.() -> Unit,
) : DumbAwareAction(text, null, icon) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = controls.attached
    }

    override fun actionPerformed(e: AnActionEvent) {
        controls.perform()
    }
}
