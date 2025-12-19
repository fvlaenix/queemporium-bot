package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test

class OnlinePictureCompareTest : BaseDuplicateCommandTest() {

  override fun getFeaturesForTest(): Array<String> {
    return arrayOf(FeatureKeys.ONLINE_COMPARE, FeatureKeys.MESSAGES_STORE)
  }

  @Test
  fun `test duplicate detection with message chain`() {
    // Create a sequence - first an original message, then a duplicate referencing it
    val (originalMessage, _) = createMessageChain(
      originalText = "This is the original message with some content",
      originalFileName = "original.jpg",
      duplicateText = "This is a duplicate of the original",
      duplicateFileName = "duplicate.jpg"
    )

    // The duplicate message should trigger the bot to send a notification
    // Note: The createMessageChain method waits for all processing to complete
    env.awaitAll()

    // Verify that the bot detected and reported the duplicate
    answerService.verify {
      // There should be at least one message about a duplicate
      messageCount(1)

      // Message should contain repost notification
      lastMessageContains("made repost")

      // Message should reference the original message ID
      lastMessageContains(originalMessage.id)
    }
  }

  @Test
  fun `test multiple duplicates in one message`() {
    // First create two original messages
    val originalMessage1 = sendMessageWithImage(
      messageText = "First original message",
      fileName = "original1.jpg"
    )

    val originalMessage2 = sendMessageWithImage(
      messageText = "Second original message",
      fileName = "original2.jpg"
    )

    // Wait for processing
    env.awaitAll()

    // Configure mock to recognize duplicates of both originals
    duplicates.stubResponse("duplicate1.jpg") {
      duplicate(
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
    }

    duplicates.stubResponse("duplicate2.jpg") {
      duplicate(
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
    }

    // Send message with multiple duplicate images
    sendMessageWithMultipleImages(
      messageText = "This message contains duplicates",
      fileConfigs = listOf(
        ImageConfig("duplicate1.jpg"),
        ImageConfig("duplicate2.jpg")
      )
    )

    // Wait for processing
    env.awaitAll()

    // Verify results
    answerService.verify {
      // Should be at least 2 messages (one for each duplicate)
      messageCount(2)

      // Should mention both original message IDs
      messagesContain(originalMessage1.id)
      messagesContain(originalMessage2.id)
    }
  }
}
