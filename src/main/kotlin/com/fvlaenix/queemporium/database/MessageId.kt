package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class MessageId(val guildId: String, val channelId: String, val messageId: String)