package br.com.weslleycampos.simpanel.screen

import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.Point2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenGeometryTest {
    private val iPhone = Dimension(1206, 2622)

    @Test
    fun fitsATallScreenToTheAreaHeightAndCentersIt() {
        assertEquals(Rectangle(359, 0, 402, 874), placeScreen(Rectangle(0, 0, 1120, 874), iPhone))
    }

    @Test
    fun fitsAWideScreenToTheAreaWidthAndCentersIt() {
        assertEquals(Rectangle(0, 300, 874, 402), placeScreen(Rectangle(0, 0, 874, 1002), Dimension(2622, 1206)))
    }

    @Test
    fun keepsTheAreaOffset() {
        assertEquals(Rectangle(8, 8, 402, 874), placeScreen(Rectangle(8, 8, 402, 874), iPhone))
    }

    @Test
    fun placesTheScreenAtAScaleCenteredInTheArea() {
        assertEquals(Rectangle(258, 125, 603, 1311), placeScreen(Rectangle(0, 0, 1120, 1562), iPhone, 0.5))
        assertEquals(Rectangle(8, 8, 1206, 2622), placeScreen(Rectangle(8, 8, 1206, 2622), iPhone, 1.0))
    }

    @Test
    fun hasNoScreenWithoutAnAreaOrADevice() {
        assertTrue(placeScreen(Rectangle(0, 0, 0, 874), iPhone).isEmpty)
        assertTrue(placeScreen(Rectangle(0, 0, 402, 874), Dimension(0, 0)).isEmpty)
    }

    @Test
    fun mapsAPointToFractionsOfTheScreen() {
        val screen = Rectangle(100, 0, 400, 800)

        assertEquals(Point2D.Double(0.5, 0.25), fractionOn(screen, Point(300, 200)))
        assertEquals(Point2D.Double(0.0, 0.0), fractionOn(screen, Point(100, 0)))
    }

    @Test
    fun clampsAPointDraggedOffTheScreen() {
        val screen = Rectangle(100, 0, 400, 800)

        assertEquals(Point2D.Double(0.0, 1.0), fractionOn(screen, Point(40, 900)))
        assertEquals(Point2D.Double(1.0, 0.0), fractionOn(screen, Point(600, -20)))
    }
}
