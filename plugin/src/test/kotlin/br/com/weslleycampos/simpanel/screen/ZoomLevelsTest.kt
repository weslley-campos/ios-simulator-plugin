package br.com.weslleycampos.simpanel.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZoomLevelsTest {
    @Test
    fun zoomsInToTheNextStep() {
        assertEquals(Zoom.Fixed(0.5), ZoomLevels(shown = 0.47, fit = 0.47).zoomedIn)
        assertEquals(Zoom.Fixed(0.67), ZoomLevels(shown = 0.498, fit = 0.498).zoomedIn, "50% shows already")
        assertNull(ZoomLevels(shown = 4.0, fit = 0.47).zoomedIn)
    }

    @Test
    fun zoomsOutStepByStepUntilTheScreenFits() {
        assertEquals(Zoom.Fixed(0.75), ZoomLevels(shown = 1.0, fit = 0.47).zoomedOut)
        assertEquals(Zoom.Fit, ZoomLevels(shown = 0.5, fit = 0.47).zoomedOut)
        assertNull(ZoomLevels(shown = 0.47, fit = 0.47).zoomedOut)
        assertNull(ZoomLevels(shown = 1.0, fit = 1.2).zoomedOut, "the screen is smaller than fitting already")
    }

    @Test
    fun zoomsToActualSizeAndToFitUnlessShownSo() {
        val fitting = ZoomLevels(shown = 0.47, fit = 0.47)
        val actualSize = ZoomLevels(shown = 1.0, fit = 0.47)

        assertEquals(Zoom.ActualSize, fitting.actualSize)
        assertNull(fitting.fitted)
        assertNull(actualSize.actualSize)
        assertEquals(Zoom.Fit, actualSize.fitted)
    }
}
