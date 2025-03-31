package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.database.MessageDataConnector
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent

class MessagesStoreCommand(val databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())

  private fun computeMessage(message: Message) {
    val messageId = message.id
    val messageData = MessageData(
      messageId = messageId,
      guildId = message.guildId,
      channelId = message.channelId,
      text = message.contentRaw,
      url = message.jumpUrl,
      authorId = message.author.id,
      epoch = message.timeCreated.toEpochSecond(),
    )

    messageDataConnector.add(messageData)
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    val computeGuild: (Guild) -> List<MessageChannel> = { guild ->
      guild.channels.mapNotNull channel@{ channel ->
        channel.id
        if (channel is MessageChannel) {
          channel
        } else {
          null
        }
      }
    }
    val takeWhile: (Message) -> Boolean = { messageDataConnector.get(it.id) == null }
    runOverOld(
      jda = event.jda,
      jobName = "StoreCommand",
      computeGuild = computeGuild,
      takeWhile = takeWhile,
      computeMessage = ::computeMessage
    )
  }

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    computeMessage(event.message)
  }

  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    messageDataConnector.delete(event.messageId)
  }
}