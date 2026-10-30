package br.com.weslleycampos.simpanel.helper

import br.com.weslleycampos.simpanel.protocol.HelperMessage
import br.com.weslleycampos.simpanel.screen.DeviceFrame
import java.awt.image.BufferedImage

sealed interface HelperEvent {
    data class Attached(val hello: HelperMessage.Hello) : HelperEvent

    class FrameReady(val image: BufferedImage) : HelperEvent

    class DeviceFrameReady(val frame: DeviceFrame) : HelperEvent

    data class Failed(val failure: HelperMessage.Failure) : HelperEvent
}
