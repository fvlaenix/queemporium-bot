package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

private val LOG = Logging.getLogger(SetHallOfFameCommand::class.java)

class SetHallOfFameCommand(
    databaseConfiguration: DatabaseConfiguration,
    private val answerService: AnswerService,
    coroutineProvider: BotCoroutineProvider,
    private val lookbackDays: Long = 90
) : CoroutineListenerAdapter(coroutineProvider) {
    private val hallOfFameConnector = HallOfFameConnector(databaseConfiguration.toDatabase())

    override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
        event.message.contentRaw.startsWith("/shogun-sama set-hall-of-fame")

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

        val parts = message.contentRaw.split(" ")
        if (parts.size != 4) {
            answerService.sendReply(
                message,
                "Format: /shogun-sama set-hall-of-fame <threshold> (this-channel|<channelId>)"
            )
            return
        }

        val threshold = parts[2].toIntOrNull()
        if (threshold == null || threshold <= 0) {
            answerService.sendReply(message, "Threshold must be a positive number!")
            return
        }

        val channelId = when (val channelSpec = parts[3]) {
            "this-channel" -> message.channelId
            else -> {
                val guild = message.guild
                val channel = guild.getTextChannelById(channelSpec)
                if (channel == null) {
                    answerService.sendReply(message, "Channel $channelSpec not found!")
                    return
                }
                channel.id
            }
        }

        val guildId = message.guildId!!

        hallOfFameConnector.cancelBacklog(guildId)

        hallOfFameConnector.setHallOfFameInfo(
            guildId = guildId,
            channelId = channelId,
            threshold = threshold
        )

        val histogram = hallOfFameConnector.computeHistogram(guildId, threshold, lookbackDays)
        val histogramText = formatHistogram(histogram, lookbackDays)

        if (histogram.values.sum() == 0) {
            answerService.sendReply(
                message,
                "Hall of Fame configured successfully! No candidate messages found in the last $lookbackDays days."
            ).await()
            return
        }

        answerService.sendTextOrAttachment(
            destination = message.channel,
            text = "Hall of Fame configured successfully!\n\n$histogramText\n\nTo approve backlog posts, use:\n/shogun-sama hall-of-fame oldest <max-age>\n\nExamples: 7d, 1w, 48h\nOr skip backlog posting entirely.",
            filename = "hall-of-fame-histogram.txt"
        ).await()

        LOG.info("Hall of Fame configured for guild $guildId: threshold=$threshold, channel=$channelId")
    }

    private fun formatHistogram(histogram: Map<Long, Int>, lookbackDays: Long): String {
        return buildString {
            appendLine("Messages above threshold by age (cumulative):")
            appendLine("=".repeat(50))
            histogram.entries.sortedBy { it.key }.forEach { (days, count) ->
                val label = when (days) {
                    1L -> "≤ 1 day"
                    in 2..6 -> "≤ $days days"
                    7L -> "≤ 1 week"
                    14L -> "≤ 2 weeks"
                    30L -> "≤ 1 month"
                    60L -> "≤ 2 months"
                    90L -> "≤ 3 months"
                    else -> "≤ $days days"
                }
                appendLine(String.format("%-20s : %4d messages", label, count))
            }
            appendLine("=".repeat(50))
            appendLine("Lookback period: $lookbackDays days")
        }
    }
}