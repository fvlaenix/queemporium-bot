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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
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
          state = HallOfFameState.NOT_SELECTED,
          hofMessageId = null,
          thresholdCrossDetectedAt = clock.millis()
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

    val message = hallOfFameConnector.getOldestToSendMessage(guildId)
    if (message == null) {
      if (debug) LOG.debug("No TO_SEND messages for guild $guildId")
      return
    }

    if (debug) LOG.debug("Attempting to send Hall of Fame message ${message.messageId} for guild $guildId")

    try {
      val guild = jda.getGuildById(guildId) ?: run {
        LOG.warn("Guild $guildId not found, skipping message ${message.messageId}")
        return
      }
      val dataMessage = messageDataConnector.get(message.messageId) ?: run {
        LOG.warn("Message data not found for ${message.messageId}, skipping")
        return
      }

      val sourceChannel = guild.getTextChannelById(dataMessage.channelId) ?: run {
        LOG.warn("Source channel ${dataMessage.channelId} not found, skipping message ${message.messageId}")
        return
      }
      val targetChannel = guild.getTextChannelById(hallOfFameInfo.channelId) ?: run {
        LOG.warn("Target channel ${hallOfFameInfo.channelId} not found, skipping message ${message.messageId}")
        return
      }
      val sourceMessage = sourceChannel.retrieveMessageById(message.messageId).complete() ?: run {
        LOG.warn("Source message ${message.messageId} not found, skipping")
        return
      }

      val messageUrl = sourceMessage.jumpUrl
      val reactionCount = sourceMessage.reactions.sumOf { it.count }

      val sentMessageId = answerService.sendMessage(
        destination = targetChannel,
        text = "Message reached $reactionCount reactions! 🎉\nOriginal message: $messageUrl"
      ).await()

      if (sentMessageId != null) {
        val forwardMessage = answerService.forwardMessage(
          message = sourceMessage,
          destination = targetChannel,
          failedCallback = {
            LOG.error("Failed to forward message: $it")
          }
        ).await()

        val dependentMessages = mutableListOf(sentMessageId)
        if (forwardMessage != null) {
          dependentMessages.add(forwardMessage)
        }

        hallOfFameConnector.markAsPosted(message.messageId, sentMessageId)

        dependentMessages.forEach { dependentId ->
          transaction(database) {
            MessageDependencyTable.insert {
              it[targetMessage] = message.messageId
              it[dependentMessage] = dependentId
            }
          }
        }

        LOG.info("Posted Hall of Fame message ${message.messageId} with $reactionCount reactions")
      } else {
        LOG.warn("Hall of Fame announcement for ${message.messageId} failed to send; skipping")
      }
    } catch (e: Exception) {
      LOG.error("Failed to process Hall of Fame message ${message.messageId}, skipping", e)
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
        coroutineProvider.safeDelay(6.hours)
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
        if (debug) LOG.debug("Message $messageId crossed threshold online, posting immediately")
        postMessageImmediately(messageId, guildId, hallOfFameInfo, emojiCount)
      } else if (existingEntry.state == HallOfFameState.POSTED) {
        if (debug) LOG.debug("Message $messageId already posted, debouncing update")
        updateDebouncer.emit(messageId, guildId, emojiCount)
      } else {
        if (debug) LOG.debug("Hall of Fame entry $messageId exists in state ${existingEntry.state}, not posted yet")
      }
    } else {
      if (debug) LOG.debug("Message $messageId below threshold ($emojiCount < ${hallOfFameInfo.threshold})")
    }
  }

  private suspend fun postMessageImmediately(
    messageId: String,
    guildId: String,
    hallOfFameInfo: HallOfFameInfo,
    reactionCount: Int
  ) {
    val currentJda = jda ?: run {
      LOG.warn("JDA not available, cannot post message immediately")
      return
    }

    try {
      val guild = currentJda.getGuildById(guildId) ?: run {
        LOG.warn("Guild $guildId not found")
        return
      }
      val dataMessage = messageDataConnector.get(messageId) ?: run {
        LOG.warn("Message data not found for $messageId")
        return
      }

      val sourceChannel = guild.getTextChannelById(dataMessage.channelId) ?: run {
        LOG.warn("Source channel ${dataMessage.channelId} not found")
        return
      }
      val targetChannel = guild.getTextChannelById(hallOfFameInfo.channelId) ?: run {
        LOG.warn("Target channel ${hallOfFameInfo.channelId} not found")
        return
      }
      val sourceMessage = sourceChannel.retrieveMessageById(messageId).complete() ?: run {
        LOG.warn("Source message $messageId not found")
        return
      }

      val messageUrl = sourceMessage.jumpUrl

      val sentMessageId = answerService.sendMessage(
        destination = targetChannel,
        text = "Message reached $reactionCount reactions! 🎉\nOriginal message: $messageUrl"
      ).await()

      if (sentMessageId != null) {
        val forwardMessage = answerService.forwardMessage(
          message = sourceMessage,
          destination = targetChannel,
          failedCallback = {
            LOG.error("Failed to forward message: $it")
          }
        ).await()

        val dependentMessages = mutableListOf(sentMessageId)
        if (forwardMessage != null) {
          dependentMessages.add(forwardMessage)
        }

        hallOfFameConnector.updateOrInsertMessage(
          HallOfFameMessage(
            messageId = messageId,
            guildId = guildId,
            timestamp = dataMessage.epoch,
            state = HallOfFameState.POSTED,
            hofMessageId = sentMessageId,
            thresholdCrossDetectedAt = clock.millis()
          )
        )

        dependentMessages.forEach { dependentId ->
          transaction(database) {
            MessageDependencyTable.insert {
              it[targetMessage] = messageId
              it[dependentMessage] = dependentId
            }
          }
        }

        LOG.info("Posted Hall of Fame message $messageId immediately (online crossing) with $reactionCount reactions")
      } else {
        LOG.warn("Failed to send Hall of Fame announcement for $messageId")
      }
    } catch (e: Exception) {
      LOG.error("Failed to post Hall of Fame message $messageId immediately", e)
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

    val hallOfFameMessage = hallOfFameConnector.getMessage(messageId) ?: return
    val announcementMessageId = hallOfFameMessage.hofMessageId ?: run {
      if (debug) LOG.debug("No hofMessageId found for $messageId, falling back to dependencies")
      val dependencies = dependencyConnector.getDependencies(messageId)
      dependencies.firstOrNull() ?: return
    }

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
