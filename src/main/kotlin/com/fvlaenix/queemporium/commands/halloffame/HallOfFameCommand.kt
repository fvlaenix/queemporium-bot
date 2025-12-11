package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration.Companion.hours

private val LOG = Logging.getLogger(HallOfFameCommand::class.java)

class HallOfFameCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService,
  coroutineProvider: BotCoroutineProvider,
  private val clock: java.time.Clock
) : CoroutineListenerAdapter(coroutineProvider) {
  private val database = databaseConfiguration.toDatabase()
  private val hallOfFameConnector = HallOfFameConnector(database)
  private val messageDataConnector = MessageDataConnector(database)
  private val dependencyConnector = MessageDependencyConnector(database)
  private val emojiDataConnector = EmojiDataConnector(database)
  private val messageEmojiDataConnector = MessageEmojiDataConnector(database)
  private val updateDebouncer = HallOfFameUpdateDebouncer(
    coroutineProvider = coroutineProvider,
    clock = clock
  ) { messageId, guildId, newCount ->
    updatePostedMessage(messageId, guildId, newCount)
  }

  private var sendJob: Job? = null
  private var retrieveJob: Job? = null
  private var jda: JDA? = null

  private fun updateHallOfFameMessages(hallOfFameInfo: HallOfFameInfo) {
    LOG.info("Updating Hall of Fame messages for guild ${hallOfFameInfo.guildId}")
    val messages = emojiDataConnector.getMessagesAboveThreshold(
      guildId = hallOfFameInfo.guildId,
      threshold = hallOfFameInfo.threshold,
    )

    messages.forEach { message ->
      val dataMessage = messageDataConnector.get(message) ?: return@forEach

      val inserted = hallOfFameConnector.addMessage(
        HallOfFameMessage(
          messageId = message,
          guildId = hallOfFameInfo.guildId,
          timestamp = dataMessage.epoch,
          isSent = false
        )
      )
      if (!inserted) {
        LOG.debug("Hall of Fame entry $message already exists for guild ${hallOfFameInfo.guildId}, skipping enqueue")
      }
    }
  }

  private suspend fun processGuild(jda: JDA, guildId: String) {
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId) ?: return

    // Get oldest unsent message
    val message = hallOfFameConnector.getOldestUnsentMessage(guildId) ?: return

    try {
      val guild = jda.getGuildById(guildId) ?: return
      val dataMessage = messageDataConnector.get(message.messageId) ?: return

      val sourceChannel = guild.getTextChannelById(dataMessage.channelId) ?: return
      val targetChannel = guild.getTextChannelById(hallOfFameInfo.channelId) ?: return
      val sourceMessage = sourceChannel.retrieveMessageById(message.messageId).complete() ?: return

      val messageUrl = sourceMessage.jumpUrl
      val reactionCount = sourceMessage.reactions.sumOf { it.count }

      val sentMessage = answerService.sendMessage(
        destination = targetChannel,
        text = "Message reached $reactionCount reactions! 🎉\nOriginal message: $messageUrl"
      ).await()
      val forwardMessage = answerService.forwardMessage(
        message = sourceMessage,
        destination = targetChannel,
        failedCallback = {
          LOG.error("Failed to forward message: $it")
        }
      ).await()

      val dependentMessages = mutableListOf<String>()
      if (sentMessage != null) {
        dependentMessages.add(sentMessage)
      }
      if (forwardMessage != null) {
        dependentMessages.add(forwardMessage)
      }

      if (sentMessage != null) {
        persistAnnouncement(message.messageId, dependentMessages)
      } else {
        LOG.warn("Hall of Fame announcement for ${message.messageId} failed to send; will retry later")
      }
    } catch (e: Exception) {
      LOG.error("Failed to process Hall of Fame message ${message.messageId}", e)
    }
  }

  private fun persistAnnouncement(messageId: String, dependentMessages: List<String>) {
    if (dependentMessages.isEmpty()) return

    transaction(database) {
      val updated = HallOfFameMessagesTable.update({
        (HallOfFameMessagesTable.messageId eq messageId) and
            (HallOfFameMessagesTable.isSent eq false)
      }) {
        it[isSent] = true
      }

      if (updated == 0) {
        LOG.debug("Hall of Fame entry $messageId already marked as sent, skipping dependency recording")
        return@transaction
      }

      dependentMessages.forEach { dependentId ->
        MessageDependencyTable.insert {
          it[targetMessage] = messageId
          it[dependentMessage] = dependentId
        }
      }
    }
  }

  private fun runHallOfFame(jda: JDA) {
    sendJob?.cancel()
    retrieveJob?.cancel()

    sendJob = coroutineProvider.mainScope.launch(CoroutineName("Hall of Fame Send Part")) {
      while (true) {
        jda.guilds.forEach { guild ->
          processGuild(jda, guild.id)
        }
        coroutineProvider.safeDelay(4.hours)
      }
    }
    retrieveJob = coroutineProvider.mainScope.launch(CoroutineName("Hall of Fame Retrieve Part")) {
      while (true) {
        hallOfFameConnector.getAll().forEach { info ->
          updateHallOfFameMessages(info)
        }
        coroutineProvider.safeDelay(9.hours)
      }
    }
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    jda = event.jda
    runHallOfFame(event.jda)
  }

  suspend fun recheckMessage(messageId: String, guildId: String) {
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId)
    if (hallOfFameInfo == null) {
      LOG.debug("Hall of Fame not configured for guild $guildId")
      return
    }

    val emojiCount = messageEmojiDataConnector.get(messageId)?.count ?: return

    if (emojiCount >= hallOfFameInfo.threshold) {
      val existingEntry = hallOfFameConnector.getMessage(messageId)

      if (existingEntry == null) {
        val dataMessage = messageDataConnector.get(messageId) ?: return
        val inserted = hallOfFameConnector.addMessage(
          HallOfFameMessage(
            messageId = messageId,
            guildId = guildId,
            timestamp = dataMessage.epoch,
            isSent = false
          )
        )
        if (inserted) {
          LOG.info("Message $messageId added to Hall of Fame queue ($emojiCount reactions)")
        } else {
          LOG.debug("Hall of Fame entry $messageId already queued for guild $guildId, skipping")
        }
      } else if (existingEntry.isSent) {
        updateDebouncer.emit(messageId, guildId, emojiCount)
      } else {
        LOG.debug("Hall of Fame entry $messageId already queued for guild $guildId, not sent yet")
      }
    }
  }

  suspend fun recheckGuild(guildId: String) {
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId) ?: return
    updateHallOfFameMessages(hallOfFameInfo)
  }

  private suspend fun updatePostedMessage(messageId: String, guildId: String, newCount: Int) {
    val currentJda = jda ?: return
    val dependencies = dependencyConnector.getDependencies(messageId)
    val announcementMessageId = dependencies.firstOrNull() ?: return

    val dataMessage = messageDataConnector.get(messageId) ?: return
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId) ?: return

    try {
      val guild = currentJda.getGuildById(guildId) ?: return
      val targetChannel = guild.getTextChannelById(hallOfFameInfo.channelId) ?: return

      val announcementMessage = targetChannel.retrieveMessageById(announcementMessageId).complete()
      val messageUrl = dataMessage.url
      val newText = "Message reached $newCount reactions! 🎉\nOriginal message: $messageUrl"
      announcementMessage.editMessage(newText).queue()

      LOG.info("Updated Hall of Fame announcement for message $messageId: $newCount reactions")
    } catch (e: Exception) {
      LOG.error("Failed to update Hall of Fame announcement for $messageId", e)
    }
  }
}
