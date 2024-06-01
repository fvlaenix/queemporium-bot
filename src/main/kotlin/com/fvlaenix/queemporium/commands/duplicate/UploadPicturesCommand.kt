package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageId
import com.fvlaenix.queemporium.utils.CoroutineUtils
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.time.Duration
import kotlin.coroutines.coroutineContext

class UploadPicturesCommand(databaseConfiguration: DatabaseConfiguration) : ReportPictureCommand(databaseConfiguration) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = DuplicateImageService.checkServerAliveness(event) ?: return
    runOverOld(event.jda, { true }) { message ->
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
        messageProblems = emptyList()
      )

      withContext(coroutineContext + CoroutineUtils.CurrentMessageMessageProblemHandler()) {
        assert(coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY] != null)
        DuplicateImageService.sendPictures(
          message = message,
          compressSize = compressSize,
          withHistoryReload = false
        ) {}
        val messageProblemsHandler = coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY]!!
        messageDataConnector.add(messageData.copy(messageProblems = messageProblemsHandler.messageProblems))
      }
    }
  }
}