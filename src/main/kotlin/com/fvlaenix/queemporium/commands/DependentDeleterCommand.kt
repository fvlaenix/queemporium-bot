package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger

private val LOG: Logger = Logger.getLogger(DependentDeleterCommand::class.java.name)

class DependentDeleterCommand(
  databaseConfiguration: DatabaseConfiguration,
  coroutineProvider: BotCoroutineProvider,
  private val messagesStoreCommand: MessagesStoreCommand
) : CoroutineListenerAdapter(coroutineProvider) {
  private val messageDependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())

  private val flowCollectorScope = CoroutineScope(SupervisorJob() + coroutineProvider.botPool)
  private var collectorJob: Job? = null

  override suspend fun onReadySuspend(event: ReadyEvent) {
    collectorJob = messagesStoreCommand.deleted
      .onEach { deleteEvent ->
        coroutineProvider.mainScope.launch(coroutineProvider.botPool) {
          handleMessageDelete(deleteEvent)
        }
      }
      .launchIn(flowCollectorScope)
  }

  private suspend fun handleMessageDelete(event: MessageDeleteEvent) {
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