package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.duplicate.protobuf.addImageResponse
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.testing.dsl.ChannelResolver
import com.fvlaenix.queemporium.testing.dsl.GuildResolver
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Test for verifying the bot properly handles error responses from DuplicateImagesService.
 */
class DuplicateServiceErrorResponseTest : BaseGrpcTest() {

  override fun getFeaturesForTest(): Array<String> {
    return arrayOf(FeatureKeys.ONLINE_COMPARE, FeatureKeys.MESSAGES_STORE)
  }

  @Test
  fun `test bot handles error response from service`() {
    // Configure the compression size response
    duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
      .setX(800)
      .build()

    // Configure error response for add image
    duplicateService.addImageResponse = addImageResponse {
      this.error = "Service error: Failed to process image"
    }

    // Create test environment
    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general")
        withChannel("duplicate-channel")
      }
    }

    // Configure duplicate detection channel
    val guildInfoConnector = getGuildInfoConnector()
    val testGuild = GuildResolver.resolve(env.jda, "Test Guild")
    val duplicateChannel = ChannelResolver.resolve(testGuild, "duplicate-channel")
    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    // Create a user
    val user = env.createUser("Test User", false)

    // Send a message with an image which will trigger error response
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message that should trigger an error response",
      listOf(createTestAttachment("error_trigger.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Verify the service received a request
    assertTrue(duplicateService.requests.isNotEmpty(), "Service should have received requests")

    // Verify the bot handled the error correctly
    answerService.verify {
      // No "made repost" messages should be present
      assertTrue(
        answerService.answers.none { it.text.contains("made repost") },
        "Should not detect duplicates from error response"
      )
    }
  }

  @Test
  fun `test bot recovers after service error`() {
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
    val testGuild = GuildResolver.resolve(env.jda, "Test Guild")
    val duplicateChannel = ChannelResolver.resolve(testGuild, "duplicate-channel")
    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    // Create a user
    val user = env.createUser("Test User", false)

    // Configure error response
    duplicateService.addImageResponse = addImageResponse {
      this.error = "Service error: Failed to process image"
    }

    // Send first message with an image
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message during service error",
      listOf(createTestAttachment("error_test.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Clear any messages from first operation
    answerService.answers.clear()

    // Reset service requests count
    val initialRequestsCount = duplicateService.requests.size

    // Configure successful response
    duplicateService.addImageResponse = addImageResponse {
      this.responseOk = com.fvlaenix.duplicate.protobuf.AddImageResponseOk.getDefaultInstance()
    }

    // Send second message after error recovery
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message after service recovery",
      listOf(createTestAttachment("recovery_test.jpg"))
    )

    // Wait for processing
    env.awaitAll()

    // Verify that the service processed the second request
    assertTrue(
      duplicateService.requests.size > initialRequestsCount,
      "Service should process requests after recovery"
    )

    // Verify there are no error messages
    answerService.verify {
      messageCount(0) // No error messages should be sent for the second request
    }
  }
}
