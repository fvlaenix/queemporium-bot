package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageId
import com.fvlaenix.queemporium.database.MessageProblems
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.time.Duration

class UploadPicturesCommand(databaseConfiguration: DatabaseConfiguration) : ReportPictureCommand(databaseConfiguration) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = DuplicateImageService.checkServerAliveness(event) ?: return
    runOverOld(event.jda, { message ->
      message.timeCreated.toEpochSecond() + Duration.ofDays(5).toSeconds() > System.currentTimeMillis() / 1000
    }) { message ->
      val messageData = MessageData(
        messageId = MessageId(
          guildId = message.guildId!!,
          channelId = message.channel.id,
          messageId = message.id
        ),
        text = message.contentRaw,
        hasSource = message.contentRaw.contains("http"),
        url = message.jumpUrl,
        author = message.author.id,
        epoch = message.timeCreated.toEpochSecond(),
        countImages = message.attachments.size + message.embeds.size,
        messageProblems = MessageProblems(emptyList())
      )
      messageDataConnector.add(messageData)

      DuplicateImageService.sendPictures(
        message = message,
        compressSize = compressSize,
        withHistoryReload = true
      ) {}
    }
  }
}