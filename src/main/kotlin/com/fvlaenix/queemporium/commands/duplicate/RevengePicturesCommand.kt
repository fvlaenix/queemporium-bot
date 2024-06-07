package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.database.MessageId
import net.dv8tion.jda.api.events.session.ReadyEvent

class RevengePicturesCommand(databaseConfiguration: DatabaseConfiguration) : ReportPictureCommand(databaseConfiguration) {
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = DuplicateImageService.checkServerAliveness(event) ?: return
    
    runOverOld(event.jda, { message ->
      val messageId = MessageId(message.guildId, message.channelId, message.id)
      !messageDuplicateDataConnector.exists(messageId)
    }) { message ->
      getMessage(compressSize, message)
    }
  }
}