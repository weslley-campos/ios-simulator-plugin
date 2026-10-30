package br.com.weslleycampos.simpanel.ui

import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals

class PickerRowLayoutTest {
    private val picker = sized(width = 240, height = 30)
    private val buttons = sized(width = 60, height = 24)
    private val row = JPanel(PickerRowLayout(picker, buttons)).apply {
        add(picker)
        add(buttons)
    }

    @Test
    fun givesThePickerItsPreferredWidthWhenTheRowHasRoom() {
        layOut(width = 400)

        assertEquals(Rectangle(0, 0, 240, 30), picker.bounds)
        assertEquals(Rectangle(240, 3, 60, 24), buttons.bounds)
    }

    @Test
    fun shrinksOnlyThePickerWhenTheRowIsNarrow() {
        layOut(width = 200)

        assertEquals(Rectangle(0, 0, 140, 30), picker.bounds)
        assertEquals(Rectangle(140, 3, 60, 24), buttons.bounds)
    }

    @Test
    fun keepsTheButtonsWholeWhenTheyAloneFillTheRow() {
        layOut(width = 50)

        assertEquals(0, picker.width)
        assertEquals(Rectangle(0, 3, 60, 24), buttons.bounds)
    }

    @Test
    fun asksForRoomForBothAndNeedsRoomForTheButtons() {
        assertEquals(Dimension(300, 30), row.preferredSize)
        assertEquals(Dimension(60, 30), row.minimumSize)
    }

    private fun layOut(width: Int) {
        row.setSize(width, 30)
        row.doLayout()
    }

    private fun sized(width: Int, height: Int) = object : JComponent() {
        override fun getPreferredSize() = Dimension(width, height)
    }
}
