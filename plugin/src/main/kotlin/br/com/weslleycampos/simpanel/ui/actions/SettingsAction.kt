package br.com.weslleycampos.simpanel.ui.actions

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.runs.DeviceHubOnRun
import br.com.weslleycampos.simpanel.runs.DeviceHubOnRunSettingId
import br.com.weslleycampos.simpanel.settings.JpegQualitySettingId
import br.com.weslleycampos.simpanel.settings.MaxFpsSettingId
import br.com.weslleycampos.simpanel.shutdown.ShutDownChoice
import br.com.weslleycampos.simpanel.shutdown.ShutDownSettingId
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI

/** The frame rates the settings offer; a rate set in Advanced Settings shows among them too. */
private val FpsChoices = listOf(30, 60)

/**
 * A toolbar button that opens the panel's settings: the frame-rate counter and the Advanced
 * Settings of the plugin, each applied as it changes.
 */
class SettingsAction(private val controls: SimulatorControls) :
    DumbAwareAction(message("action.settings"), null, AllIcons.General.GearPlain) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(settingsPanel(), null)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .createPopup()
        val anchor = e.inputEvent?.component
        if (anchor != null) popup.showUnderneathOf(anchor) else popup.showInBestPositionFor(e.dataContext)
    }

    private fun settingsPanel(): DialogPanel = panel {
        row {
            checkBox(message("settings.showFrameRate"))
                .applyToComponent {
                    isSelected = controls.frameRateVisible
                    addItemListener { controls.frameRateVisible = isSelected }
                }
        }
        separator()
        row(message("settings.maxFps")) {
            val current = AdvancedSettings.getInt(MaxFpsSettingId).coerceIn(1, 60)
            segmentedButton((FpsChoices + current).distinct().sorted()) { text = it.toString() }
                .apply { selectedItem = current }
                .whenItemSelected { AdvancedSettings.setInt(MaxFpsSettingId, it) }
            label(message("settings.fps"))
        }
        row(message("settings.jpegQuality")) {
            spinner(10..100, step = 5)
                .applyToComponent {
                    number = AdvancedSettings.getInt(JpegQualitySettingId).coerceIn(10, 100)
                    // The UI DSL's onChanged does not support JBIntSpinner, so listen on the spinner itself.
                    addChangeListener { AdvancedSettings.setInt(JpegQualitySettingId, number) }
                }
            label("%")
        }
        row(message("settings.shutDownOnClose")) {
            segmentedButton(ShutDownChoice.entries) { text = it.toString() }
                .apply { selectedItem = AdvancedSettings.getEnum(ShutDownSettingId, ShutDownChoice::class.java) }
                .whenItemSelected { AdvancedSettings.setEnum(ShutDownSettingId, it) }
        }
        row(message("settings.deviceHubOnRun")) {
            segmentedButton(DeviceHubOnRun.entries) { text = it.toString() }
                .apply { selectedItem = AdvancedSettings.getEnum(DeviceHubOnRunSettingId, DeviceHubOnRun::class.java) }
                .whenItemSelected { AdvancedSettings.setEnum(DeviceHubOnRunSettingId, it) }
        }
    }.apply { border = JBUI.Borders.empty(8, 12) }
}
