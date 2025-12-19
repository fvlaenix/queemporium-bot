package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.commands.MessagesStoreCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.events.session.ReadyEvent
import kotlin.time.Duration.Companion.days

private val LOG = Logging.getLogger(LongTermEmojiesStoreCommand::class.java)

class LongTermEmojiesStoreCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val config: LongTermEmojiesStoreCommandConfig,
  coroutineProvider: BotCoroutineProvider,
  private val messagesStoreCommand: MessagesStoreCommand
) : AbstractEmojiesStoreCommand(databaseConfiguration, coroutineProvider) {

  override suspend fun onReadySuspend(event: ReadyEvent) {
    runCatching {
      runOverOldWithMessageStore(
        messagesStoreCommand,
        config.distanceInDays.days,
        config.guildThreshold,
        config.channelThreshold,
        config.messageThreshold,
        config.emojisThreshold,
        config.isShuffle
      )
    }.onFailure { exception ->
      LOG.error("Error while running emojies collect with MessageStoreService", exception)
      throw exception
    }
  }
}