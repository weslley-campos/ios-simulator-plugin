package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.screen.Zoom
import br.com.weslleycampos.simpanel.screen.ZoomLevels
import br.com.weslleycampos.simpanel.ui.ScreenView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

/** A zoom button that zooms the screen to what [pick] picks, enabled while that changes the zoom. */
class ZoomAction(
    text: String,
    icon: Icon,
    private val screen: ScreenView,
    private val pick: ZoomLevels.() -> Zoom?,
) : DumbAwareAction(text, null, icon) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = screen.zoomLevels?.pick() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        screen.zoomLevels?.pick()?.let { screen.zoom = it }
    }
}
