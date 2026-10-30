package br.com.weslleycampos.simpanel.keyboard

import br.com.weslleycampos.simpanel.protocol.PluginMessage

/** A key pressed and released with [modifiers] held around it, all given as [HidUsage]s. */
data class Keystroke(val usage: Int, val modifiers: List<Int> = emptyList()) {
    /** The modifiers and the key going down, then coming up the other way round. */
    fun presses(): List<PluginMessage.Key> {
        val usages = modifiers + usage
        return usages.map { PluginMessage.Key(it, down = true) } +
            usages.asReversed().map { PluginMessage.Key(it, down = false) }
    }

    companion object {
        val Paste = Keystroke(HidUsage.letter('v'), listOf(HidUsage.LeftCommand))
    }
}
