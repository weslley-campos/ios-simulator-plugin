package br.com.weslleycampos.simpanel.screen

import java.awt.Point
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.min
import kotlin.math.roundToInt

/** How far, in points, the screen draws past the bezel's hole, so the clip's jagged edge stays under the bezel. */
private const val ClipOverlap = 2.0

/** A device frame fitted into the panel, in the panel's coordinates. */
class PlacedFrame(
    val frame: DeviceFrame,
    val device: Rectangle,
    val screen: Rectangle,
    /** The screen with its corners rounded, which keeps them inside the body. */
    val screenClip: Shape,
    val buttons: Map<FrameButton, Rectangle>,
) {
    fun buttonAt(point: Point): FrameButton? = buttons.entries.firstOrNull { it.value.contains(point) }?.key
}

/**
 * [frame] centered in [area], at [scale] pixels of the area per point or, when it's null, at the
 * largest size that fits; null when the area is empty.
 */
fun placeFrame(area: Rectangle, frame: DeviceFrame, scale: Double? = null): PlacedFrame? {
    if (area.isEmpty || frame.width <= 0 || frame.height <= 0) return null
    val scale = scale ?: min(area.width / frame.width, area.height / frame.height)
    val x = area.x + (area.width - frame.width * scale) / 2
    val y = area.y + (area.height - frame.height * scale) / 2
    // Rounding the edges rather than the sizes keeps the screen's edges under the bezel's hole.
    fun place(rect: Rectangle2D): Rectangle {
        val left = (x + rect.minX * scale).roundToInt()
        val top = (y + rect.minY * scale).roundToInt()
        val right = (x + rect.maxX * scale).roundToInt()
        val bottom = (y + rect.maxY * scale).roundToInt()
        return Rectangle(left, top, right - left, bottom - top)
    }
    val screen = place(frame.screen)
    val overlap = ClipOverlap * scale
    val arc = 2 * (frame.screenCornerRadius + ClipOverlap) * scale
    return PlacedFrame(
        frame = frame,
        device = place(Rectangle2D.Double(0.0, 0.0, frame.width, frame.height)),
        screen = screen,
        screenClip = RoundRectangle2D.Double(
            screen.x - overlap,
            screen.y - overlap,
            screen.width + 2 * overlap,
            screen.height + 2 * overlap,
            arc,
            arc,
        ),
        buttons = frame.buttons.associateWith { place(it.bounds) },
    )
}
