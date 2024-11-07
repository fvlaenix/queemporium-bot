package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val LOG = Logger.getLogger(OnlineEmojiesStoreCommand::class.java.name)

class OnlineEmojiesStoreCommand(
  databaseConfiguration: DatabaseConfiguration,
  parameters: Map<String, String>
) : AbstractEmojiesStoreCommand(databaseConfiguration) {

  private val distanceInDays = parameters["distanceInDays"]?.toIntOrNull() ?: 7

  override suspend fun onReadySuspend(event: ReadyEvent) {
    while (true) {
      runCatching {
        runOverOld(event.jda, distanceInDays.days)
      }.onFailure { exception ->
        LOG.log(Level.SEVERE, "Error while running emojies collect", exception)
      }

      delay(12.hours)
    }
  }
}