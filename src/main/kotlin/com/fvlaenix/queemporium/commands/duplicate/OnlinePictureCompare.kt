package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.commands.MessagesStoreCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.service.DuplicateImageService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent

class OnlinePictureCompare(
  databaseConfiguration: DatabaseConfiguration,
  answerService: AnswerService,
  duplicateImageService: DuplicateImageService,
  coroutineProvider: BotCoroutineProvider,
  private val messagesStoreCommand: MessagesStoreCommand
) : ReportPictureCommand(databaseConfiguration, answerService, duplicateImageService, coroutineProvider) {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())

  @OptIn(DelicateCoroutinesApi::class)
  private val comparingContext = newFixedThreadPoolContext(10, "Online Compare Image")
  private var compressSize: CompressSize? = null

  private val flowCollectorScope = CoroutineScope(SupervisorJob() + coroutineProvider.botPool)
  private var receivedCollectorJob: Job? = null
  private var deletedCollectorJob: Job? = null

  override suspend fun onReadySuspend(event: ReadyEvent) {
    compressSize = duplicateImageService.checkServerAliveness(event)

    receivedCollectorJob = messagesStoreCommand.received
      .onEach { receivedEvent ->
        coroutineProvider.mainScope.launch(coroutineProvider.botPool) {
          handleMessageReceived(receivedEvent)
        }
      }
      .launchIn(flowCollectorScope)

    deletedCollectorJob = messagesStoreCommand.deleted
      .onEach { deleteEvent ->
        coroutineProvider.mainScope.launch(coroutineProvider.botPool) {
          handleMessageDelete(deleteEvent)
        }
      }
      .launchIn(flowCollectorScope)
  }

  private suspend fun handleMessageReceived(event: MessageReceivedEvent) {
    if (!event.isFromGuild) return
    val message = event.message
    if (
      guildInfoConnector.isChannelExcluded(message.guildId!!, message.channelId) ||
      guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) == message.channelId
    ) return
    coroutineScope {
      launch(comparingContext + EXCEPTION_HANDLER) {
        getMessage(compressSize!!, message)
      }
    }
  }

  private suspend fun handleMessageDelete(event: MessageDeleteEvent) {
    if (!event.isFromGuild) return
    val guildId = event.guild.id
    val channelId = event.channel.id
    val messageId = event.messageId

    if (
      guildInfoConnector.isChannelExcluded(guildId, channelId) ||
      guildInfoConnector.getDuplicateInfoChannel(guildId) == channelId
    ) return
    val messageDuplicateData = messageDuplicateDataConnector.get(messageId) ?: return

    duplicateImageService.deleteImage(
      (0 until messageDuplicateData.countImages).map { i ->
        DuplicateImageService.DeleteImageData(messageId, i)
      }
    )
    messageDuplicateDataConnector.delete(messageId)
  }
}
