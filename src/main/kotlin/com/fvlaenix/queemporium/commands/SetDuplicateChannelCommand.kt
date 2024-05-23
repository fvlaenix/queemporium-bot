package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.utils.AnswerUtils.sendReplyNow
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SetDuplicateChannelCommand(databaseConfiguration: DatabaseConfiguration): CoroutineListenerAdapter() {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  
  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw == "/shogun-sama beg-this-channel-duplicate"

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
    guildInfoConnector.setDuplicateInfo(message.guildId!!, channel.id)
    message.sendReplyNow("My verdicts will now appear here!")
  }
}