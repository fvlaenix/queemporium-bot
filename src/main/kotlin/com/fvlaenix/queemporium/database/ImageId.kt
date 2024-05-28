package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class ImageId(val guildId: String?, val channelId: String, val messageId: String, val numberInMessage: Int) {
  fun toMessageId(): MessageId = MessageId(guildId, channelId, messageId)
}