package br.com.weslleycampos.simpanel.screen

import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * [source] at the size it's drawn at, halved step by step so that a large reduction stays smooth,
 * and kept until it's drawn at another size. Drawn at its own size or larger, it's [source] itself,
 * which drawing enlarges smoothly enough.
 */
class ScaledImage(val source: BufferedImage) {
    private var scaled: BufferedImage? = null

    fun at(width: Int, height: Int): BufferedImage {
        if (width >= source.width && height >= source.height) return source
        scaled?.let { if (it.width == width && it.height == height) return it }
        var image = source
        while (image.width / 2 >= width && image.height / 2 >= height) {
            image = resized(image, image.width / 2, image.height / 2)
        }
        return resized(image, width.coerceAtLeast(1), height.coerceAtLeast(1)).also { scaled = it }
    }
}

private fun resized(image: BufferedImage, width: Int, height: Int): BufferedImage =
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE).also { resized ->
        val g = resized.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(image, 0, 0, width, height, null)
        } finally {
            g.dispose()
        }
    }
