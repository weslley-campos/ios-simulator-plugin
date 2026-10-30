package br.com.weslleycampos.simpanel.screen

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ScaledImageTest {
    private val bezel = BufferedImage(1482, 2976, BufferedImage.TYPE_INT_ARGB).apply {
        createGraphics().run {
            color = Color.RED
            fillRect(0, 0, width, height)
            dispose()
        }
    }

    @Test
    fun scalesToTheSizeItIsDrawnAt() {
        val scaled = ScaledImage(bezel).at(247, 496)

        assertEquals(247, scaled.width)
        assertEquals(496, scaled.height)
        assertEquals(Color.RED.rgb, scaled.getRGB(123, 248))
    }

    @Test
    fun keepsTheCopyUntilAnotherSizeIsAskedFor() {
        val image = ScaledImage(bezel)
        val scaled = image.at(247, 496)

        assertSame(scaled, image.at(247, 496))
        assertNotSame(scaled, image.at(494, 992))
    }

    @Test
    fun leavesEnlargingToTheDrawing() {
        val image = ScaledImage(bezel)

        assertSame(bezel, image.at(1482, 2976))
        assertSame(bezel, image.at(2964, 5952))
    }
}
