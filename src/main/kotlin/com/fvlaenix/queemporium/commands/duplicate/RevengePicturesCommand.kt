package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.commands.MessagesStoreCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import net.dv8tion.jda.api.events.session.ReadyEvent

private val LOG = Logging.getLogger(RevengePicturesCommand::class.java)

class RevengePicturesCommand(
  databaseConfiguration: DatabaseConfiguration,
  answerService: AnswerService,
  duplicateImageService: DuplicateImageService,
  coroutineProvider: BotCoroutineProvider,
  private val messagesStoreCommand: MessagesStoreCommand
) : ReportPictureCommand(databaseConfiguration, answerService, duplicateImageService, coroutineProvider) {
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())

  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = duplicateImageService.checkServerAliveness(event) ?: return

    // Register as a consumer BEFORE waiting for scan to complete
    // This prevents the cache from being cleared when MessagesStoreCommand unregisters
    val messageStoreService = messagesStoreCommand.messageStoreService.await()
    messageStoreService.registerConsumer()

    // Wait for MessagesStoreCommand to finish persisting all messages before we start scanning
    LOG.info("Waiting for MessagesStoreCommand to complete initial scan")
    messagesStoreCommand.initialScanComplete.await()
    LOG.info("MessagesStoreCommand initial scan complete, starting revenge pictures scan")

    LOG.info("Starting revenge pictures scan using MessageStoreService")

    var totalGuilds = 0
    var totalChannels = 0
    var totalMessages = 0

    messageStoreService.guilds().collect { guild ->
      totalGuilds++
      LOG.info("RevengePictures: Processing guild $totalGuilds (${guild.id})")

      guild.channels().collect { channel ->
        totalChannels++
        LOG.info("RevengePictures: Processing channel $totalChannels (${channel.id})")

        // Collect messages that haven't been processed yet
        // Messages come from Discord newest-first, but we need to process oldest-first
        // so that original messages are processed before their duplicates
        val messagesToProcess = channel.messages()
          .takeWhile { message -> !messageDuplicateDataConnector.exists(message.id) }
          .toList()
          .asReversed() // Process oldest-first

        LOG.info("RevengePictures: Processing ${messagesToProcess.size} messages for channel ${channel.id} (oldest-first)")

        messagesToProcess.forEach { message ->
          getMessage(compressSize, message)
          totalMessages++
        }
      }
    }

    messageStoreService.unregisterConsumer()
    LOG.info("Completed revenge pictures scan - processed $totalMessages messages across $totalChannels channels in $totalGuilds guilds")
  }
}