package br.com.weslleycampos.simpanel.ui

import br.com.weslleycampos.simpanel.SimPanelBundle.message
import br.com.weslleycampos.simpanel.simulators.Simulator
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList

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
        append(value.name)
        append("  ${value.runtime}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        if (value.booted) append("  ${message("devices.booted")}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
}
