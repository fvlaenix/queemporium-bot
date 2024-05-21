package com.fvlaenix.queemporium.database

import kotlinx.serialization.Serializable

@Serializable
data class AdditionalImageInfo(val isSpoiler: String, val originalSizeHeight: Int, val originalSizeWidth: Int)