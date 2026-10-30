package br.com.weslleycampos.simpanel.screen

import br.com.weslleycampos.simpanel.protocol.DeviceButton
import java.awt.Point
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacedFrameTest {
    private val frame = proMaxFrame()
    private val power = frame.buttons.single()

    @Test
    fun placesTheFrameAtItsOwnSizeWhenTheAreaMatchesIt() {
        val placed = assertNotNull(placeFrame(Rectangle(0, 0, 494, 992), frame))

        assertEquals(Rectangle(0, 0, 494, 992), placed.device)
        assertEquals(Rectangle(27, 18, 440, 956), placed.screen)
    }

    @Test
    fun scalesTheFrameToFitAndCentersIt() {
        val placed = assertNotNull(placeFrame(Rectangle(0, 0, 600, 496), frame))

        assertEquals(Rectangle(177, 0, 247, 496), placed.device)
        assertEquals(Rectangle(190, 9, 220, 478), placed.screen)
        assertEquals(Rectangle(415, 147, 8, 50), placed.buttons[power])
    }

    @Test
    fun roundsTheScreensCornersInsideTheBody() {
        val placed = assertNotNull(placeFrame(Rectangle(0, 0, 494, 992), frame))

        assertFalse(placed.screenClip.contains(28.0, 19.0), "the screen's square corner")
        assertTrue(placed.screenClip.contains(46.0, 37.0), "the hole's corner, where it crosses the diagonal")
        assertTrue(placed.screenClip.contains(26.0, 500.0), "the hole's edge, under the bezel")
    }

    @Test
    fun findsTheButtonUnderAPoint() {
        val placed = assertNotNull(placeFrame(Rectangle(0, 0, 600, 496), frame))

        assertEquals(DeviceButton.Power, placed.buttonAt(Point(418, 160))?.button)
        assertNull(placed.buttonAt(Point(300, 300)))
    }

    @Test
    fun placesNothingInAnEmptyArea() {
        assertNull(placeFrame(Rectangle(), frame))
    }
}
