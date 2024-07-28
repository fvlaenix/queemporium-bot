package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.utils.CoroutineUtils.channelTransform
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val LOG = Logger.getLogger(EmojiesStoreCommand::class.java.name)

class EmojiesStoreCommand(val databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageEmojiDataConnector = MessageEmojiDataConnector(databaseConfiguration.toDatabase())
  private val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())
  
  private suspend fun runOverOld(jda: JDA) {
    LOG.log(Level.INFO, "Start emojies collect")
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
    val startTime = System.currentTimeMillis() / 1000
    val takeWhile: (Message) -> Boolean = { message ->
      message.timeCreated.toEpochSecond() + 7.days.inWholeSeconds > startTime
    }
    val computeMessage: suspend (Message) -> Unit = computeMessage@{ message ->
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
      val messageEmojiData = messageEmojiDataConnector.get(messageId)
      if (messageEmojiData?.count == message.reactions.sumOf { it.count }) return@computeMessage
      
      messageEmojiDataConnector.delete(messageId)
      emojiDataConnector.delete(messageId)
      
      coroutineScope {
        val reactions = channelTransform(message.reactions, 16) { messageReaction ->
          kotlin.runCatching {
            messageReaction.retrieveUsers().complete().map { author ->
              Pair(author.id, messageReaction.emoji.name)
            }
          }.onFailure { exception ->
            LOG.log(Level.SEVERE, "Failed to get emoji", exception)
          }.getOrNull()
        }
        channelTransform(reactions, 16) { reactionList ->
          reactionList.forEach { reaction ->
            emojiDataConnector.insert(EmojiData(messageId, reaction.second, reaction.first))
          }
        }
      }
      messageEmojiDataConnector.insert(MessageEmojiData(messageId, message.reactions.sumOf { it.count }))
    }
    runOverOld(
      jda = jda,
      jobName = "EmojiesStore",
      computeGuild = computeGuild,
      takeWhile = takeWhile,
      computeMessage = computeMessage
    )
  }
  
  override suspend fun onReadySuspend(event: ReadyEvent) {
    while (true) {
      kotlin.runCatching {
        runOverOld(event.jda)
      }.onFailure { exception ->
        LOG.log(Level.SEVERE, "Error while running emojies collect", exception) 
      }
      
      delay(12.hours)
    }
  }
}