package br.com.weslleycampos.simpanel.screen

import kotlin.math.abs

/** The zooms that zooming in and out step through, as on Running Devices. */
private val Steps = listOf(0.25, 0.33, 0.5, 0.67, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0)

/** Below this, two zooms show the same percentage. */
private const val Tolerance = 0.005

/** How large the screen is shown. */
sealed interface Zoom {
    /** As large as fits in the panel. */
    data object Fit : Zoom

    /** At [scale] pixels of the Mac's display per pixel of the simulator's screen, scrolled when it doesn't fit. */
    data class Fixed(val scale: Double) : Zoom

    companion object {
        val ActualSize = Fixed(1.0)
    }
}

/**
 * The screen shown at [shown] pixels of the Mac's display per pixel of the simulator's, where [fit]
 * fits it in the panel, and the zoom each of the zoom buttons picks, or null when it changes nothing.
 */
class ZoomLevels(val shown: Double, val fit: Double) {
    val zoomedIn: Zoom? = Steps.firstOrNull { it > shown + Tolerance }?.let(Zoom::Fixed)

    /** The step below; zooming out stops at fitting the screen, as it would only leave it smaller. */
    val zoomedOut: Zoom? = when {
        shown <= fit + Tolerance -> null
        else -> Steps.lastOrNull { it < shown - Tolerance && it > fit + Tolerance }?.let(Zoom::Fixed) ?: Zoom.Fit
    }

    val actualSize: Zoom? = Zoom.ActualSize.takeIf { abs(shown - it.scale) > Tolerance }

    val fitted: Zoom? = Zoom.Fit.takeIf { abs(shown - fit) > Tolerance }
}
