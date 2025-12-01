package com.fvlaenix.queemporium.configuration.commands

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class LongTermEmojiesStoreCommandConfig(
  val distanceInDays: Int,
  val guildThreshold: Int,
  val channelThreshold: Int,
  val messageThreshold: Int,
  val emojisThreshold: Int,
  val isShuffle: Boolean
) {
  init {
    require(distanceInDays > 0) { "distanceInDays must be positive" }
    require(guildThreshold > 0) { "guildThreshold must be positive" }
    require(channelThreshold > 0) { "channelThreshold must be positive" }
    require(messageThreshold > 0) { "messageThreshold must be positive" }
    require(emojisThreshold > 0) { "emojisThreshold must be positive" }
  }

  companion object {
    fun fromParams(params: JsonObject): LongTermEmojiesStoreCommandConfig {
      return LongTermEmojiesStoreCommandConfig(
        distanceInDays = params["distanceInDays"]?.jsonPrimitive?.intOrNull ?: 7,
        guildThreshold = params["guildThreshold"]?.jsonPrimitive?.intOrNull ?: 2,
        channelThreshold = params["channelThreshold"]?.jsonPrimitive?.intOrNull ?: 4,
        messageThreshold = params["messageThreshold"]?.jsonPrimitive?.intOrNull ?: Runtime.getRuntime()
          .availableProcessors(),
        emojisThreshold = params["emojisThreshold"]?.jsonPrimitive?.intOrNull ?: 16,
        isShuffle = params["isShuffle"]?.jsonPrimitive?.booleanOrNull ?: true
      )
    }
  }
}
