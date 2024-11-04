package com.fvlaenix.queemporium.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension

private val LOG = Logger.getLogger(AnswerServiceImpl::class.java.name)

class AnswerServiceImpl : AnswerService() {
  private fun toFileUpload(byteArray: ByteArray, name: String): FileUpload {
    return FileUpload.fromData(byteArray, name)
  }

  private fun toFileUpload(bufferedImage: BufferedImage, name: String): FileUpload {
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, Path(name).extension, outputStream)
    return toFileUpload(outputStream.toByteArray(), name)
  }

  private fun toFileUpload(imageUploadInfo: ImageUploadInfo): FileUpload {
    return toFileUpload(imageUploadInfo.image, imageUploadInfo.fileName)
      .let {
        if (imageUploadInfo.isSpoiler) it.asSpoiler()
        else it
      }
  }

  override suspend fun sendMessage(
    destination: MessageChannel,
    text: String,
    imageWithFileNames: List<ImageUploadInfo>
  ): Deferred<String?> {
    check(text.length <= 2000)

    val fileUploads = imageWithFileNames.map(::toFileUpload)

    val deferred = CompletableDeferred<String?>()
    val callback: (Throwable) -> Unit = {
      deferred.complete(null)
      LOG.log(
        Level.SEVERE,
        "Can't send message with ${text.take(200)}, length: ${text.length}, fileUploadsCount: ${fileUploads.size}",
        it
      )
    }

    if (fileUploads.isNotEmpty()) {
      destination.sendFiles(fileUploads).addContent(text)
    } else {
      destination.sendMessage(text)
    }
      .queue({ deferred.complete(it.id) }, callback)
    return deferred
  }
}