package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

private val LOG = Logging.getLogger(HallOfFameOldestCommand::class.java)

class HallOfFameOldestCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService,
  coroutineProvider: BotCoroutineProvider,
  private val clock: java.time.Clock
) : CoroutineListenerAdapter(coroutineProvider) {
  private val database = databaseConfiguration.toDatabase()
  private val hallOfFameConnector = HallOfFameConnector(database)
  private val emojiDataConnector = EmojiDataConnector(database)
  private val messageDataConnector = MessageDataConnector(database)

  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw.startsWith("/shogun-sama hall-of-fame oldest")

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) {
      answerService.sendReply(event.message, "This only applies to servers, stupid!")
      return
    }

    val message = event.message
    if (!message.isFromAdmin() && !message.isFromRoleAdmin()) {
      answerService.sendReply(message, "Pathetic, only admins can use this!")
      return
    }

    val guildId = message.guildId!!

    val hofInfo = hallOfFameConnector.getHallOfFameInfo(guildId)
    if (hofInfo == null) {
      answerService.sendReply(
        message,
        "Hall of Fame is not configured for this server. Use /shogun-sama set-hall-of-fame first!"
      )
      return
    }

    val parts = message.contentRaw.split(" ")
    if (parts.size != 4) {
      answerService.sendReply(message, "Format: /shogun-sama hall-of-fame oldest <max-age>\nExamples: 7d, 1w, 48h")
      return
    }

    val durationString = parts[3]
    val maxAgeMillis = parseDurationToMillis(durationString)

    if (maxAgeMillis == null || maxAgeMillis <= 0) {
      answerService.sendReply(message, "Invalid duration format: $durationString\nSupported formats: 7d, 1w, 48h")
      return
    }

    enqueueBacklogCandidates(guildId, hofInfo.threshold)
    val updated = hallOfFameConnector.markMessagesAsToSend(guildId, maxAgeMillis, clock.millis())

    answerService.sendReply(
      message,
      "Backlog approved! Messages from the last ${formatDuration(maxAgeMillis)} will be posted gradually."
    ).await()

    LOG.info("Hall of Fame backlog approved for guild $guildId: maxAgeMillis=$maxAgeMillis, updated=$updated messages")
  }

  private fun enqueueBacklogCandidates(guildId: String, threshold: Int) {
    val nowMillis = clock.millis()
    val messageIds = emojiDataConnector.getMessagesAboveThreshold(guildId, threshold)
    messageIds.forEach { messageId ->
      val messageData = messageDataConnector.get(messageId) ?: return@forEach
      hallOfFameConnector.addMessage(
        HallOfFameMessage(
          messageId = messageId,
          guildId = guildId,
          timestamp = messageData.epoch,
          state = HallOfFameState.NOT_SELECTED,
          hofMessageId = null,
          thresholdCrossDetectedAt = nowMillis
        )
      )
    }
  }

  private fun parseDurationToMillis(durationString: String): Long? {
    val regex = Regex("(\\d+)([dwh])")
    val match = regex.matchEntire(durationString) ?: return null

    val value = match.groupValues[1].toLongOrNull() ?: return null
    val unit = match.groupValues[2]

    return when (unit) {
      "d" -> value * 24 * 60 * 60 * 1000
      "w" -> value * 7 * 24 * 60 * 60 * 1000
      "h" -> value * 60 * 60 * 1000
      else -> null
    }
  }

  private fun formatDuration(durationMillis: Long): String {
    val oneHour = 60L * 60L * 1000L
    val oneDay = 24L * oneHour
    val oneWeek = 7L * oneDay
    val hours = durationMillis / oneHour
    val days = durationMillis / oneDay

    return when {
      durationMillis % oneWeek == 0L -> {
        val weeks = durationMillis / oneWeek
        if (weeks == 1L) "1 week" else "$weeks weeks"
      }

      durationMillis % oneDay == 0L -> {
        if (days == 1L) "1 day" else "$days days"
      }

      hours == 1L -> "1 hour"
      else -> "$hours hours"
    }
  }
}
