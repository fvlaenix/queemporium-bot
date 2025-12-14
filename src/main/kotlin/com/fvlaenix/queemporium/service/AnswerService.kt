package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CorrectAuthorMappingData
import com.fvlaenix.queemporium.database.MessageDuplicateData
import com.fvlaenix.queemporium.service.DuplicateImageService.DuplicateImageData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.image.BufferedImage
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO

private val LOG: Logger = Logger.getLogger(AnswerService::class.java.name)

abstract class AnswerService {
  data class ImageUploadInfo(
    val image: BufferedImage,
    val fileName: String,
    val isSpoiler: Boolean
  )

  abstract suspend fun sendMessage(
    destination: MessageChannel,
    text: String,
    imageWithFileNames: List<ImageUploadInfo> = emptyList()
  ): Deferred<String?>

  suspend fun sendReply(
    destination: Message,
    text: String,
    imageWithFileNames: List<ImageUploadInfo> = emptyList()
  ): Deferred<String?> {
    return sendMessage(destination.channel, text, imageWithFileNames)
  }

  abstract suspend fun forwardMessage(
    message: Message,
    destination: MessageChannel,
    successCallback: (Message) -> Unit = {},
    failedCallback: (Throwable) -> Unit = {}
  ): Deferred<String?>

  suspend fun sendDuplicateMessageInfo(
    duplicateChannel: MessageChannel,
    messageAuthorId: String,
    fileName: String,
    image: BufferedImage,
    messageData: MessageDuplicateData.FullInfo,
    additionalImageInfo: AdditionalImageInfo,
    isSpoiler: Boolean,
    originalData: List<Pair<MessageDuplicateData.FullInfo, DuplicateImageData>>
  ): List<Deferred<String?>> {
    val duplicateMessageDatas = mutableListOf<Deferred<String?>>()

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
        val duplicateMessageData = sendMessage(
          destination = duplicateChannel,
          text = accumulator,
          imageWithFileNames = listOf(
            ImageUploadInfo(
              image = image,
              fileName = fileName,
              isSpoiler = isSpoiler
            )
          ),
        )
        duplicateMessageDatas.add(duplicateMessageData)
        accumulator = prefix
      }
      accumulator += duplicateMessage
    }
    val duplicateMessageData = sendMessage(
      destination = duplicateChannel,
      text = accumulator,
      imageWithFileNames = listOf(
        ImageUploadInfo(
          image = image,
          fileName = fileName,
          isSpoiler = isSpoiler
        )
      ),
    )
    duplicateMessageDatas.add(duplicateMessageData)
    return duplicateMessageDatas
  }

  suspend fun sendAuthorChangeRequest(
    duplicateChannel: MessageChannel,
    authorId: String,
    messageUrl: String,
    correct: CorrectAuthorMappingData
  ): Deferred<String?> {
    val message = """
        <@$authorId> made mistake in author name!

        Change name from ${correct.from} to ${correct.to}

        Message: $messageUrl
      """.trimIndent()
    return sendMessage(duplicateChannel, message)
  }

  suspend fun sendPixivCompressDetectRequest(
    duplicateChannel: MessageChannel,
    authorId: String,
    messageUrl: String
  ): Deferred<String?> {
    val message = """
        <@$authorId> (no tag while beta testing) made mistake in sending picture!

        Your picture was sent with Pixiv compression. Please, open it in another tab and copy properly

        Message: $messageUrl
      """.trimIndent()
    val image = withContext(Dispatchers.IO) {
      ImageIO.read(AnswerService::class.java.getResourceAsStream("/images/what-a-pixel.jpg"))
    }
    return sendMessage(
      destination = duplicateChannel,
      text = message,
      imageWithFileNames = listOf(ImageUploadInfo(image, "what-a-pixel.jpg", false))
    )
  }

  suspend fun sendTextOrAttachment(
    destination: MessageChannel,
    text: String,
    filename: String = "details.txt"
  ): Deferred<String?> {
    return if (text.length <= 2000) {
      sendMessage(destination, text)
    } else {
      val deferred = CompletableDeferred<String?>()
      withContext(Dispatchers.IO) {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val fileUpload = FileUpload.fromData(textBytes, filename)
        destination.sendFiles(fileUpload)
          .queue(
            { message -> deferred.complete(message.id) },
            { error ->
              LOG.log(Level.SEVERE, "Error while sending message: $error")
              deferred.complete(null)
            }
          )
      }
      deferred
    }
  }

  abstract suspend fun sendFile(
    destination: MessageChannel,
    filename: String,
    bytes: ByteArray
  ): Deferred<String?>
}