package com.fvlaenix.queemporium.utils

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.logging.Level
import java.util.logging.LogManager
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension

private val LOG = LogManager.getLogManager().getLogger(AnswerUtils::class.java.name)

object AnswerUtils {
  data class ImageUploadInfo(
    val image: BufferedImage,
    val fileName: String,
    val isSpoiler: Boolean
  )
  
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
  
  fun MessageChannelUnion.sendMessage(
    text: String,
    imageWithFileNames: List<ImageUploadInfo>,
  ) {
    assert(text.length <= 2000)
    val fileUploads = imageWithFileNames.map(::toFileUpload)
    sendFiles(fileUploads)
      .addContent(text)
      .queue({}, {
        LOG.log(Level.SEVERE, "Can't send message with ${text.take(200)}, length: ${text.length}, fileUploadsCount: ${fileUploads.size}", it)
      })
  }
  
  fun MessageChannelUnion.sendDuplicateMessageInfo(
    
  ) {
    TODO()
  }
}