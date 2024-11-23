package com.fvlaenix.queemporium.utils

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.MessageProblem
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import com.fvlaenix.queemporium.utils.DownloadUtils.readImageFromAttachment
import com.fvlaenix.queemporium.utils.DownloadUtils.readImageFromUrl
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.dv8tion.jda.api.entities.Message
import java.awt.image.BufferedImage
import java.net.URL

object MessageUtils {
  data class MessageImageInfo(
    val bufferedImage: BufferedImage,
    val guildId: String?,
    val channelId: String,
    val messageId: String,
    val numberInMessage: Int,
    val additionalImageInfo: AdditionalImageInfo
  )
  
  fun CoroutineScope.addImagesFromMessage(message: Message, withHistoryReload: Boolean = true, compressSize: CompressSize?): Channel<MessageImageInfo> {
    val channel = Channel<MessageImageInfo>(Channel.UNLIMITED)
    var currentId = 0
    val serverId = message.guildId!!
    val channelId = message.channelId
    val messageId = message.id
    val jobs = mutableListOf<Job>()
    launch(EXCEPTION_HANDLER) {
      for (attachment in message.attachments) {
        if (attachment.isImage) {
          val id = currentId++
          val job = launch attachment@ { 
            val (image, additionalInfo) = readImageFromAttachment(attachment, compressSize).let { 
              if (it == null) {
                kotlin.coroutines.coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY]?.messageProblems?.add(
                  MessageProblem.ImageProblem.InternalError(id, "Image can't be loaded")
                )
                return@attachment
              }
              it
            }
            channel.send(
              MessageImageInfo(
                bufferedImage = image,
                guildId = serverId,
                channelId = channelId,
                messageId = messageId,
                numberInMessage = id, 
                additionalImageInfo = additionalInfo
              )
            )
          }
          jobs.add(job)
        }
      }
      if (withHistoryReload) { delay(10000) }
      val reloadedMessage =
        if (withHistoryReload) message.channel.getHistoryAround(message.id, 1).complete().getMessageById(message.id)
        else message
      if (reloadedMessage != null) {
        for (embed in reloadedMessage.embeds) {
          val url = embed?.image?.url
          if (url != null) {
            val id = currentId
            currentId++
            val job = launch embed@ {
              val (image, size) = readImageFromUrl(url, compressSize).let {
                if (it == null) {
                  kotlin.coroutines.coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY]?.messageProblems?.add(
                    MessageProblem.ImageProblem.InternalError(id, "Image can't be loaded")
                  )
                  return@embed
                }
                it
              }
              channel.send(
                MessageImageInfo(
                  bufferedImage = image,
                  guildId = serverId,
                  channelId = channelId,
                  messageId = messageId,
                  numberInMessage = id,
                  additionalImageInfo = AdditionalImageInfo(
                    fileName = URL(url).file,
                    isSpoiler = false, // Can't take is spoiler from embed
                    originalSizeHeight = size.height,
                    originalSizeWidth = size.width
                  )
                )
              )
            }
            jobs.add(job)
          }
        }
      }
      launch(EXCEPTION_HANDLER) {
        jobs.joinAll()
        channel.close()
      }
    }
    
    return channel
  }
}