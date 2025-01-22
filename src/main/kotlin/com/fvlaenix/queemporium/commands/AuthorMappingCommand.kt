package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.CorrectAuthorMappingConnector
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class AuthorMappingCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService
) : CoroutineListenerAdapter() {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  private val authorMappingConnector = CorrectAuthorMappingConnector(databaseConfiguration.toDatabase())

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (event.author.isBot || !event.isFromGuild) return

    val message = event.message
    val content = message.contentRaw

    if (message.attachments.isEmpty()) return

    val mapping = authorMappingConnector.findMapping(content)
    if (mapping == null) return

    val duplicateChannel = guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) ?: return
    val channel = message.guild.getTextChannelById(duplicateChannel) ?: return
    answerService.sendAuthorChangeRequest(channel, message.author.id, message.jumpUrl, mapping)
  }
}