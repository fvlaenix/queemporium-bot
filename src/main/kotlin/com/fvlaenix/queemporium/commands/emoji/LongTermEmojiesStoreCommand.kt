package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

private val LOG = Logger.getLogger(LongTermEmojiesStoreCommand::class.java.name)

class LongTermEmojiesStoreCommand(
  databaseConfiguration: DatabaseConfiguration,
  parameters: Map<String, String>
) : AbstractEmojiesStoreCommand(databaseConfiguration) {
  private val distanceInDays = parameters["distanceInDays"]?.toIntOrNull() ?: 7
  private val guildThreshold = parameters["guildThreshold"]?.toIntOrNull() ?: 2
  private val channelsThreshold = parameters["channelThreshold"]?.toIntOrNull() ?: 4
  private val messageThreshold = parameters["messageThreshold"]?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()
  private val emojisThreshold = parameters["emojisThreshold"]?.toIntOrNull() ?: 16
  private val isShuffle = parameters["isShuffle"]?.toBooleanStrictOrNull() == true

  override suspend fun onReadySuspend(event: ReadyEvent) {
    runCatching {
      runOverOld(
        event.jda,
        distanceInDays.days,
        guildThreshold,
        channelsThreshold,
        messageThreshold,
        emojisThreshold,
        isShuffle
      )
    }.onFailure { exception ->
      LOG.log(Level.SEVERE, "Error while running emojies collect", exception)
    }
  }
}