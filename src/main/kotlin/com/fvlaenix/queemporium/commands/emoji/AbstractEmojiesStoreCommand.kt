package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.commands.MessagesStoreCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.utils.CoroutineUtils.channelTransform
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.takeWhile
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import kotlin.time.Duration

private val LOG = Logging.getLogger(AbstractEmojiesStoreCommand::class.java)

abstract class AbstractEmojiesStoreCommand(
  val databaseConfiguration: DatabaseConfiguration,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageEmojiDataConnector = MessageEmojiDataConnector(databaseConfiguration.toDatabase())
  private val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())

  protected suspend fun runOverOld(
    jda: JDA,
    takeDistance: Duration,
    guildThreshold: Int,
    channelThreshold: Int,
    messageThreshold: Int,
    reactionThreshold: Int,
    isShuffle: Boolean
  ) {
    LOG.info(
      "Start emojies collect. Distance: $takeDistance. Guild Threshold: $guildThreshold, Channel Threshold: $channelThreshold, Message Threshold: $messageThreshold, Reaction Threshold: $reactionThreshold, Shuffle: $isShuffle"
    )
    val computeGuild: (Guild) -> List<MessageChannel> = { guild ->
      val channels = guild.channels.mapNotNull channel@{ channel -> channel as? MessageChannel }
      if (isShuffle) {
        channels.shuffled()
      } else {
        channels
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
            LOG.error("Failed to get emoji", exception)
          }.getOrNull()
        }
        channelTransform(reactions, reactionThreshold) { reactionList ->
          val emojiDatas = reactionList.map { reaction -> EmojiData(messageId, reaction.second, reaction.first) }
          emojiDataConnector.insert(emojiDatas)
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
      computeMessage = computeMessage,
      isShuffled = isShuffle
    )
  }

  protected suspend fun runOverOldWithMessageStore(
    messagesStoreCommand: MessagesStoreCommand,
    takeDistance: Duration,
    guildThreshold: Int,
    channelThreshold: Int,
    messageThreshold: Int,
    reactionThreshold: Int,
    isShuffle: Boolean
  ) {
    LOG.info(
      "Start emojies collect with MessageStoreService. Distance: $takeDistance. Guild Threshold: $guildThreshold, Channel Threshold: $channelThreshold, Message Threshold: $messageThreshold, Reaction Threshold: $reactionThreshold, Shuffle: $isShuffle"
    )

    val startTime = System.currentTimeMillis() / 1000
    val timeFilter: (Message) -> Boolean = { message ->
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
            LOG.error("Failed to get emoji", exception)
          }.getOrNull()
        }
        channelTransform(reactions, reactionThreshold) { reactionList ->
          val emojiDatas = reactionList.map { reaction -> EmojiData(messageId, reaction.second, reaction.first) }
          emojiDataConnector.insert(emojiDatas)
        }
      }
      messageEmojiDataConnector.insert(MessageEmojiData(messageId, message.reactions.sumOf { it.count }))
    }

    val messageStoreService = messagesStoreCommand.messageStoreService.await()
    messageStoreService.registerConsumer()

    try {
      var totalGuilds = 0
      var totalChannels = 0
      var totalMessages = 0

      messageStoreService.guilds().collect { guild ->
        totalGuilds++
        LOG.info("EmojiesStore: Processing guild $totalGuilds")

        guild.channels().collect { channel ->
          totalChannels++
          LOG.info("EmojiesStore: Processing channel $totalChannels")

          if (isShuffle) {
            val batchSize = 500
            var messages = channel.takeNext(batchSize)
            while (messages.isNotEmpty()) {
              val filteredMessages = messages.filter(timeFilter).shuffled()
              filteredMessages.forEach { message ->
                computeMessage(message)
                totalMessages++
                if (totalMessages % 100 == 0) {
                  LOG.info("EmojiesStore: Processed $totalMessages messages")
                }
              }
              if (messages.size < batchSize) break
              messages = channel.takeNext(batchSize)
            }
          } else {
            channel.messages().takeWhile(timeFilter).collect { message ->
              computeMessage(message)
              totalMessages++
              if (totalMessages % 100 == 0) {
                LOG.info("EmojiesStore: Processed $totalMessages messages")
              }
            }
          }
        }
      }

      LOG.info("EmojiesStore: Finish - processed $totalMessages messages across $totalChannels channels in $totalGuilds guilds")
    } finally {
      messageStoreService.unregisterConsumer()
    }
  }
}