package br.com.weslleycampos.simpanel.protocol

import kotlinx.serialization.Serializable
import java.awt.geom.Rectangle2D

/** A rectangle in points from the top left of the device the helper draws. */
@Serializable
data class ChromeRect(val x: Double, val y: Double, val width: Double, val height: Double) {
    fun toRectangle() = Rectangle2D.Double(x, y, width, height)
}
