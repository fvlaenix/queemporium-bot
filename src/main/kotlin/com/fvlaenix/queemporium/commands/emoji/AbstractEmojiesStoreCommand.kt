package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.utils.CoroutineUtils.channelTransform
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration

private val LOG = Logger.getLogger(AbstractEmojiesStoreCommand::class.java.name)

abstract class AbstractEmojiesStoreCommand(
  val databaseConfiguration: DatabaseConfiguration
) : CoroutineListenerAdapter() {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageEmojiDataConnector = MessageEmojiDataConnector(databaseConfiguration.toDatabase())
  private val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())

  protected suspend fun runOverOld(
    jda: JDA,
    takeDistance: Duration,
    guildThreshold: Int,
    channelThreshold: Int,
    messageThreshold: Int,
    reactionThreshold: Int
  ) {
    LOG.log(Level.INFO, "Start emojies collect. Distance: $takeDistance. Guild Threshold: $guildThreshold, Channel Threshold: $channelThreshold, Message Threshold: $messageThreshold, Reaction Threshold: $reactionThreshold")
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
      message.timeCreated.toEpochSecond() + takeDistance.inWholeSeconds > startTime
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
        val reactions = channelTransform(message.reactions, reactionThreshold) { messageReaction ->
          runCatching {
            messageReaction.retrieveUsers().complete().map { author ->
              Pair(author.id, messageReaction.emoji.name)
            }
          }.onFailure { exception ->
            LOG.log(Level.SEVERE, "Failed to get emoji", exception)
          }.getOrNull()
        }
        channelTransform(reactions, reactionThreshold) { reactionList ->
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
      guildThreshold = guildThreshold,
      channelThreshold = channelThreshold,
      messageThreshold = messageThreshold,
      computeGuild = computeGuild,
      takeWhile = takeWhile,
      computeMessage = computeMessage
    )
  }
}