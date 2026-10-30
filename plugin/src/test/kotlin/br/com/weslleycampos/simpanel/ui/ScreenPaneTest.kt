package br.com.weslleycampos.simpanel.ui

import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenPaneTest {
    private val controls = JPanel().apply { preferredSize = Dimension(30, 170) }
    private val pane = ScreenPane(ScreenView(), controls)

    @Test
    fun floatsTheControlsOverTheScreen() {
        assertEquals(0, pane.getComponentZOrder(controls))
    }

    @Test
    fun putsTheControlsInTheBottomRightCorner() {
        pane.setSize(400, 800)
        pane.doLayout()

        assertEquals(Rectangle(362, 622, 30, 170), controls.bounds)
    }
}
