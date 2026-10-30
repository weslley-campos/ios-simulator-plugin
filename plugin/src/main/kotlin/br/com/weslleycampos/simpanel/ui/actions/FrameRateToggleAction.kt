package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction

/** A settings menu checkbox that shows or hides the frame rate over the screen. */
class FrameRateToggleAction(private val controls: SimulatorControls) :
    DumbAwareToggleAction(message("action.showFrameRate")) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun isSelected(e: AnActionEvent) = controls.frameRateVisible

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        controls.frameRateVisible = state
    }
}
