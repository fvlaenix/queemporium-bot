package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.CheckImageResponseImagesInfoKt.checkImageResponseImageInfo
import com.fvlaenix.duplicate.protobuf.addImageResponse
import com.fvlaenix.duplicate.protobuf.addImageResponseOk
import com.fvlaenix.duplicate.protobuf.checkImageResponseImagesInfo
import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.verification.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.Ignore

/**
 * Simple test of the bot's interaction with DuplicateImagesService.
 * Following the style of existing OnlineCompareTest.
 */
class SimpleDuplicateServiceTest : BaseGrpcTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(OnlinePictureCompare::class)
  }

  @Test
  @Ignore("Failed because we can't make getHistoryAround without pain")
  fun `test bot handles duplicate detection`() {
    // Configure the compression size response
    duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
      .setX(800)
      .build()

    // Create test environment
    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general")
        withChannel("duplicate-channel")
      }
    }

    // Configure duplicate detection channel
    val guildInfoConnector = getGuildInfoConnector()
    val testGuild = env.jda.getGuildsByName("Test Guild", false).first()
    val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()
    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    // Create a user
    val user = env.createUser("Test User", false)

    // Send original message first to add it to the database
    val creation = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "This is the original message",
      listOf(createTestAttachment("original.jpg"))
    )
    val message = creation.complete()

    // Wait for processing
    env.awaitAll()

    // Clear any messages from the first operation
    answerService.answers.clear()

    // Configure duplicate response with a dummy original message ID
    val additionalInfo = AdditionalImageInfo(
      fileName = "original.jpg",
      isSpoiler = false,
      originalSizeWidth = 100,
      originalSizeHeight = 100
    )

    // Configure the correct addImageResponse
    duplicateService.addImageResponse = addImageResponse {
      this.responseOk = addImageResponseOk {
        this.isAdded = true
        this.imageInfo = checkImageResponseImagesInfo {
          this.images.add(
            checkImageResponseImageInfo {
              this.messageId = message.id
              this.numberInMessage = 0
              this.additionalInfo = Json.encodeToString(additionalInfo)
              this.level = 95
            }
          )
        }
      }
    }

    // Send a message that should be detected as a duplicate
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "This message should be detected as a duplicate",
      listOf(createTestAttachment("duplicate.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Verify that the bot detected the duplicate
    answerService.verify {
      // Should have at least one message
      messageCount(1)

      // Message should be about a repost
      messagesContain("made repost")

      // Message should reference the original message ID
      messagesContain(message.id)
    }
  }

  @Test
  @Ignore("Not supported yet")
  fun `test bot handles service unavailability`() {
    // Configure the compression size response
    duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
      .setX(800)
      .build()

    // Create test environment
    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general")
        withChannel("duplicate-channel")
      }
    }

    // Configure duplicate detection channel
    val guildInfoConnector = getGuildInfoConnector()
    val testGuild = env.jda.getGuildsByName("Test Guild", false).first()
    val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()
    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    // Simulate server unavailability
    simulateServerUnavailable()

    // Create a user
    val user = env.createUser("Test User", false)

    // Send a message with an image when the service is unavailable
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message when service is unavailable",
      listOf(createTestAttachment("test_image.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Verify that the bot handled the service unavailability
    answerService.verify {
      messagesContain("временно недоступен")
    }
  }
}