package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDuplicateData
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.utils.CoroutineUtils
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.session.ReadyEvent
import kotlin.coroutines.coroutineContext

class UploadPicturesCommand(
  databaseConfiguration: DatabaseConfiguration,
  answerService: AnswerService
) : ReportPictureCommand(databaseConfiguration, answerService) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = DuplicateImageService.checkServerAliveness(event) ?: return
    runOverOld(event.jda, { true }) { message ->
      val messageId = message.id
      val messageData = MessageData(
        messageId = messageId,
        guildId = message.guildId,
        channelId = message.channelId,
        text = message.contentRaw,
        url = message.jumpUrl,
        authorId = message.author.id,
        epoch = message.timeCreated.toEpochSecond(),
      )
      val messageDuplicateData = MessageDuplicateData(
        messageId = messageId,
        hasSource = message.contentRaw.contains("http"),
        countImages = message.attachments.size + message.embeds.size,
        messageProblems = emptyList()
      )
      
      messageDataConnector.add(messageData)
      if (messageDuplicateDataConnector.exists(messageData.messageId)) return@runOverOld
      
      withContext(coroutineContext + CoroutineUtils.CurrentMessageMessageProblemHandler()) {
        assert(coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY] != null)
        DuplicateImageService.sendPictures(
          message = message,
          compressSize = compressSize,
          withHistoryReload = false
        ) {}
        val messageProblemsHandler = coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY]!!
        messageDuplicateDataConnector.add(messageDuplicateData.copy(messageProblems = messageProblemsHandler.messageProblems))
      }
    }
  }
}