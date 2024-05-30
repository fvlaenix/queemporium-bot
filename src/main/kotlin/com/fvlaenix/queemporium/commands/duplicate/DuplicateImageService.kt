package com.fvlaenix.queemporium.commands.duplicate

import DUPLICATE_IMAGE_HOSTNAME
import com.fvlaenix.alive.protobuf.isAliveRequest
import com.fvlaenix.duplicate.protobuf.DuplicateImagesServiceGrpcKt
import com.fvlaenix.duplicate.protobuf.addImageRequest
import com.fvlaenix.duplicate.protobuf.getCompressionSizeRequest
import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.ImageId
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import com.fvlaenix.queemporium.utils.ChannelUtils
import com.fvlaenix.queemporium.utils.ChannelUtils.STANDARD_IMAGE_CHANNEL_SIZE
import com.fvlaenix.queemporium.utils.MessageUtils
import com.fvlaenix.queemporium.utils.MessageUtils.addImagesFromMessage
import com.google.protobuf.kotlin.toByteString
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension

private val LOG = Logger.getLogger(DuplicateImageService::class.java.name)

object DuplicateImageService {
  private val DUPLICATE_SEND_SEMAPHORE = Semaphore(20)
  
  class DuplicateImageData(
    val imageId: ImageId,
    val additionalImageInfo: AdditionalImageInfo,
    val level: Long
  )
  
  suspend fun <T> withOpenedChannel(block: suspend (DuplicateImagesServiceGrpcKt.DuplicateImagesServiceCoroutineStub) -> T): T {
    val duplicateImageChannel = ManagedChannelBuilder.forAddress(DUPLICATE_IMAGE_HOSTNAME, 50055)
      .usePlaintext()
      .maxInboundMessageSize(STANDARD_IMAGE_CHANNEL_SIZE) // 50 mb
      .build()
    val duplicateChannelService = DuplicateImagesServiceGrpcKt.DuplicateImagesServiceCoroutineStub(duplicateImageChannel)
    return ChannelUtils.runWithClose(duplicateImageChannel, duplicateChannelService, block)
  }
  
  private suspend fun sendPicture(
    imageId: ImageId,
    additionalImageInfo: AdditionalImageInfo,
    image: BufferedImage,
    fileName: String,
    epoch: Long
  ): List<DuplicateImageData>? {
    val result = try {
      withOpenedChannel { service ->
        service.addImageWithCheck(
          addImageRequest {
            this.group = imageId.guildId ?: "private-channel-${imageId.channelId}"
            this.imageId = Json.encodeToString(imageId)
            this.additionalInfo = Json.encodeToString(additionalImageInfo)
            this.image = com.fvlaenix.image.protobuf.image {
              val outputStream = ByteArrayOutputStream()
              ImageIO.write(image, Path(fileName).extension, outputStream)
              this.content = outputStream.toByteArray().toByteString()
              this.fileName = fileName
            }
            this.timestamp = epoch
          }
        )
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Can't send request to server with image: $imageId", e)
      return null
    }
    if (result.hasError()) {
      LOG.log(Level.SEVERE, "Error in request to server with image: $imageId: ${result.error}")
    }
    val responseOK = result.responseOk
    return responseOK.imageInfo.imagesList.map { DuplicateImageData(
      imageId = Json.decodeFromString(it.imageId),
      additionalImageInfo = Json.decodeFromString(it.additionalInfo),
      level = it.level
    ) }
  }
  
  private suspend fun CoroutineScope.sendPictures(
    epoch: Long,
    channelInput: Channel<MessageUtils.MessageImageInfo>,
    channelOutput: Channel<Pair<MessageUtils.MessageImageInfo, List<DuplicateImageData>>>
  ) {
    launch(EXCEPTION_HANDLER) {
      val jobs = mutableListOf<Job>()
      for (picture in channelInput) {
        val job = launch(EXCEPTION_HANDLER) picture@{
          val result = sendPicture(
            imageId = picture.imageId,
            additionalImageInfo = picture.additionalImageInfo,
            image = picture.bufferedImage,
            fileName = picture.additionalImageInfo.fileName,
            epoch = epoch
          )
          if (result == null) {
            LOG.log(Level.SEVERE, "Failed to find duplicate: ${picture.imageId}")
            return@picture
          }
          if (result.isEmpty()) {
            return@picture
          }
          channelOutput.send(picture to result)
        }
        jobs.add(job)
      }
      launch(EXCEPTION_HANDLER) {
        jobs.joinAll()
        channelOutput.close()
      }
    }
  }

  suspend fun sendPictures(message: Message, compressSize: CompressSize, withHistoryReload: Boolean, callback: (Pair<MessageUtils.MessageImageInfo, List<DuplicateImageData>>) -> Unit) {
    coroutineScope {
      DUPLICATE_SEND_SEMAPHORE.withPermit {
        val imagesChannel = addImagesFromMessage(message, withHistoryReload, compressSize)
        val duplicateChannel = Channel<Pair<MessageUtils.MessageImageInfo, List<DuplicateImageData>>>(Channel.UNLIMITED)
        sendPictures(message.timeCreated.toEpochSecond(), imagesChannel, duplicateChannel)
        for (duplicate in duplicateChannel) {
          try {
            callback(duplicate)
          } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Error in callback function: ${duplicate.first}", e)
          }
        }
      }
    }
  }
  
  suspend fun checkServerAliveness(event: ReadyEvent): CompressSize? {
    var compressSize: CompressSize? = null
    val isAlive = ChannelUtils.checkServerAliveness("duplicateImages") {
      withOpenedChannel { service ->
        service.isAlive(isAliveRequest {  })
        compressSize = service.getImageCompressionSize(getCompressionSizeRequest {  }).let { 
          CompressSize(it.x.takeIf { it > 0 }, it.y.takeIf { it > 0 }) 
        }
      }
    }
    if (!isAlive) {
      LOG.log(Level.SEVERE, "Server is not alive")
      event.jda.shutdown()
    }
    return compressSize
  }
}