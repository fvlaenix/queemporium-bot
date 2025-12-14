package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension

private val LOG = Logging.getLogger(AnswerServiceImpl::class.java)

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
      LOG.error(
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

  override suspend fun forwardMessage(
    message: Message,
    destination: MessageChannel,
    successCallback: (Message) -> Unit,
    failedCallback: (Throwable) -> Unit
  ): Deferred<String?> {
    val deferred = CompletableDeferred<String?>()
    message.forwardTo(destination).queue({ deferred.complete(it.id); successCallback(it) }, { deferred.complete(null); failedCallback(it) })
    return deferred
  }

  override suspend fun sendFile(
    destination: MessageChannel,
    filename: String,
    bytes: ByteArray
  ): Deferred<String?> {
    val fileUpload = toFileUpload(bytes, filename)
    val deferred = CompletableDeferred<String?>()
    destination.sendFiles(fileUpload)
      .queue(
        { message -> deferred.complete(message.id) },
        { error ->
          LOG.error("Error while sending file: $filename, size: ${bytes.size}", error)
          deferred.complete(null)
        }
      )
    return deferred
  }
}
