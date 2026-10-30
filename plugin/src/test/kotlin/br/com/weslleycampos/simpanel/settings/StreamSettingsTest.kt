package br.com.weslleycampos.simpanel.settings

import br.com.weslleycampos.simpanel.protocol.PluginMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamSettingsTest {
    @Test
    fun turnsTheQualityPercentIntoTheFractionTheHelperTakes() {
        assertEquals(PluginMessage.Settings(maxFps = 30, jpegQuality = 0.7), StreamSettings.of(30, 70))
    }

    @Test
    fun bringsTheSettingsWithinWhatTheHelperStreams() {
        assertEquals(PluginMessage.Settings(maxFps = 1, jpegQuality = 0.1), StreamSettings.of(0, 5))
        assertEquals(PluginMessage.Settings(maxFps = 60, jpegQuality = 1.0), StreamSettings.of(240, 150))
    }
}
