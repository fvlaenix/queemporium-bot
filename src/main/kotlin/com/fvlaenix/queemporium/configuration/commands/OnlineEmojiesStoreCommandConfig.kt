package com.fvlaenix.queemporium.configuration.commands

import com.fvlaenix.queemporium.commands.emoji.OnlineEmojiesStoreCommand
import com.fvlaenix.queemporium.configuration.BotConfiguration
import java.util.logging.Level
import java.util.logging.Logger

private val LOG = Logger.getLogger(OnlineEmojiesStoreCommandConfig::class.java.name)

class OnlineEmojiesStoreCommandConfig(
  val distanceInDays: Int,
  val guildThreshold: Int,
  val channelThreshold: Int,
  val messageThreshold: Int,
  val emojisThreshold: Int
) {
  init {
    require(distanceInDays > 0) { "distanceInDays must be positive" }
    require(guildThreshold > 0) { "guildThreshold must be positive" }
    require(channelThreshold > 0) { "channelThreshold must be positive" }
    require(messageThreshold > 0) { "messageThreshold must be positive" }
    require(emojisThreshold > 0) { "emojisThreshold must be positive" }
  }

  companion object {
    private val CLASS_NAME = OnlineEmojiesStoreCommand::class.java.name

    fun load(botConfiguration: BotConfiguration): OnlineEmojiesStoreCommandConfig {
      LOG.log(Level.INFO, "Loading OnlineEmojiesStoreCommand configuration")

      val feature = botConfiguration.features
        .find { it.className == CLASS_NAME }
        ?: return OnlineEmojiesStoreCommandConfig(7, 2, 4, Runtime.getRuntime().availableProcessors(), 16)

      return OnlineEmojiesStoreCommandConfig(
        distanceInDays = feature.parameter
          .firstOrNull { it.name == "distanceInDays" }
          ?.value?.toIntOrNull() ?: 7,
        guildThreshold = feature.parameter
          .firstOrNull { it.name == "guildThreshold" }
          ?.value?.toIntOrNull() ?: 2,
        channelThreshold = feature.parameter
          .firstOrNull { it.name == "channelThreshold" }
          ?.value?.toIntOrNull() ?: 4,
        messageThreshold = feature.parameter
          .firstOrNull { it.name == "messageThreshold" }
          ?.value?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors(),
        emojisThreshold = feature.parameter
          .firstOrNull { it.name == "emojisThreshold" }
          ?.value?.toIntOrNull() ?: 16
      )
    }
  }
}