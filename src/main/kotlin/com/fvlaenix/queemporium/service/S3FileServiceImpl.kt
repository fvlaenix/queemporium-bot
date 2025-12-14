package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.configuration.S3Configuration
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

class S3FileServiceImpl(
  private val config: S3Configuration
) : S3FileService {
  companion object {
    private val LOG = Logging.getLogger(S3FileServiceImpl::class.java)
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024L
  }

  private val s3Client: S3Client by lazy {
    val credentials = AwsBasicCredentials.create(config.accessKey, config.secretKey)
    S3Client.builder()
      .region(Region.of(config.region))
      .credentialsProvider(StaticCredentialsProvider.create(credentials))
      .build()
  }

  override suspend fun fetchFile(s3Path: String): S3FileResult = withContext(Dispatchers.IO) {
    try {
      LOG.info("Fetching file from S3: bucket=${config.bucketName}, key=$s3Path")

      val headRequest = HeadObjectRequest.builder()
        .bucket(config.bucketName)
        .key(s3Path)
        .build()

      val headResponse = try {
        s3Client.headObject(headRequest)
      } catch (e: NoSuchKeyException) {
        LOG.warn("S3 object not found: bucket=${config.bucketName}, key=$s3Path")
        return@withContext S3FileResult.NotFound
      }

      val contentLength = headResponse.contentLength()
      if (contentLength > MAX_FILE_SIZE) {
        LOG.warn("S3 object too large: bucket=${config.bucketName}, key=$s3Path, size=$contentLength, max=$MAX_FILE_SIZE")
        return@withContext S3FileResult.TooLarge
      }

      val getRequest = GetObjectRequest.builder()
        .bucket(config.bucketName)
        .key(s3Path)
        .build()

      val bytes = s3Client.getObjectAsBytes(getRequest).asByteArray()
      val filename = extractFilename(s3Path)

      LOG.info("Successfully fetched file from S3: bucket=${config.bucketName}, key=$s3Path, size=${bytes.size}, filename=$filename")
      S3FileResult.Success(S3FileData(bytes, filename))
    } catch (e: NoSuchKeyException) {
      LOG.warn("S3 object not found during GET: bucket=${config.bucketName}, key=$s3Path")
      S3FileResult.NotFound
    } catch (e: SdkException) {
      LOG.error("S3 SDK error: bucket=${config.bucketName}, key=$s3Path", e)
      S3FileResult.Error("S3 error: ${e.message}")
    } catch (e: Exception) {
      LOG.error("Unexpected error fetching from S3: bucket=${config.bucketName}, key=$s3Path", e)
      S3FileResult.Error("Unexpected error: ${e.message}")
    }
  }

  private fun extractFilename(s3Path: String): String {
    val parts = s3Path.split("/")
    val basename = parts.lastOrNull()?.takeIf { it.isNotEmpty() }
    return basename ?: "file"
  }
}
