package br.com.weslleycampos.simpanel.keyboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsLayoutTest {
    private val shift = listOf(HidUsage.LeftShift)
    private val option = listOf(HidUsage.LeftOption)

    @Test
    fun typesLettersAndSymbolsWithShiftWhereTheyNeedIt() {
        assertEquals(listOf(Keystroke(HidUsage.letter('h'))), UsLayout.type('h'))
        assertEquals(listOf(Keystroke(HidUsage.letter('h'), shift)), UsLayout.type('H'))
        assertEquals(listOf(Keystroke(0x1e, shift)), UsLayout.type('!'))
        assertEquals(listOf(Keystroke(HidUsage.Space)), UsLayout.type(' '))
        assertEquals(listOf(Keystroke(0x34, shift)), UsLayout.type('"'))
    }

    @Test
    fun typesWhatOptionTypesOnAMac() {
        assertEquals(listOf(Keystroke(HidUsage.letter('c'), option)), UsLayout.type('ç'))
        assertEquals(
            listOf(Keystroke(HidUsage.letter('c'), listOf(HidUsage.LeftOption, HidUsage.LeftShift))),
            UsLayout.type('Ç'),
        )
        assertEquals(listOf(Keystroke(0x33, option)), UsLayout.type('…'))
    }

    @Test
    fun typesAnAccentWithItsDeadKeyThenTheLetter() {
        val acute = Keystroke(HidUsage.letter('e'), option)

        assertEquals(listOf(acute, Keystroke(HidUsage.letter('a'))), UsLayout.type('á'))
        assertEquals(listOf(acute, Keystroke(HidUsage.letter('e'), shift)), UsLayout.type('É'))
        assertEquals(
            listOf(Keystroke(HidUsage.letter('n'), option), Keystroke(HidUsage.letter('o'))),
            UsLayout.type('õ'),
        )
    }

    @Test
    fun hasNoKeysForWhatTheLayoutCantType() {
        assertNull(UsLayout.type('中'))
        assertNull(UsLayout.type('ẽ'))
    }
}
