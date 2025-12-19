package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.service.MessageStoreService
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent

private val LOG = Logging.getLogger(MessagesStoreCommand::class.java)

class MessagesStoreCommand(
  val databaseConfiguration: DatabaseConfiguration,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())

  private val _messageStoreService = CompletableDeferred<MessageStoreService>()
  val messageStoreService: Deferred<MessageStoreService> = _messageStoreService

  private val _initialScanComplete = CompletableDeferred<Unit>()
  val initialScanComplete: Deferred<Unit> = _initialScanComplete

  val received = MutableSharedFlow<MessageReceivedEvent>(
    replay = 128,
    extraBufferCapacity = 0,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  val deleted = MutableSharedFlow<MessageDeleteEvent>(
    replay = 128,
    extraBufferCapacity = 0,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  private fun computeMessage(message: Message) {
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

    messageDataConnector.add(messageData)
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    if (!_messageStoreService.isCompleted) {
      val service = MessageStoreService(event.jda, coroutineProvider)
      service.initialize()
      _messageStoreService.complete(service)
      LOG.info("StoreCommand: MessageStoreService initialized")
    }

    val service = messageStoreService.await()
    service.registerConsumer()

    LOG.info("StoreCommand: Starting message store scan")
    var totalMessages = 0
    var totalChannels = 0
    var totalGuilds = 0

    service.guilds().collect { guild ->
      totalGuilds++
      LOG.info("StoreCommand: Processing guild ${totalGuilds}")

      guild.channels().collect { channel ->
        totalChannels++
        LOG.info("StoreCommand: Processing channel ${totalChannels}")

        channel.messages().takeWhile { message ->
          val exists = messageDataConnector.get(message.id) != null
          if (exists) {
            LOG.debug("StoreCommand: Skipping message ${message.id} (already exists)")
          }
          !exists
        }.collect { message ->
          LOG.debug("StoreCommand: Processing message ${message.id}")
          computeMessage(message)
          totalMessages++
          if (totalMessages % 100 == 0) {
            LOG.info("StoreCommand: Processed $totalMessages messages")
          }
        }
      }
    }

    service.unregisterConsumer()
    LOG.info("StoreCommand: Finish onReady - processed $totalMessages messages across $totalChannels channels in $totalGuilds guilds")

    if (!_initialScanComplete.isCompleted) {
      _initialScanComplete.complete(Unit)
      LOG.info("StoreCommand: Initial scan complete signal sent")
    }
  }

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    runCatching {
      computeMessage(event.message)
      coroutineProvider.mainScope.launch(coroutineProvider.botPool) {
        received.emit(event)
      }
    }.onFailure { e ->
      LOG.error("Persist failed for ${event.messageId}", e)
    }
  }

  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    runCatching {
      messageDataConnector.delete(event.messageId)
    }.onFailure { e ->
      LOG.error("Delete failed for ${event.messageId}", e)
    }
    coroutineProvider.mainScope.launch(coroutineProvider.botPool) {
      deleted.emit(event)
    }
  }
}