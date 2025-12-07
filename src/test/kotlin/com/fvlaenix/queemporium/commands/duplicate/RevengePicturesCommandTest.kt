package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.verification.verify
import kotlin.test.Ignore
import kotlin.test.Test

class RevengePicturesCommandTest : BaseDuplicateCommandTest() {

  override fun getFeaturesForTest(): Array<String> {
    return arrayOf(FeatureKeys.MESSAGES_STORE, FeatureKeys.REVENGE_PICTURES)
  }

  override var autoStartEnvironment: Boolean = false

  @Test
  @Ignore("FIXME: synchronization between close by time images in revenge thing")
  fun `test revenge command finds duplicates in existing messages`() {
    // Setup: create an "original" message but don't trigger events yet
    val originalMessage = sendMessageWithImage(
      messageText = "Original message with image",
      fileName = "original.jpg"
    )

    // Configure the duplicate service to detect a duplicate
    mockDuplicateService.setResponseForFile(
      "duplicate.jpg", listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = "original.jpg",
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 95
        )
      )
    )

    // Send a second message that will be a duplicate
    sendMessageWithImage(
      messageText = "Duplicate message with image",
      fileName = "duplicate.jpg"
    )

    // Start the environment to activate the command
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify that a duplicate was detected
    answerService.verify {
      messageCount(1)
      lastMessageContains("made repost")
      lastMessageContains(originalMessage.id)
    }
  }

  @Test
  fun `test revenge command ignores excluded channels`() {
    // Add generalChannel to excluded list
    guildInfoConnector.addExcludingChannel(testGuild.id, generalChannel.id)

    // Setup: create an "original" message
    val originalMessage = sendMessageWithImage(
      messageText = "Original message with image",
      fileName = "original.jpg"
    )

    // Configure the service to detect duplicates
    mockDuplicateService.setResponseForFile(
      "duplicate.jpg", listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = "original.jpg",
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 95
        )
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // No duplicate messages should be reported since the channel is excluded
    answerService.verify {
      messageCount(0)
    }
  }

  @Test
  @Ignore("FIXME: synchronization between close by time images in revenge thing")
  fun `test revenge command processes multiple images in message`() {
    // Setup: create two "original" messages
    val originalMessage1 = sendMessageWithImage(
      messageText = "First original message",
      fileName = "original1.jpg"
    )

    val originalMessage2 = sendMessageWithImage(
      messageText = "Second original message",
      fileName = "original2.jpg"
    )

    // Create a message with multiple images
    sendMessageWithMultipleImages(
      messageText = "Message with multiple images",
      fileConfigs = listOf(
        ImageConfig("test1.jpg"),
        ImageConfig("test2.jpg")
      )
    )

    // Configure the service to detect duplicates
    mockDuplicateService.setResponseForFile(
      "test1.jpg", listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage1.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = "original1.jpg",
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 90
        )
      )
    )

    mockDuplicateService.setResponseForFile(
      "test2.jpg", listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage2.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = "original2.jpg",
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 85
        )
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Two duplicates should be detected
    answerService.verify {
      // One message for each duplicate
      messageCount(2)
      messagesContain(originalMessage1.id)
      messagesContain(originalMessage2.id)
    }
  }

  @Test
  @Ignore("FIXME: synchronization between close by time images in revenge thing")
  fun `test revenge command processes different similarity levels`() {
    // Setup: create an "original" message
    val originalMessage = sendMessageWithImage(
      messageText = "Original message",
      fileName = "original.jpg"
    )

    // Setup: create a "similar" message with high similarity
    sendMessageWithImage(
      messageText = "High similarity message",
      fileName = "high_similarity.jpg"
    )

    // Setup: create a "similar" message with low similarity
    sendMessageWithImage(
      messageText = "Low similarity message",
      fileName = "low_similarity.jpg"
    )

    // Configure the service to detect duplicates with different similarity levels
    mockDuplicateService.setResponseForFile(
      "high_similarity.jpg", listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = "original.jpg",
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 95 // High similarity
        )
      )
    )

    mockDuplicateService.setResponseForFile(
      "low_similarity.jpg", listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = "original.jpg",
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 65 // Low similarity
        )
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Both duplicates should be detected
    answerService.verify {
      // One for each duplicate
      messageCount(2)
    }
  }
}