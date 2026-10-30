package br.com.weslleycampos.simpanel.ui

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager

/**
 * Lays out the simulator picker and the buttons after it in one row. The buttons always get their
 * preferred width; the picker gets its own, or whatever the buttons leave of a narrower row.
 */
class PickerRowLayout(private val picker: Component, private val buttons: Component) : LayoutManager {
    override fun addLayoutComponent(name: String?, component: Component) = Unit

    override fun removeLayoutComponent(component: Component) = Unit

    override fun preferredLayoutSize(parent: Container) =
        size(parent, picker.preferredSize.width + buttons.preferredSize.width)

    override fun minimumLayoutSize(parent: Container) = size(parent, buttons.preferredSize.width)

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val room = parent.width - insets.left - insets.right
        val buttonsWidth = buttons.preferredSize.width
        val pickerWidth = picker.preferredSize.width.coerceAtMost(room - buttonsWidth).coerceAtLeast(0)
        val rowHeight = parent.height - insets.top - insets.bottom
        place(picker, insets.left, insets.top, pickerWidth, rowHeight)
        place(buttons, insets.left + pickerWidth, insets.top, buttonsWidth, rowHeight)
    }

    private fun place(component: Component, x: Int, top: Int, width: Int, rowHeight: Int) {
        val height = component.preferredSize.height.coerceAtMost(rowHeight)
        component.setBounds(x, top + (rowHeight - height) / 2, width, height)
    }

    private fun size(parent: Container, width: Int): Dimension {
        val insets = parent.insets
        val height = maxOf(picker.preferredSize.height, buttons.preferredSize.height)
        return Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom)
    }
}
