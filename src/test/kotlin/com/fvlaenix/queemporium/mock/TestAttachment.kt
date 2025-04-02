package com.fvlaenix.queemporium.mock

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.AttachmentProxy
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CompletableFuture

fun createTestAttachment(
  fileName: String,
  width: Int = 100,
  height: Int = 100,
  isSpoiler: Boolean = false,
  isImage: Boolean = true
): Message.Attachment {
  val attachment = mockk<Message.Attachment>()
  every { attachment.fileName } returns fileName
  every { attachment.isSpoiler } returns isSpoiler
  every { attachment.isImage } returns isImage
  every { attachment.width } returns width
  every { attachment.height } returns height
  every { attachment.url } returns "https://example.com/$fileName"

  val proxy = mockk<AttachmentProxy>()
  every { attachment.proxy } returns proxy

  val testImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  val download = CompletableFuture.completedFuture<InputStream>(
    ByteArrayInputStream(ImageUtils.bufferedImageToByteArray(testImage)))

  every { proxy.download() } returns download
  every { proxy.download(any(), any()) } returns download
  every { proxy.withClient(any()) } returns proxy

  return attachment
}