package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.ui.ControlIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

/** A toolbar toggle that shows or hides the device around the screen. */
class FrameToggleAction(private val controls: SimulatorControls) :
    DumbAwareToggleAction(message("action.showFrame"), null, ControlIcons.DeviceFrame) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun isSelected(e: AnActionEvent) = controls.frameVisible

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        controls.frameVisible = state
    }
}
