package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import com.fvlaenix.queemporium.database.MessageId
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

private val LOG: Logger = LogManager.getLogManager().getLogger(DependentDeleterCommand::class.java.name)

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
        val guild: Guild = jda.getGuildById(dependencyMessage.guildId) ?: return
        val channel: TextChannel = guild.getTextChannelById(dependencyMessage.channelId) ?: return
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