package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.simulators.Simulator
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import java.awt.Graphics2D
import javax.swing.JList

/**
 * Shows a simulator as its name, runtime and state. In a picker too narrow for all of it, the
 * details drop from the right and then the name ends in an ellipsis.
 */
class SimulatorRenderer : ColoredListCellRenderer<Simulator>() {
    override fun customizeCellRenderer(
        list: JList<out Simulator>,
        value: Simulator?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        if (value == null) {
            append(message("devices.none"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
            return
        }
        val ellipsis = SimpleColoredComponent.DefaultFragmentTextClipper.INSTANCE
        appendWithClipping(value.name, SimpleTextAttributes.REGULAR_ATTRIBUTES, ellipsis)
        appendWithClipping("  ${value.runtime}", SimpleTextAttributes.GRAYED_ATTRIBUTES, WholeOrNothing)
        if (value.booted) {
            val state = "  ${message("devices.booted")}"
            appendWithClipping(state, SimpleTextAttributes.GRAYED_ATTRIBUTES, WholeOrNothing)
        }
    }
}

/** Draws a detail only when all of it fits. */
private object WholeOrNothing : SimpleColoredComponent.FragmentTextClipper {
    override fun clipText(
        component: SimpleColoredComponent,
        g2: Graphics2D,
        fragmentIndex: Int,
        text: String,
        availTextWidth: Int,
    ): String = if (component.getFontMetrics(g2.font).stringWidth(text) <= availTextWidth) text else ""
}
