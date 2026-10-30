package br.com.weslleycampos.simpanel.helper

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

/** Reuses one ImageIO reader and keeps it off the disk cache `ImageIO.read(InputStream)` uses. */
class JpegDecoder : AutoCloseable {
    private val reader = ImageIO.getImageReadersByFormatName("jpeg").next()

    fun decode(jpeg: ByteArray): BufferedImage =
        MemoryCacheImageInputStream(ByteArrayInputStream(jpeg)).use { input ->
            reader.setInput(input, true, true)
            reader.read(0)
        }

    override fun close() {
        reader.dispose()
    }
}
