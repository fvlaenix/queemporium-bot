package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.utils.AnswerUtils.sendReplyNow
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ExcludeChannelCommand(databaseConfiguration: DatabaseConfiguration): CoroutineListenerAdapter() {
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
      event.message.sendReplyNow("This only applies to servers, stupid!")
      return
    }
    val message = event.message
    val channel = message.channel
    if (!message.isFromAdmin()) {
      message.sendReplyNow("Pathetic, only admins can use this!")
    }
    val isChannelExcluded = guildInfoConnector.isChannelExclude(message.guildId!!, channel.id)
    if (message.contentRaw == COMMAND_ADD_TO_EXCLUDE) {
      if (!isChannelExcluded) {
        guildInfoConnector.addExcludedChannel(message.guildId!!, channel.id)
        message.sendReplyNow("Hmmm... All right, if you beg me...")
      } else {
        message.sendReplyNow("I don't watch this channel anyway, you pathetic fool")
      }
    } else {
      if (isChannelExcluded) {
        message.sendReplyNow("I'm watching this channel as it is, you pathetic fool")
      } else {
        guildInfoConnector.excludeExcludedChannel(message.guildId!!, channel.id)
        message.sendReplyNow("Be careful! I'm eyeing this channel now!")
      }
    }
  }
}