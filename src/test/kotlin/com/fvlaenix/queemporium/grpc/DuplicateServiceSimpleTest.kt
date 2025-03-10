package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.duplicate.protobuf.getCompressionSizeRequest
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.verification.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Simple test to demonstrate the use of BaseGrpcTest for testing bot's interaction with gRPC services.
 */
class DuplicateServiceSimpleTest : BaseGrpcTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(OnlinePictureCompare::class)
  }

  @Test
  @Ignore("Currently bot doesn't support backward messages")
  fun `test bot handles duplicate detection service unavailability`() {
    // 1. Set up mocked services
    val answerService = MockAnswerService()

    // Override the answer service
    koin.loadModules(listOf(module {
      single { answerService }
    }))

    // 2. Configure test gRPC service
    duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
      .setX(800)
      .build()

    // 3. Create test environment
    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general")
        withChannel("duplicate-channel")
      }
    }

    // 4. Set up duplicate channel configuration
    val guildInfoConnector = getGuildInfoConnector()
    val testGuild = env.jda.getGuildsByName("Test Guild", false).first()
    val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()
    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    // 5. Simulate server unavailability
    simulateServerUnavailable()

    // 6. Create a user and send a test message with image
    val user = env.createUser("Test User", false)
    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Test message with image",
      listOf(createTestAttachment("test_image.jpg"))
    )

    // 7. Wait for processing to complete
    env.awaitAll()

    // 8. Verify the bot properly handled the unavailable service
    answerService.verify {
      messagesContain("unavailable")
    }
  }

  @Test
  fun `test can communicate with gRPC service directly`() = runBlocking {
    // 1. Configure test service response
    duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
      .setX(800)
      .build()

    // 2. We can test the gRPC service directly using the client
    val response = duplicateImagesClient.getImageCompressionSize(getCompressionSizeRequest {})

    // 3. Assert the test service returns the expected value
    assertEquals(800, response.x)

    // 4. Verify that the service received our request
    assertTrue(duplicateService.requests.isNotEmpty())
  }
}