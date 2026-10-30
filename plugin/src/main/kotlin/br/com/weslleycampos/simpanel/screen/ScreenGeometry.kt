package br.com.weslleycampos.simpanel.screen

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.Point2D
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The [device]'s screen centered in [area], at [scale] pixels of the area per pixel of the device, or
 * when it's null, at the largest scale that fits.
 */
fun placeScreen(area: Rectangle, device: Dimension, scale: Double? = null): Rectangle {
    if (area.isEmpty || device.width <= 0 || device.height <= 0) return Rectangle()
    val scale = scale ?: min(area.width.toDouble() / device.width, area.height.toDouble() / device.height)
    val width = (device.width * scale).roundToInt()
    val height = (device.height * scale).roundToInt()
    return Rectangle(
        area.x + (area.width - width) / 2,
        area.y + (area.height - height) / 2,
        width,
        height,
    )
}

/** Where [point] falls on [screen], as fractions of its width and height clamped to 0..1. */
fun fractionOn(screen: Rectangle, point: Point): Point2D.Double = Point2D.Double(
    ((point.x - screen.x).toDouble() / screen.width).coerceIn(0.0, 1.0),
    ((point.y - screen.y).toDouble() / screen.height).coerceIn(0.0, 1.0),
)
