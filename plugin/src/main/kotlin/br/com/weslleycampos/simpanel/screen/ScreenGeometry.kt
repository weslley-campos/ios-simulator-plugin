package br.com.weslleycampos.simpanel.screen

import br.com.weslleycampos.simpanel.simulators.Orientation
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.geom.AffineTransform
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
fun fractionOn(screen: Rectangle, point: Point2D): Point2D.Double = Point2D.Double(
    ((point.x - screen.x).toDouble() / screen.width).coerceIn(0.0, 1.0),
    ((point.y - screen.y).toDouble() / screen.height).coerceIn(0.0, 1.0),
)

/**
 * The space the screen and the device around it are placed in, upright, to show them in [area] turned
 * to [orientation]: at the origin, with the area's sides swapped when it's sideways.
 */
fun upright(area: Rectangle, orientation: Orientation): Rectangle =
    if (orientation.sideways) Rectangle(area.height, area.width) else Rectangle(area.width, area.height)

/** Turns the [upright] space onto [area], as the screen shows turned to [orientation]. */
fun turnOnto(area: Rectangle, orientation: Orientation): AffineTransform {
    val (x, y) = when (orientation) {
        Orientation.Portrait -> 0 to 0
        Orientation.LandscapeLeft -> 0 to area.height
        Orientation.PortraitUpsideDown -> area.width to area.height
        Orientation.LandscapeRight -> area.width to 0
    }
    return AffineTransform.getTranslateInstance((area.x + x).toDouble(), (area.y + y).toDouble()).apply {
        quadrantRotate(-orientation.quarterTurnsLeft)
    }
}
