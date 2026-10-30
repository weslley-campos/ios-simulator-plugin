package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import com.intellij.ide.actions.RevealFileAction
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.nio.file.Path

private const val GroupId = "iOS Simulator"

object SimPanelNotifications {
    fun screenshotSaved(project: Project, file: Path) {
        val text = message("screenshot.saved", file.fileName.toString())
        group().createNotification(text, NotificationType.INFORMATION)
            .addAction(NotificationAction.createSimpleExpiring(RevealFileAction.getActionName()) {
                RevealFileAction.openFile(file)
            })
            .notify(project)
    }

    fun failed(project: Project, title: String, detail: String?) {
        group().createNotification(title, detail.orEmpty(), NotificationType.ERROR).notify(project)
    }

    private fun group() = NotificationGroupManager.getInstance().getNotificationGroup(GroupId)
}
