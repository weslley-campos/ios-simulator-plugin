package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.ui.ControlIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

/** A toolbar toggle that shows or hides the simulator's on-screen keyboard. */
class SoftwareKeyboardToggleAction(private val controls: SimulatorControls) :
    DumbAwareToggleAction(message("action.showSoftwareKeyboard"), null, ControlIcons.Keyboard) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun isSelected(e: AnActionEvent) = controls.softwareKeyboardShown

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        controls.softwareKeyboardShown = state
    }
}
