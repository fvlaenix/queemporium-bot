package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import java.util.logging.Level
import java.util.logging.Logger

private val LOG: Logger = Logger.getLogger(DependentDeleterCommand::class.java.name)

class DependentDeleterCommand(
  databaseConfiguration: DatabaseConfiguration,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {
  private val messageDependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())

  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    if (!event.isFromGuild) return

    val messageId = event.messageId

    val dependencyMessages = messageDependencyConnector.getDependencies(messageId)
    dependencyMessages.forEach { dependencyMessage ->
      try {
        val messageData = messageDataConnector.get(dependencyMessage) ?: return@forEach
        val jda: JDA = event.jda
        val channel: MessageChannel = if (messageData.guildId != null) {
          val guild: Guild = jda.getGuildById(messageData.guildId) ?: return
          guild.getTextChannelById(messageData.channelId) ?: return
        } else {
          jda.getPrivateChannelById(messageData.channelId) ?: return
        }
        val message: Message = channel.retrieveMessageById(messageData.messageId).complete() ?: return
        message.delete().queue {
          messageDependencyConnector.removeMessage(dependencyMessage)
        }
      } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Failed to delete dependent message: $dependencyMessage", e)
      }
    }
  }
}