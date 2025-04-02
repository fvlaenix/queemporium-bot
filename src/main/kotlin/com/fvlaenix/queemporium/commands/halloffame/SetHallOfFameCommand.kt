package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SetHallOfFameCommand(
    databaseConfiguration: DatabaseConfiguration,
    private val answerService: AnswerService,
    coroutineProvider: BotCoroutineProvider
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
        if (parts.size != 4 || parts[3] != "this-channel") {
            answerService.sendReply(message, "Format: /shogun-sama set-hall-of-fame <threshold> this-channel")
            return
        }

        val threshold = parts[2].toIntOrNull()
        if (threshold == null || threshold <= 0) {
            answerService.sendReply(message, "Threshold must be a positive number!")
            return
        }

        hallOfFameConnector.setHallOfFameInfo(
            guildId = message.guildId!!,
            channelId = message.channelId,
            threshold = threshold
        )
        
        answerService.sendReply(message, "Hall of Fame configured successfully!")
    }
}