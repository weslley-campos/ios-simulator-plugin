package br.com.weslleycampos.simpanel.ui

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import java.awt.Rectangle

/**
 * Lays a vertical toolbar out in one column, as [ToolbarLayoutStrategy.NOWRAP_STRATEGY] does, but
 * centers each button in it, so icons line up under a wider text button instead of to its left.
 */
object CenteredColumnLayout : ToolbarLayoutStrategy by ToolbarLayoutStrategy.NOWRAP_STRATEGY {
    override fun calculateBounds(toolbar: ActionToolbar): List<Rectangle> {
        val bounds = ToolbarLayoutStrategy.NOWRAP_STRATEGY.calculateBounds(toolbar)
        val left = bounds.minOfOrNull { it.x } ?: return bounds
        val column = bounds.maxOf { it.width }
        return bounds.map { Rectangle(left + (column - it.width) / 2, it.y, it.width, it.height) }
    }
}
