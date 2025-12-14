package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.HallOfFameConnector
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
  private val hallOfFameConnector = HallOfFameConnector(databaseConfiguration.toDatabase())

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
    if (parts.size != 5) {
      answerService.sendReply(message, "Format: /shogun-sama hall-of-fame oldest <max-age>\nExamples: 7d, 1w, 48h")
      return
    }

    val durationString = parts[4]
    val maxAgeDays = parseDuration(durationString)

    if (maxAgeDays == null) {
      answerService.sendReply(message, "Invalid duration format: $durationString\nSupported formats: 7d, 1w, 48h")
      return
    }

    val updated = hallOfFameConnector.markMessagesAsToSend(guildId, maxAgeDays, clock.millis())

    answerService.sendReply(
      message,
      "Backlog approved! Messages from the last ${formatDays(maxAgeDays)} will be posted gradually."
    ).await()

    LOG.info("Hall of Fame backlog approved for guild $guildId: maxAgeDays=$maxAgeDays, updated=$updated messages")
  }

  private fun parseDuration(durationString: String): Long? {
    val regex = Regex("(\\d+)([dwh])")
    val match = regex.matchEntire(durationString) ?: return null

    val value = match.groupValues[1].toLongOrNull() ?: return null
    val unit = match.groupValues[2]

    return when (unit) {
      "d" -> value
      "w" -> value * 7
      "h" -> value / 24
      else -> null
    }
  }

  private fun formatDays(days: Long): String {
    return when {
      days == 1L -> "1 day"
      days < 7 -> "$days days"
      days == 7L -> "1 week"
      days % 7 == 0L -> "${days / 7} weeks"
      else -> "$days days"
    }
  }
}
