package br.com.weslleycampos.simpanel.screen

import br.com.weslleycampos.simpanel.protocol.DeviceButton
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

/** The iPhone 18 Pro Max as Xcode's phone12 chrome lays it out, drawn with clear one-pixel images. */
fun proMaxFrame(): DeviceFrame {
    val pixel = ScaledImage(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
    val power = FrameButton(
        button = DeviceButton.Power,
        bounds = Rectangle2D.Double(477.0, 293.0, 16.0, 101.0),
        onTop = false,
        image = pixel,
        pressedImage = pixel,
    )
    return DeviceFrame(
        width = 494.0,
        height = 992.0,
        screen = Rectangle2D.Double(27.0, 18.0, 440.0, 956.0),
        screenCornerRadius = 63.0,
        bezel = pixel,
        buttons = listOf(power),
    )
}
