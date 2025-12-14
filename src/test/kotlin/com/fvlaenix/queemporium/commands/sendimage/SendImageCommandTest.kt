package com.fvlaenix.queemporium.commands.sendimage

import com.fvlaenix.queemporium.commands.SendImageCommand
import com.fvlaenix.queemporium.database.ImageMappingTable
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.service.S3FileData
import com.fvlaenix.queemporium.service.S3FileResult
import com.fvlaenix.queemporium.verification.verify
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class SendImageCommandTest : BaseSendImageCommandTest() {

  override fun getFeaturesForTest(): Array<String> {
    return arrayOf(FeatureKeys.SEND_IMAGE)
  }

  @Test
  fun `test usage error - no key provided`() {
    val result = env.sendMessage(
      defaultGuildName,
      defaultGeneralChannelName,
      testUser,
      "/shogun-sama image",
      emptyList()
    )

    runWithScenario {
      awaitAll()
    }

    result.complete(true)

    answerService.verify {
      messageCount(1)
      lastMessageContains(SendImageCommand.ERROR_USAGE)
    }
  }

  @Test
  fun `test key not found in database`() {
    sendImageMessage(key = "missing-key")

    answerService.verify {
      messageCount(1)
      lastMessageContains(SendImageCommand.ERROR_KEY_NOT_FOUND)
      lastMessageContains("missing-key")
    }
  }

  @Test
  fun `test S3 file not found`() {
    insertImageMapping("test-key", "path/to/file.png")
    mockS3FileService.setResponseForPath("path/to/file.png", S3FileResult.NotFound)

    sendImageMessage(key = "test-key")

    answerService.verify {
      messageCount(1)
      lastMessageContains(SendImageCommand.ERROR_FILE_NOT_FOUND)
    }
  }

  @Test
  fun `test S3 file too large`() {
    insertImageMapping("large-key", "path/to/large-file.png")
    mockS3FileService.setResponseForPath("path/to/large-file.png", S3FileResult.TooLarge)

    sendImageMessage(key = "large-key")

    answerService.verify {
      messageCount(1)
      lastMessageContains(SendImageCommand.ERROR_FILE_TOO_LARGE)
    }
  }

  @Test
  fun `test S3 fetch error`() {
    insertImageMapping("error-key", "path/to/error-file.png")
    mockS3FileService.setResponseForPath("path/to/error-file.png", S3FileResult.Error("Connection failed"))

    sendImageMessage(key = "error-key")

    answerService.verify {
      messageCount(1)
      lastMessageContains(SendImageCommand.ERROR_FETCH_FAILED)
    }
  }

  @Test
  fun `test successful file fetch`() {
    insertImageMapping("success-key", "images/test-image.png")

    val testFileData = S3FileData(
      bytes = ByteArray(1024) { it.toByte() },
      filename = "test-image.png"
    )

    mockS3FileService.setResponseForPath("images/test-image.png", S3FileResult.Success(testFileData))

    sendImageMessage(key = "success-key")

    answerService.verify {
      messageCount(1)
      lastMessageContains("test-image.png")
      lastMessageContains("1024 bytes")
    }
  }

  private fun insertImageMapping(key: String, s3Path: String) {
    val databaseConfig =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.configuration.DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    transaction(database) {
      ImageMappingTable.insert {
        it[ImageMappingTable.key] = key
        it[ImageMappingTable.s3Path] = s3Path
      }
    }
  }
}
