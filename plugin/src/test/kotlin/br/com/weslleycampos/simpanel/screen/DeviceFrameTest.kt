package br.com.weslleycampos.simpanel.screen

import br.com.weslleycampos.simpanel.protocol.ChromeButton
import br.com.weslleycampos.simpanel.protocol.ChromeRect
import br.com.weslleycampos.simpanel.protocol.DeviceButton
import br.com.weslleycampos.simpanel.protocol.HelperMessage
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeviceFrameTest {
    @Test
    fun readsTheImagesTheHelperSent() {
        val frame = DeviceFrame.of(chrome(bezel = png(1482, 2976)))

        assertEquals(Rectangle2D.Double(27.0, 18.0, 440.0, 956.0), frame.screen)
        assertEquals(63.0, frame.screenCornerRadius)
        assertEquals(1482, frame.bezel.source.width)
        assertEquals(DeviceButton.Power, frame.buttons.single().button)
        assertEquals(48, frame.buttons.single().pressedImage.source.width)
    }

    @Test
    fun refusesAnImageItCannotRead() {
        assertFailsWith<IOException> { DeviceFrame.of(chrome(bezel = byteArrayOf(1, 2, 3))) }
    }

    private fun chrome(bezel: ByteArray) = HelperMessage.Chrome(
        width = 494.0,
        height = 992.0,
        scale = 3.0,
        screen = ChromeRect(27.0, 18.0, 440.0, 956.0),
        screenCornerRadius = 63.0,
        bezel = bezel,
        buttons = listOf(
            ChromeButton(
                button = DeviceButton.Power,
                frame = ChromeRect(477.0, 293.0, 16.0, 101.0),
                onTop = false,
                image = png(48, 303),
                pressedImage = png(48, 303),
            ),
        ),
    )

    private fun png(width: Int, height: Int): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", it)
    }.toByteArray()
}
