package br.com.weslleycampos.simpanel.keyboard

import br.com.weslleycampos.simpanel.protocol.PluginMessage
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceKeyboardTest {
    private val source = JPanel()
    private val keys = mutableListOf<PluginMessage.Key>()
    private var pastes = 0
    private val pastedText = mutableListOf<String>()
    private val keyboard = DeviceKeyboard(
        onKey = { keys += it },
        onPaste = { pastes++ },
        onPasteText = { pastedText += it },
    )

    private fun pressed(keyCode: Int, character: Char = KeyEvent.CHAR_UNDEFINED, modifiers: Int = 0) =
        keyboard.dispatch(KeyEvent(source, KeyEvent.KEY_PRESSED, 0, modifiers, keyCode, character))

    private fun released(keyCode: Int, character: Char = KeyEvent.CHAR_UNDEFINED, modifiers: Int = 0) =
        keyboard.dispatch(KeyEvent(source, KeyEvent.KEY_RELEASED, 0, modifiers, keyCode, character))

    private fun typed(character: Char, modifiers: Int = 0) =
        keyboard.dispatch(KeyEvent(source, KeyEvent.KEY_TYPED, 0, modifiers, KeyEvent.VK_UNDEFINED, character))

    private fun down(vararg usages: Int) = usages.map { PluginMessage.Key(it, down = true) }

    private fun up(vararg usages: Int) = usages.map { PluginMessage.Key(it, down = false) }

    @Test
    fun typesACharacterAsTheKeysThatTypeIt() {
        assertTrue(pressed(KeyEvent.VK_H, 'H', InputEvent.SHIFT_DOWN_MASK))
        assertTrue(typed('H', InputEvent.SHIFT_DOWN_MASK))
        released(KeyEvent.VK_H, 'H', InputEvent.SHIFT_DOWN_MASK)

        assertEquals(Keystroke(HidUsage.letter('h'), listOf(HidUsage.LeftShift)).presses(), keys)
    }

    @Test
    fun holdsAnEditingKeyWithItsModifiersUntilItComesUp() {
        val modifiers = InputEvent.ALT_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK

        assertTrue(pressed(KeyEvent.VK_LEFT, modifiers = modifiers))
        assertTrue(pressed(KeyEvent.VK_LEFT, modifiers = modifiers))
        assertEquals(down(HidUsage.LeftOption, HidUsage.LeftShift, HidUsage.Left), keys)

        assertTrue(released(KeyEvent.VK_LEFT, modifiers = modifiers))
        assertEquals(up(HidUsage.Left, HidUsage.LeftShift, HidUsage.LeftOption), keys.drop(3))
    }

    @Test
    fun spendsTheCharacterAnEditingKeyTypes() {
        pressed(KeyEvent.VK_BACK_SPACE, '\b')

        assertTrue(typed('\b'))
        assertEquals(down(HidUsage.Backspace), keys)
    }

    @Test
    fun pastesTheClipboardForCommandV() {
        assertTrue(pressed(KeyEvent.VK_V, 'v', InputEvent.META_DOWN_MASK))

        assertEquals(1, pastes)
        assertEquals(emptyList(), keys)
    }

    @Test
    fun editsTextOnTheSimulatorWithItsCommandShortcuts() {
        assertTrue(pressed(KeyEvent.VK_Z, 'z', InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK))

        assertEquals(
            Keystroke(HidUsage.letter('z'), listOf(HidUsage.LeftShift, HidUsage.LeftCommand)).presses(),
            keys,
        )
    }

    @Test
    fun leavesTheIdesShortcutsAndModifiersToIt() {
        assertFalse(pressed(KeyEvent.VK_COMMA, ',', InputEvent.META_DOWN_MASK))
        assertFalse(pressed(KeyEvent.VK_R, 'r', InputEvent.CTRL_DOWN_MASK))
        assertFalse(pressed(KeyEvent.VK_ENTER, '\n', InputEvent.CTRL_DOWN_MASK))
        assertFalse(pressed(KeyEvent.VK_ESCAPE, '\u001b', InputEvent.SHIFT_DOWN_MASK))
        assertFalse(pressed(KeyEvent.VK_F12))
        assertFalse(pressed(KeyEvent.VK_SHIFT, modifiers = InputEvent.SHIFT_DOWN_MASK))
        assertFalse(typed('v', InputEvent.META_DOWN_MASK))

        assertEquals(emptyList(), keys)
    }

    @Test
    fun pastesWhatTheLayoutHasNoKeysFor() {
        typed('中')
        typed('\uD83D')
        typed('\uDE00')

        assertEquals(listOf("中", "😀"), pastedText)
        assertEquals(emptyList(), keys)
    }

    @Test
    fun liftsTheHeldKeysWhenReleasedAll() {
        pressed(KeyEvent.VK_DOWN)

        keyboard.releaseAll()
        pressed(KeyEvent.VK_DOWN)

        assertEquals(down(HidUsage.Down) + up(HidUsage.Down) + down(HidUsage.Down), keys)
    }
}
