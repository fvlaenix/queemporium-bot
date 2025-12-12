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
  private val clock: java.time.Clock,
  private val debug: Boolean = false
) : CoroutineListenerAdapter(coroutineProvider) {
  private val database = databaseConfiguration.toDatabase()
  private val hallOfFameConnector = HallOfFameConnector(database)
  private val messageDataConnector = MessageDataConnector(database)
  private val dependencyConnector = MessageDependencyConnector(database)
  private val emojiDataConnector = EmojiDataConnector(database)
  private val messageEmojiDataConnector = MessageEmojiDataConnector(database)
  private val updateDebouncer = HallOfFameUpdateDebouncer(
    coroutineProvider = coroutineProvider,
    clock = clock,
    debug = debug
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

    if (debug) {
      LOG.debug("Guild ${hallOfFameInfo.guildId}: Found ${messages.size} messages above threshold ${hallOfFameInfo.threshold}")
    }

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
      if (inserted) {
        if (debug) LOG.debug("Guild ${hallOfFameInfo.guildId}: Enqueued new message $message")
      } else {
        if (debug) LOG.debug("Hall of Fame entry $message already exists for guild ${hallOfFameInfo.guildId}, skipping enqueue")
      }
    }
  }

  private suspend fun processGuild(jda: JDA, guildId: String) {
    if (debug) LOG.debug("Processing guild $guildId for Hall of Fame sending")
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId) ?: return

    // Get oldest unsent message
    val message = hallOfFameConnector.getOldestUnsentMessage(guildId)
    if (message == null) {
      if (debug) LOG.debug("No unsent messages for guild $guildId")
      return
    }

    if (debug) LOG.debug("Attempting to send Hall of Fame message ${message.messageId} for guild $guildId")

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

    if (debug) LOG.debug("Persisting announcement dependencies for $messageId: $dependentMessages")

    transaction(database) {
      val updated = HallOfFameMessagesTable.update({
        (HallOfFameMessagesTable.messageId eq messageId) and
            (HallOfFameMessagesTable.isSent eq false)
      }) {
        it[isSent] = true
      }

      if (updated == 0) {
        if (debug) LOG.debug("Hall of Fame entry $messageId already marked as sent, skipping dependency recording")
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
    if (debug) LOG.debug("Starting Hall of Fame background jobs")
    sendJob?.cancel()
    retrieveJob?.cancel()

    sendJob = coroutineProvider.mainScope.launch(CoroutineName("Hall of Fame Send Part")) {
      while (true) {
        if (debug) LOG.debug("Running Hall of Fame send cycle")
        jda.guilds.forEach { guild ->
          processGuild(jda, guild.id)
        }
        coroutineProvider.safeDelay(4.hours)
      }
    }
    retrieveJob = coroutineProvider.mainScope.launch(CoroutineName("Hall of Fame Retrieve Part")) {
      while (true) {
        if (debug) LOG.debug("Running Hall of Fame retrieve cycle")
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
      if (debug) LOG.debug("Hall of Fame not configured for guild $guildId")
      return
    }

    val emojiCount = messageEmojiDataConnector.get(messageId)?.count ?: return

    if (debug) LOG.debug("Rechecking message $messageId in $guildId. Count: $emojiCount, Threshold: ${hallOfFameInfo.threshold}")

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
          if (debug) LOG.debug("Hall of Fame entry $messageId already queued for guild $guildId, skipping")
        }
      } else if (existingEntry.isSent) {
        if (debug) LOG.debug("Message $messageId already sent, debouncing update")
        updateDebouncer.emit(messageId, guildId, emojiCount)
      } else {
        if (debug) LOG.debug("Hall of Fame entry $messageId already queued for guild $guildId, not sent yet")
      }
    } else {
      if (debug) LOG.debug("Message $messageId below threshold ($emojiCount < ${hallOfFameInfo.threshold})")
    }
  }

  suspend fun recheckGuild(guildId: String) {
    if (debug) LOG.debug("Rechecking guild $guildId")
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId) ?: return
    updateHallOfFameMessages(hallOfFameInfo)
  }

  private suspend fun updatePostedMessage(messageId: String, guildId: String, newCount: Int) {
    if (debug) LOG.debug("Updating posted Hall of Fame message for $messageId in $guildId to count $newCount")
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
