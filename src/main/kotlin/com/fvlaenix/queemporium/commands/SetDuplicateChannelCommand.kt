package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SetDuplicateChannelCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())

  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw == "/shogun-sama beg-this-channel-duplicate"

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) {
      answerService.sendReply(event.message, "This only applies to servers, stupid!")
      return
    }
    val message = event.message
    val channel = message.channel
    if (!message.isFromAdmin()) {
      answerService.sendReply(message, "Pathetic, only admins can use this!")
      return
    }
    guildInfoConnector.setDuplicateInfo(message.guildId!!, channel.id)
    answerService.sendReply(message, "My verdicts will now appear here!")
  }
}