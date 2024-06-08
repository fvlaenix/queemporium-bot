package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class ImageId(val messageId: String, val numberInMessage: Int)