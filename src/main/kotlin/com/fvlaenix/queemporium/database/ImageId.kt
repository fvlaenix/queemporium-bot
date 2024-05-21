package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class ImageId(val serverId: String, val channelId: String, val messageId: String)