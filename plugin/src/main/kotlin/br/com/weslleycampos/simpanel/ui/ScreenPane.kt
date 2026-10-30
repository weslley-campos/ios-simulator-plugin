package br.com.weslleycampos.simpanel.ui

import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLayeredPane

/** How far the zoom controls float from the panel's bottom right corner. */
private const val ControlsMargin = 8

/** The screen in a scroll pane, which a zoomed screen scrolls in, with [controls] floating over its bottom right. */
class ScreenPane(screen: ScreenView, private val controls: JComponent) : JBLayeredPane() {
    private val scrollPane = ScrollPaneFactory.createScrollPane(screen, true)

    init {
        add(scrollPane, JLayeredPane.DEFAULT_LAYER)
        add(controls, JLayeredPane.PALETTE_LAYER)
    }

    override fun doLayout() {
        scrollPane.setBounds(0, 0, width, height)
        val size = controls.preferredSize
        val margin = JBUI.scale(ControlsMargin)
        controls.setBounds(width - size.width - margin, height - size.height - margin, size.width, size.height)
    }

    override fun getPreferredSize(): Dimension = scrollPane.preferredSize
}
