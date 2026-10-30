package br.com.weslleycampos.simpanel.screen

import br.com.weslleycampos.simpanel.protocol.DeviceButton
import java.awt.geom.Rectangle2D

/** A button on the device frame, in points from its top left, that holds [button] down while pressed. */
class FrameButton(
    val button: DeviceButton,
    val bounds: Rectangle2D.Double,
    /** Drawn over the body instead of under it. */
    val onTop: Boolean,
    val image: ScaledImage,
    val pressedImage: ScaledImage,
)
