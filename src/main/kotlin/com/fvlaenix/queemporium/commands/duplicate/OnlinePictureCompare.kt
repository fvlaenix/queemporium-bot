package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.duplicate.protobuf.deleteImageRequest
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent

class OnlinePictureCompare(databaseConfiguration: DatabaseConfiguration) : ReportPictureCommand(databaseConfiguration) {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())
  
  @OptIn(DelicateCoroutinesApi::class)
  private val comparingContext = newFixedThreadPoolContext(10, "Online Compare Image")
  private var compressSize: CompressSize? = null
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    compressSize = DuplicateImageService.checkServerAliveness(event)
  }

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) return
    val message = event.message
    if (
      guildInfoConnector.isChannelExclude(message.guildId!!, message.channelId) || 
      guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) == message.channelId
    ) return
    coroutineScope { 
      launch(comparingContext + EXCEPTION_HANDLER) {
        getMessage(compressSize!!, message)
      }
    }
  }

  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    if (!event.isFromGuild) return
    val guildId = event.guild.id
    val channelId = event.channel.id
    val messageId = event.messageId
    
    if (
      guildInfoConnector.isChannelExclude(guildId, channelId) ||
      guildInfoConnector.getDuplicateInfoChannel(guildId) == channelId
    ) return
    val messageDuplicateData = messageDuplicateDataConnector.get(messageId) ?: return
    DuplicateImageService.withOpenedChannel { service ->
      (0 until messageDuplicateData.countImages).forEach { numberImage ->
        service.deleteImage(deleteImageRequest { this.messageId = messageId; this.numberInMessage = numberImage })
      }
    }
    messageDataConnector.delete(messageId)
    messageDuplicateDataConnector.delete(messageId)
  }
}