package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.commands.OnlineEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val LOG = Logger.getLogger(OnlineEmojiesStoreCommand::class.java.name)

class OnlineEmojiesStoreCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val config: OnlineEmojiesStoreCommandConfig,
  coroutineProvider: BotCoroutineProvider
) : AbstractEmojiesStoreCommand(databaseConfiguration, coroutineProvider) {

  override suspend fun onReadySuspend(event: ReadyEvent) {
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
        LOG.log(Level.SEVERE, "Error while running emojies collect", exception)
      }

      coroutineProvider.safeDelay(12.hours)
    }
  }
}