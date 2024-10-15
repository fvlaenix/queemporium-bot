package com.fvlaenix.queemporium.utils

import com.fvlaenix.queemporium.commands.duplicate.DuplicateImageService
import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CorrectAuthorMappingData
import com.fvlaenix.queemporium.database.MessageDuplicateData
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension

private val LOG = Logger.getLogger(AnswerUtils::class.java.name)

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
  
  fun MessageChannel.sendMessageNow(
    text: String,
    imageWithFileNames: List<ImageUploadInfo> = emptyList(),
  ): Future<String?> {
    assert(text.length <= 2000)
    val fileUploads = imageWithFileNames.map(::toFileUpload)
    
    val future = CompletableFuture<String?>()
    val callback: (Throwable) -> Unit = {
      future.complete(null)
      LOG.log(Level.SEVERE, "Can't send message with ${text.take(200)}, length: ${text.length}, fileUploadsCount: ${fileUploads.size}", it)
    }
    
    if (fileUploads.isNotEmpty()) { sendFiles(fileUploads).addContent(text) } 
    else { sendMessage(text) }
      .queue({ future.complete(it.id) }, callback)
    return future
  }
  
  fun Message.sendReplyNow(
    text: String,
    imageWithFileNames: List<ImageUploadInfo> = emptyList(),
  ): Future<String?> {
    return channel.sendMessageNow(
      text = text, 
      imageWithFileNames = imageWithFileNames
    )
  }
  
  fun sendDuplicateMessageInfo(
    duplicateChannel: MessageChannel,
    messageAuthorId: String,
    fileName: String,
    image: BufferedImage,
    messageData: MessageDuplicateData.FullInfo,
    additionalImageInfo: AdditionalImageInfo,
    isSpoiler: Boolean,
    originalData: List<Pair<MessageDuplicateData.FullInfo, DuplicateImageService.DuplicateImageData>>
  ): List<Future<String?>> {
    val duplicateMessageDatas = mutableListOf<Future<String?>>()
    
    val prefix = """
        <@$messageAuthorId> made repost!

        Delete (or ask to delete if you don't have rights) that post which is weaker in any of the points (importance from the very first one):
         1) Contains link to the work and the author and name of character
         2) Contains link to the work and the author
         3) Contains link to the work
         4) If your message contains strictly more pictures (variations)
         5) Contains translation
         Don't delete messages with 10+ unique reactions
         
        Duplicate: ${messageData.url}
      """.trimIndent()
    
    val duplicateMessages = originalData.map { (duplicateInfoMessageData, duplicateInfoDuplicateData) ->
      var duplicateInfoText = "\n\nOriginal: ${duplicateInfoMessageData.url}\n"

      if (messageData.hasSource && !duplicateInfoMessageData.hasSource) duplicateInfoText += "= Looks like your have source\n"
      if (!messageData.hasSource && duplicateInfoMessageData.hasSource) duplicateInfoText += "= **Looks like original have source**\n"

      if (messageData.countImages < duplicateInfoMessageData.countImages) duplicateInfoText += "= **Looks like you have less images**\n"
      if (messageData.countImages > duplicateInfoMessageData.countImages) duplicateInfoText += "= Looks like you have more images\n"

      if (
        additionalImageInfo.originalSizeWidth > duplicateInfoDuplicateData.additionalImageInfo.originalSizeWidth &&
        additionalImageInfo.originalSizeHeight > duplicateInfoDuplicateData.additionalImageInfo.originalSizeHeight
      ) {
        duplicateInfoText += "= Looks like your image is bigger\n"
      } else {
        if (
          additionalImageInfo.originalSizeWidth < duplicateInfoDuplicateData.additionalImageInfo.originalSizeWidth &&
          additionalImageInfo.originalSizeHeight < duplicateInfoDuplicateData.additionalImageInfo.originalSizeHeight
        ) {
          duplicateInfoText += "= **Looks like your image is smaller**\n"
        } else {
          if (
            additionalImageInfo.originalSizeWidth != duplicateInfoDuplicateData.additionalImageInfo.originalSizeWidth ||
            additionalImageInfo.originalSizeHeight != duplicateInfoDuplicateData.additionalImageInfo.originalSizeHeight
          ) {
            duplicateInfoText += "= **ALERT ALERT ALERT: Images incompatible: " +
                    "${additionalImageInfo.originalSizeWidth}x${additionalImageInfo.originalSizeHeight} and " +
                    "${duplicateInfoDuplicateData.additionalImageInfo.originalSizeWidth}x${duplicateInfoDuplicateData.additionalImageInfo.originalSizeHeight}!**\n"
          }
        }
      }
      duplicateInfoText
    }
    var accumulator = prefix
    duplicateMessages.forEach { duplicateMessage ->
      if ((accumulator + duplicateMessage).length > 1900) {
        val duplicateMessageData = duplicateChannel.sendMessageNow(
          text = accumulator,
          imageWithFileNames = listOf(ImageUploadInfo(
            image = image,
            fileName = fileName,
            isSpoiler = isSpoiler
          )),
        )
        duplicateMessageDatas.add(duplicateMessageData)
        accumulator = prefix
      }
      accumulator += duplicateMessage
    }
    val duplicateMessageData = duplicateChannel.sendMessageNow(
      text = accumulator,
      imageWithFileNames = listOf(ImageUploadInfo(
        image = image,
        fileName = fileName,
        isSpoiler = isSpoiler
      )),
    )
    duplicateMessageDatas.add(duplicateMessageData)
    return duplicateMessageDatas
  }
  
  fun sendAuthorChangeRequest(
    duplicateChannel: MessageChannel,
    authorId: String,
    messageUrl: String,
    correct: CorrectAuthorMappingData
  ) {
    val message = """
        <@$authorId> made mistake in author name!

        Change name from ${correct.from} to ${correct.to}

        Message: $messageUrl
      """.trimIndent()
    duplicateChannel.sendMessageNow(message)
  }
  
  fun sendPixivCompressDetectRequest(
    duplicateChannel: MessageChannel,
    authorId: String,
    messageUrl: String
  ) {
    val message = """
        <@$authorId> (no tag while beta testing) made mistake in sending picture!

        Your picture was sent with Pixiv compression. Please, open it in another tab and copy properly
        
        Message: $messageUrl
      """.trimIndent()
    duplicateChannel.sendMessageNow(message)
  }
}