package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.commands.halloffame.HallOfFameCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.commands.OnlineEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val LOG = Logging.getLogger(OnlineEmojiesStoreCommand::class.java)

class OnlineEmojiesStoreCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val config: OnlineEmojiesStoreCommandConfig,
  coroutineProvider: BotCoroutineProvider
) : AbstractEmojiesStoreCommand(databaseConfiguration, coroutineProvider) {
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageEmojiDataConnector = MessageEmojiDataConnector(databaseConfiguration.toDatabase())
  private val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())

  private var jda: JDA? = null

  override suspend fun onReadySuspend(event: ReadyEvent) {
    jda = event.jda
    while (true) {
      runCatching {
        runOverOld(
          event.jda,
          config.distanceInDays.days,
          config.guildThreshold,
          config.channelThreshold,
          config.messageThreshold,
          config.emojisThreshold,
          false
        )
      }.onFailure { exception ->
        LOG.error("Error while running emojies collect", exception)
      }

      coroutineProvider.safeDelay(12.hours)
    }
  }

  override suspend fun onMessageReactionAddSuspend(event: MessageReactionAddEvent) {
    if (!event.isFromGuild) return

    val messageId = event.messageId
    val guildId = event.guild.id

    val messageExists = messageDataConnector.get(messageId) != null
    if (!messageExists) {
      LOG.info("Reaction added to unknown message $messageId, skipping real-time processing")
      return
    }

    val emojiId = event.emoji.name
    val authorId = event.userId

    runCatching {
      emojiDataConnector.insert(EmojiData(messageId, emojiId, authorId))

      val message = event.channel.retrieveMessageById(messageId).complete()
      val totalReactions = message.reactions.sumOf { it.count }
      messageEmojiDataConnector.insert(MessageEmojiData(messageId, totalReactions))

      LOG.info("Updated emoji count for message $messageId: $totalReactions reactions")

      getHallOfFameCommand()?.recheckMessage(messageId, guildId)
    }.onFailure { exception ->
      LOG.error("Failed to process reaction add event for message $messageId", exception)
    }
  }

  override suspend fun onMessageReactionRemoveSuspend(event: MessageReactionRemoveEvent) {
    if (!event.isFromGuild) return

    val messageId = event.messageId
    val guildId = event.guild.id

    val messageExists = messageDataConnector.get(messageId) != null
    if (!messageExists) {
      LOG.debug("Reaction removed from unknown message $messageId, skipping")
      return
    }

    runCatching {
      val message = event.channel.retrieveMessageById(messageId).complete()
      val totalReactions = message.reactions.sumOf { it.count }
      messageEmojiDataConnector.insert(MessageEmojiData(messageId, totalReactions))

      LOG.info("Updated emoji count for message $messageId: $totalReactions reactions (removed)")

      getHallOfFameCommand()?.recheckMessage(messageId, guildId)
    }.onFailure { exception ->
      LOG.error("Failed to process reaction remove event for message $messageId", exception)
    }
  }

  private fun getHallOfFameCommand(): HallOfFameCommand? {
    return jda?.registeredListeners?.filterIsInstance<HallOfFameCommand>()?.firstOrNull()
  }
}
