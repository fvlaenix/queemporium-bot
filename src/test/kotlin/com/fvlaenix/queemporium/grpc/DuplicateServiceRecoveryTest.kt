package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.CheckImageResponseImagesInfoKt.checkImageResponseImageInfo
import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.duplicate.protobuf.addImageResponse
import com.fvlaenix.duplicate.protobuf.addImageResponseOk
import com.fvlaenix.duplicate.protobuf.checkImageResponseImagesInfo
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.verification.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.Ignore

/**
 * Test for verifying the bot properly recovers after service disruption.
 */
class DuplicateServiceRecoveryTest : BaseGrpcTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(OnlinePictureCompare::class)
  }

  @Test
  @Ignore("Not supported yet")
  fun `test bot recovers after service disruption`() {
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

    // 1. First phase: Service is unavailable
    simulateServerUnavailable()

    // Send message during service unavailability
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message during service unavailability",
      listOf(createTestAttachment("unavailable_test.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Verify error message
    answerService.verify {
      messagesContain("unavailable")
    }

    // Clear any messages from first operation
    answerService.answers.clear()

    // 2. Second phase: Service is restored
    restoreServerNormalBehavior()

    // Create original message
    val creation = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Original message after service restored",
      listOf(createTestAttachment("original.jpg"))
    )
    val message = creation.complete()

    // Wait for processing
    env.awaitAll()

    // Clear any messages from the first operation
    answerService.answers.clear()

    // Configure duplicate response for the original message
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

    // Send a duplicate message after service is restored
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Duplicate message after service restored",
      listOf(createTestAttachment("duplicate.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Verify that the bot detected the duplicate after service restoration
    answerService.verify {
      // Should have at least one message
      messageCount(1)

      // Message should be about a repost
      messagesContain("made repost")

      // Message should reference the original message ID
      messagesContain(message.id)
    }
  }
}