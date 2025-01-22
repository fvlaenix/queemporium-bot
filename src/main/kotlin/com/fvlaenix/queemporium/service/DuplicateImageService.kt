package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.utils.MessageUtils
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.session.ReadyEvent

interface DuplicateImageService {
  suspend fun addImageWithCheck(
    message: Message,
    compressSize: CompressSize,
    withHistoryReload: Boolean,
    callback: suspend (Pair<MessageUtils.MessageImageInfo, List<DuplicateImageData>>) -> Unit
  )

  suspend fun checkServerAliveness(event: ReadyEvent): CompressSize?

  suspend fun deleteImage(deleteData: List<DeleteImageData>)

  data class DeleteImageData(val messageId: String, val numberInMessage: Int)

  data class DuplicateImageData(
    val messageId: String,
    val numberInMessage: Int,
    val additionalImageInfo: AdditionalImageInfo,
    val level: Long
  )
}