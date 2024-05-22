package com.fvlaenix.queemporium.utils

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.ImageId
import com.fvlaenix.queemporium.utils.DownloadUtils.readImageFromAttachment
import com.fvlaenix.queemporium.utils.DownloadUtils.readImageFromUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Message
import java.awt.image.BufferedImage
import java.net.URL
import java.util.logging.LogManager
import kotlin.coroutines.coroutineContext

private val LOG = LogManager.getLogManager().getLogger(MessageUtils::class.java.name)

object MessageUtils {
  data class MessageImageInfo(
    val bufferedImage: BufferedImage,
    val imageId: ImageId,
    val additionalImageInfo: AdditionalImageInfo
  )
  
  fun CoroutineScope.addImagesFromMessage(message: Message, withHistoryReload: Boolean = true, compressSize: CompressSize): Channel<MessageImageInfo> {
    val channel = Channel<MessageImageInfo>()
    launch {
      var currentId = 0
      val serverId = message.guildId!!
      val channelId = message.channelId
      val messageId = message.id
      for (attachment in message.attachments) {
        if (attachment.isImage) {
          val id = currentId++
          launch attachment@ { 
            val (image, additionalInfo) = readImageFromAttachment(attachment, compressSize) ?: return@attachment
            channel.send(
              MessageImageInfo(
                bufferedImage = image,
                imageId = ImageId(serverId, channelId, messageId, id), 
                additionalImageInfo = additionalInfo
              )
            )
          }
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
            launch embed@ {
              val (image, size) = readImageFromUrl(url, compressSize) ?: return@embed
              channel.send(
                MessageImageInfo(
                  bufferedImage = image,
                  imageId = ImageId(serverId, channelId, messageId, id),
                  additionalImageInfo = AdditionalImageInfo(
                    fileName = URL(url).file,
                    isSpoiler = false, // Can't take is spoiler from embed
                    originalSizeHeight = size.height,
                    originalSizeWidth = size.width
                  )
                )
              )
            }
          }
        }
      }
    }
    return channel
  }
}