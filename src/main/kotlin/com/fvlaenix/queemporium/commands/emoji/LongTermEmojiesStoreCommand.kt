package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

private val LOG = Logger.getLogger(LongTermEmojiesStoreCommand::class.java.name)

class LongTermEmojiesStoreCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val config: LongTermEmojiesStoreCommandConfig,
  coroutineProvider: BotCoroutineProvider
) : AbstractEmojiesStoreCommand(databaseConfiguration, coroutineProvider) {

  override suspend fun onReadySuspend(event: ReadyEvent) {
    runCatching {
      runOverOld(
        event.jda,
        config.distanceInDays.days,
        config.guildThreshold,
        config.channelThreshold,
        config.messageThreshold,
        config.emojisThreshold,
        config.isShuffle
      )
    }.onFailure { exception ->
      LOG.log(Level.SEVERE, "Error while running emojies collect", exception)
    }
  }
}