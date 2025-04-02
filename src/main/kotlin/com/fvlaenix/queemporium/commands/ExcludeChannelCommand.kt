package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ExcludeChannelCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {
  companion object {
    const val COMMAND_ADD_TO_EXCLUDE = "/shogun-sama beg-remove-decree"
    const val COMMAND_REMOVE_FROM_EXCLUDE = "/shogun-sama beg-add-decree"
  }

  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())

  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw == COMMAND_REMOVE_FROM_EXCLUDE ||
        event.message.contentRaw == COMMAND_ADD_TO_EXCLUDE

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
    val isChannelExcluded = guildInfoConnector.isChannelExcluded(message.guildId!!, channel.id)
    if (message.contentRaw == COMMAND_ADD_TO_EXCLUDE) {
      if (!isChannelExcluded) {
        guildInfoConnector.addExcludingChannel(message.guildId!!, channel.id)
        answerService.sendReply(message, "Hmmm... All right, if you beg me...")
      } else {
        answerService.sendReply(message, "I don't watch this channel anyway, you pathetic fool")
      }
    } else {
      if (!isChannelExcluded) {
        answerService.sendReply(message, "I'm watching this channel as it is, you pathetic fool")
      } else {
        guildInfoConnector.removeExcludingChannel(message.guildId!!, channel.id)
        answerService.sendReply(message, "Be careful! I'm eyeing this channel now!")
      }
    }
  }
}