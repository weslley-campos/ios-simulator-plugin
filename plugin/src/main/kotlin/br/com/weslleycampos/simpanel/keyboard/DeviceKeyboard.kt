package br.com.weslleycampos.simpanel.keyboard

import br.com.weslleycampos.simpanel.protocol.PluginMessage
import java.awt.event.KeyEvent

/** The keys that edit and move rather than type, which go down and up as they do on the Mac. */
private val EditingKeys = mapOf(
    KeyEvent.VK_ENTER to HidUsage.Enter,
    KeyEvent.VK_ESCAPE to HidUsage.Escape,
    KeyEvent.VK_BACK_SPACE to HidUsage.Backspace,
    KeyEvent.VK_TAB to HidUsage.Tab,
    KeyEvent.VK_DELETE to HidUsage.DeleteForward,
    KeyEvent.VK_HOME to HidUsage.Home,
    KeyEvent.VK_END to HidUsage.End,
    KeyEvent.VK_PAGE_UP to HidUsage.PageUp,
    KeyEvent.VK_PAGE_DOWN to HidUsage.PageDown,
    KeyEvent.VK_LEFT to HidUsage.Left,
    KeyEvent.VK_RIGHT to HidUsage.Right,
    KeyEvent.VK_UP to HidUsage.Up,
    KeyEvent.VK_DOWN to HidUsage.Down,
)

/** The accent keys, which type nothing until the letter that takes the accent. */
private val DeadKeys = KeyEvent.VK_DEAD_GRAVE..KeyEvent.VK_DEAD_SEMIVOICED_SOUND

/** The ⌘ shortcuts that edit text on the simulator instead of running the IDE's actions. */
private val CommandShortcuts = mapOf(
    KeyEvent.VK_A to HidUsage.letter('a'),
    KeyEvent.VK_C to HidUsage.letter('c'),
    KeyEvent.VK_X to HidUsage.letter('x'),
    KeyEvent.VK_Z to HidUsage.letter('z'),
)

/**
 * Turns the Mac keyboard into the simulator's. Editing and arrow keys go down and up with the
 * modifiers held, and typed characters go as the keys that type them on the simulator's US layout,
 * or are pasted when it has none. ⌘V pastes the Mac clipboard and ⌘A, ⌘C, ⌘X and ⌘Z edit on the
 * simulator; other shortcuts, with ⌘ or Control, are left to the IDE.
 */
class DeviceKeyboard(
    private val onKey: (PluginMessage.Key) -> Unit,
    private val onPaste: () -> Unit,
    private val onPasteText: (String) -> Unit,
) {
    /** The usages each held key went down with, its modifiers first, by key code. */
    private val held = mutableMapOf<Int, List<Int>>()
    private var highSurrogate: Char? = null

    /** Handles [e] and tells whether it is spent, or should go on to the IDE. */
    fun dispatch(e: KeyEvent): Boolean = when (e.id) {
        KeyEvent.KEY_PRESSED -> pressed(e)
        KeyEvent.KEY_TYPED -> typed(e)
        KeyEvent.KEY_RELEASED -> released(e)
        else -> false
    }

    /** Lifts the keys still down, for when the screen loses the keyboard while one is held. */
    fun releaseAll() {
        held.values.forEach(::lift)
        held.clear()
    }

    private fun pressed(e: KeyEvent): Boolean {
        if (e.isControlDown) return false
        val usage = EditingKeys[e.keyCode]
        return when {
            usage != null -> holdDown(e, usage)
            e.isMetaDown -> shortcut(e)
            else -> e.keyChar != KeyEvent.CHAR_UNDEFINED || e.keyCode in DeadKeys
        }
    }

    /** Presses an editing key; the Mac's repeats of a held key are spent, as the simulator repeats it. */
    private fun holdDown(e: KeyEvent, usage: Int): Boolean {
        if (e.keyCode == KeyEvent.VK_ESCAPE && e.modifiersEx != 0) return false
        if (e.keyCode in held) return true
        val usages = e.modifierUsages() + usage
        usages.forEach { onKey(PluginMessage.Key(it, down = true)) }
        held[e.keyCode] = usages
        return true
    }

    private fun shortcut(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.VK_V && !e.isShiftDown && !e.isAltDown) {
            onPaste()
            return true
        }
        val usage = CommandShortcuts[e.keyCode] ?: return false
        Keystroke(usage, e.modifierUsages()).presses().forEach(onKey)
        return true
    }

    private fun typed(e: KeyEvent): Boolean {
        if (e.isMetaDown || e.isControlDown) return false
        val character = e.keyChar
        when {
            character < ' ' || character == '\u007f' -> Unit
            character.isHighSurrogate() -> highSurrogate = character
            character.isLowSurrogate() -> highSurrogate?.let { onPasteText("$it$character") }
            else -> UsLayout.type(character)?.forEach { it.presses().forEach(onKey) }
                ?: onPasteText(character.toString())
        }
        if (!character.isHighSurrogate()) highSurrogate = null
        return true
    }

    private fun released(e: KeyEvent): Boolean {
        val usages = held.remove(e.keyCode) ?: return false
        lift(usages)
        return true
    }

    private fun lift(usages: List<Int>) {
        usages.asReversed().forEach { onKey(PluginMessage.Key(it, down = false)) }
    }

    private fun KeyEvent.modifierUsages() = buildList {
        if (isAltDown) add(HidUsage.LeftOption)
        if (isShiftDown) add(HidUsage.LeftShift)
        if (isMetaDown) add(HidUsage.LeftCommand)
    }
}
