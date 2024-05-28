package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import com.fvlaenix.queemporium.database.MessageId
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import java.util.logging.Level
import java.util.logging.Logger

private val LOG: Logger = Logger.getLogger(DependentDeleterCommand::class.java.name)

class DependentDeleterCommand(databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
  private val messageDependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  
  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    if (!event.isFromGuild) return
    
    val guildId = event.guild.id
    val channelId = event.channel.id
    val messageId = event.messageId
    
    val dependencyMessages = messageDependencyConnector.getDependencies(MessageId(guildId, channelId, messageId))
    dependencyMessages.forEach { dependencyMessage ->
      try {
        val jda: JDA = event.jda
        val channel: MessageChannel = if (dependencyMessage.guildId != null) {
          val guild: Guild = jda.getGuildById(dependencyMessage.guildId) ?: return
          guild.getTextChannelById(dependencyMessage.channelId) ?: return
        } else {
          jda.getPrivateChannelById(dependencyMessage.channelId) ?: return
        }
        val message: Message = channel.retrieveMessageById(dependencyMessage.messageId).complete() ?: return
        message.delete().queue {
          messageDependencyConnector.removeMessage(dependencyMessage)
        }
      } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Failed to delete dependent message: $dependencyMessage", e)
      }
    }
  }
}