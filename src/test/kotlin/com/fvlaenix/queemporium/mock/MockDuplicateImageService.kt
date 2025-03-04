// Новый файл MockDuplicateImageService.kt
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

class MockDuplicateImageService : DuplicateImageService {
  // Хранилище для предопределенных ответов по имени файла
  private val responsesByFileName = mutableMapOf<String, List<DuplicateImageService.DuplicateImageData>>()

  // Для общих ответов, не привязанных к конкретному имени файла
  var nextResponse: List<DuplicateImageService.DuplicateImageData>? = null
  var defaultCompressSize: CompressSize = CompressSize(width = 500, height = null)

  // Флаг для имитации неработающего сервера
  var isServerAlive: Boolean = true

  // Настраиваем ответ для конкретного файла
  fun setResponseForFile(fileName: String, response: List<DuplicateImageService.DuplicateImageData>) {
    responsesByFileName[fileName] = response
  }

  // Очистка всех настроенных ответов
  fun clearResponses() {
    responsesByFileName.clear()
    nextResponse = null
  }

  override suspend fun addImageWithCheck(
    message: Message,
    compressSize: CompressSize,
    withHistoryReload: Boolean,
    callback: suspend (Pair<MessageUtils.MessageImageInfo, List<DuplicateImageService.DuplicateImageData>>) -> Unit
  ) {
    val imagesChannel = coroutineScope {
      addImagesFromMessage(
        message = message,
        withHistoryReload = false,
        compressSize = compressSize
      )
    }

    val duplicateChannel = Channel<Pair<MessageUtils.MessageImageInfo, List<DuplicateImageService.DuplicateImageData>>>(Channel.UNLIMITED)

    try {
      // Обрабатываем каждое изображение
      imagesChannel.consumeEach { imageInfo ->
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
    // Проверяем "работоспособность" сервера
    return if (isServerAlive) defaultCompressSize else null
  }

  override suspend fun deleteImage(deleteData: List<DuplicateImageService.DeleteImageData>) {
    // Пустая реализация, так как мы не храним реальные данные
    // В реальной ситуации здесь можно было бы удалять из responsesByFileName
  }
}