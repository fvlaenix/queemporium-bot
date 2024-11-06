package com.fvlaenix.queemporium.mock

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object TestImages {
    fun createTestImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.dispose()
        return image
    }

    fun loadTestImage(name: String): BufferedImage {
        return ImageIO.read(TestImages::class.java.getResourceAsStream("/test-images/$name"))
    }
}