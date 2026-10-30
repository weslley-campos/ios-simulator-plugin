package br.com.weslleycampos.simpanel.protocol

import kotlinx.serialization.Serializable

/** A button on the device the helper draws, with its images as PNGs. */
@Serializable
class ChromeButton(
    val button: DeviceButton,
    val frame: ChromeRect,
    /** Drawn over the body instead of under it. */
    val onTop: Boolean,
    @Serializable(with = Base64Serializer::class) val image: ByteArray,
    @Serializable(with = Base64Serializer::class) val pressedImage: ByteArray,
)
