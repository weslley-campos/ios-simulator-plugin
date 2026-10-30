package br.com.weslleycampos.simpanel.settings

import br.com.weslleycampos.simpanel.protocol.PluginMessage
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.options.advanced.AdvancedSettingsChangeListener

const val MaxFpsSettingId = "simpanel.maxFps"
const val JpegQualitySettingId = "simpanel.jpegQuality"

/** How the helper streams the screen, as Advanced Settings sets it. */
object StreamSettings {
    fun current(): PluginMessage.Settings = of(
        maxFps = AdvancedSettings.getInt(MaxFpsSettingId),
        jpegQualityPercent = AdvancedSettings.getInt(JpegQualitySettingId),
    )

    /** The settings within what the helper streams: 1 to 60 frames a second, at a quality of 10 to 100%. */
    fun of(maxFps: Int, jpegQualityPercent: Int) = PluginMessage.Settings(
        maxFps = maxFps.coerceIn(1, 60),
        jpegQuality = jpegQualityPercent.coerceIn(10, 100) / 100.0,
    )

    /** Calls [onChange] with the settings each time one of them changes, until [parent] is disposed. */
    fun watch(parent: Disposable, onChange: (PluginMessage.Settings) -> Unit) {
        val listener = object : AdvancedSettingsChangeListener {
            override fun advancedSettingChanged(id: String, oldValue: Any, newValue: Any) {
                if (id == MaxFpsSettingId || id == JpegQualitySettingId) onChange(current())
            }
        }
        ApplicationManager.getApplication().messageBus.connect(parent)
            .subscribe(AdvancedSettingsChangeListener.TOPIC, listener)
    }
}
