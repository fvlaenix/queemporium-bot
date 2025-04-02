package com.fvlaenix.queemporium.mock

import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.utils.MessageUtils
import com.fvlaenix.queemporium.utils.MessageUtils.addImagesFromMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.session.ReadyEvent

/**
 * Mock implementation of DuplicateImageService for testing purposes.
 * Allows predefined responses for specific file names and tracks all received requests.
 */
class MockDuplicateImageService : DuplicateImageService {
  // Storage for predefined responses by file name
  private val responsesByFileName = mutableMapOf<String, List<DuplicateImageService.DuplicateImageData>>()

  // For general responses, not tied to specific file name
  var nextResponse: List<DuplicateImageService.DuplicateImageData>? = null
  var defaultCompressSize: CompressSize = CompressSize(width = 500, height = null)

  // Flag to simulate non-working server
  var isServerAlive: Boolean = true

  // List to track all requests made to this service
  private val requests: MutableList<Any> = mutableListOf()

  /**
   * Configure response for a specific file
   */
  fun setResponseForFile(fileName: String, response: List<DuplicateImageService.DuplicateImageData>?) {
    if (response == null) {
      responsesByFileName.remove(fileName)
    } else {
      responsesByFileName[fileName] = response
    }
  }

  fun countAddImageRequests(): Int =
    requests.count { it is AddImageRequest }

  override suspend fun addImageWithCheck(
    message: Message,
    compressSize: CompressSize,
    withHistoryReload: Boolean,
    callback: suspend (Pair<MessageUtils.MessageImageInfo, List<DuplicateImageService.DuplicateImageData>>) -> Unit
  ) {
    // Record this request
    requests.add(AddImageRequest(message.id, compressSize, withHistoryReload))

    val imagesChannel = coroutineScope {
      addImagesFromMessage(
        message = message,
        withHistoryReload = false,
        compressSize = compressSize
      )
    }

    val duplicateChannel =
      Channel<Pair<MessageUtils.MessageImageInfo, List<DuplicateImageService.DuplicateImageData>>>(Channel.UNLIMITED)

    try {
      // Process each image
      imagesChannel.consumeEach { imageInfo ->
        // Record this image processing
        requests.add(
          ProcessImageRequest(
            messageId = message.id,
            fileName = imageInfo.additionalImageInfo.fileName,
            numberInMessage = imageInfo.numberInMessage
          )
        )

        val response = responsesByFileName[imageInfo.additionalImageInfo.fileName]
          ?: nextResponse

        if (response != null) {
          duplicateChannel.send(imageInfo to response)
        }
      }
    } finally {
      duplicateChannel.close()
    }

    for (result in duplicateChannel) {
      callback(result)
    }
  }

  override suspend fun checkServerAliveness(event: ReadyEvent): CompressSize? {
    // Record this request
    requests.add(CheckServerAlivenessRequest())

    // Check "server availability"
    return if (isServerAlive) defaultCompressSize else null
  }

  override suspend fun deleteImage(deleteData: List<DuplicateImageService.DeleteImageData>) {
    // Record this request
    requests.add(DeleteImageRequest(deleteData))
  }

  // Data classes to track different request types

  data class AddImageRequest(
    val messageId: String,
    val compressSize: CompressSize,
    val withHistoryReload: Boolean
  )

  data class ProcessImageRequest(
    val messageId: String,
    val fileName: String,
    val numberInMessage: Int
  )

  class CheckServerAlivenessRequest

  data class DeleteImageRequest(
    val deleteData: List<DuplicateImageService.DeleteImageData>
  )
}