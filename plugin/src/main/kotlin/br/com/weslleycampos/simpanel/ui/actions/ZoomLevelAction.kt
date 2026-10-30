package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.ui.ScreenView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import kotlin.math.roundToInt

/** The zoom the screen is shown at, as a percentage; clicking it folds the zoom controls or unfolds them. */
class ZoomLevelAction(
    private val screen: ScreenView,
    private val expanded: () -> Boolean,
    private val toggle: () -> Unit,
) : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun displayTextInToolbar() = true

    override fun update(e: AnActionEvent) {
        val shown = screen.zoomLevels?.shown
        e.presentation.text = shown?.let { "${(it * 100).roundToInt()}%" }.orEmpty()
        e.presentation.description = message(if (expanded()) "zoom.hide" else "zoom.show")
        e.presentation.isEnabled = shown != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        toggle()
    }
}
