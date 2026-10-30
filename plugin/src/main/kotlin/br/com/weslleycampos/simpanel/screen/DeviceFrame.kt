package br.com.weslleycampos.simpanel.screen

import br.com.weslleycampos.simpanel.protocol.HelperMessage
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

/** The device Xcode's Simulator draws around the screen, in points from its top left. */
class DeviceFrame(
    val width: Double,
    val height: Double,
    val screen: Rectangle2D.Double,
    /** How round the screen's corners are, which the frames are clipped to. */
    val screenCornerRadius: Double,
    /** The body, with the screen cut out of it. */
    val bezel: ScaledImage,
    val buttons: List<FrameButton>,
) {
    companion object {
        fun of(chrome: HelperMessage.Chrome) = DeviceFrame(
            width = chrome.width,
            height = chrome.height,
            screen = chrome.screen.toRectangle(),
            screenCornerRadius = chrome.screenCornerRadius,
            bezel = ScaledImage(png(chrome.bezel)),
            buttons = chrome.buttons.map { button ->
                FrameButton(
                    button = button.button,
                    bounds = button.frame.toRectangle(),
                    onTop = button.onTop,
                    image = ScaledImage(png(button.image)),
                    pressedImage = ScaledImage(png(button.pressedImage)),
                )
            },
        )
    }
}

/** Reads in memory; ImageIO closes the stream itself. */
private fun png(bytes: ByteArray): BufferedImage =
    ImageIO.read(MemoryCacheImageInputStream(ByteArrayInputStream(bytes)))
        ?: throw IOException("The helper sent an image ImageIO can't read")
