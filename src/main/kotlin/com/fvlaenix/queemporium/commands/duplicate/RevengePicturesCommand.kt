package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageId
import net.dv8tion.jda.api.events.session.ReadyEvent

class RevengePicturesCommand(databaseConfiguration: DatabaseConfiguration) : ReportPictureCommand(databaseConfiguration) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = DuplicateImageService.checkServerAliveness(event) ?: return
    
    runOverOld(event.jda, { message ->
      val messageId = MessageId(message.guildId, message.channelId, message.id)
      messageDataConnector.get(messageId) == null
    }) { message ->
      getMessage(compressSize, message)
    }
  }
}