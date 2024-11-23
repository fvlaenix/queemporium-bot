package com.fvlaenix.queemporium.utils

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.Size
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.coobird.thumbnailator.Thumbnails
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.utils.AttachmentProxy
import okhttp3.OkHttpClient
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.jvm.Throws

private val LOG = Logger.getLogger(DownloadUtils::class.java.name)
private const val STANDARD_ATTEMPTS = 6

object DownloadUtils {
  
  private val DOWNLOAD_SEMAPHORE = Semaphore(16)
  
  @Throws(IOException::class)
  private fun readImage(inputStringGetter: () -> InputStream): BufferedImage? {
    return inputStringGetter().use { inputStream ->
      ImageIO.read(inputStream)
    }
  }
  
  @Throws(IOException::class)
  private fun readImageRepeatedly(inputStringGetter: () -> InputStream, attempts: Int = STANDARD_ATTEMPTS): BufferedImage {
    var attemptsLeft = attempts
    while (attemptsLeft > 0) {
      val image: BufferedImage? = try {
        readImage(inputStringGetter)
      } catch (e: IOException) {
        if (attemptsLeft == 1) throw e
        null
      }
      if (image != null) {
        return image
      }
      attemptsLeft--
    }
    throw IOException("Count of attempts failed")
  }
  
  private fun readImageFromUrl(url: String): Pair<BufferedImage, Size>? {
    return try {
      val image = readImageRepeatedly({ URL(url).openStream() })
      image to Size(image.width, image.height)
    } catch (e: IOException) {
      LOG.log(Level.SEVERE, "Can't read image from url: $url", e)
      null
    }
  }
  
  suspend fun readImageFromUrl(url: String, compressSize: CompressSize?): Pair<BufferedImage, Size>? {
    return DOWNLOAD_SEMAPHORE.withPermit { readImageFromUrl(url) }?.let { (image, size) ->
      if (compressSize == null) {
        image to size
      } else {
        val scaledSize = compressSize.getScaledSize(size)
        Thumbnails.of(image).size(scaledSize.width, scaledSize.height).asBufferedImage() to size
      }
    }
  }
  
  suspend fun readImageFromAttachment(attachment: Attachment, compressSize: CompressSize?): Pair<BufferedImage, AdditionalImageInfo>? {
    var attemptsLeft = STANDARD_ATTEMPTS
    val okHttpClient = OkHttpClient()
    while (attemptsLeft > 0) {
      try {
        val client = object : OkHttpClient() {
          override fun newBuilder(): Builder {
            val multiplier = (STANDARD_ATTEMPTS + 1 - attemptsLeft)
            return Builder()
              .connectTimeout(Duration.ofMillis(okHttpClient.connectTimeoutMillis.toLong() * multiplier))
              .readTimeout(Duration.ofMillis(okHttpClient.readTimeoutMillis.toLong() * multiplier))
              .callTimeout(Duration.ofMillis(okHttpClient.callTimeoutMillis.toLong() * multiplier))
              .writeTimeout(Duration.ofMillis(okHttpClient.writeTimeoutMillis.toLong() * multiplier))
          }
        }
        val proxy = attachment.proxy.withClient(client) as AttachmentProxy
        val originalSize = Size(attachment.width, attachment.height)
        val size = compressSize?.getScaledSize(originalSize) ?: originalSize
        return DOWNLOAD_SEMAPHORE.withPermit {
          readImageRepeatedly(
            { if (compressSize == null) proxy.download().get() else proxy.download(size.width, size.height).get() },
            attempts = 2
          ) to AdditionalImageInfo(attachment.fileName, attachment.isSpoiler, originalSize.width, originalSize.height)
        }
      } catch (e: IOException) {
        LOG.log(Level.SEVERE, "Can't read attachment from file: ${attachment.url}", e)
      } catch (e: ExecutionException) {
        LOG.log(Level.SEVERE, "Can't read attachment from file: ${attachment.url}", e)
      }
      attemptsLeft--
    }
    return null
  }
}