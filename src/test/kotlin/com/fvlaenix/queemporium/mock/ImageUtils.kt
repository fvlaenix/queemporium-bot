package com.fvlaenix.queemporium.mock

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageUtils {
  fun bufferedImageToByteArray(image: BufferedImage, format: String = "png"): ByteArray {
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, format, outputStream)
    return outputStream.toByteArray()
  }

  fun createTestImage(width: Int, height: Int): BufferedImage {
    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  }
}