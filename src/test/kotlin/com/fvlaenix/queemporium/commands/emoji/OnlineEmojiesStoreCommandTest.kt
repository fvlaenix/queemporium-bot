package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.features.FeatureKeys
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

/**
 * Tests for OnlineEmojiesStoreCommand
 */
class OnlineEmojiesStoreCommandTest : BaseEmojiStoreCommandTest() {

  override fun getFeatureKeysForTest(): Array<String> {
    return arrayOf(FeatureKeys.ONLINE_EMOJI)
  }

  override var autoStartEnvironment: Boolean = false

  @Test
  fun `test command stores emojis from new message with reactions`() {
    // Create a message with multiple reactions
    val message = createMessageWithReactions(
      messageText = "Message with multiple reactions for testing",
      reactionConfig = listOf(
        ReactionConfig("👍", listOf(1, 2, 3)), // 3 users put a thumbs up
        ReactionConfig("❤️", listOf(2, 4))     // 2 users put a heart
      )
    )

    runWithScenario {
      reactions.expectCount(message, 0)
    }

    // Start the environment which activates the command
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    runWithScenario {
      reactions.awaitProcessing("online emoji store command finished")
      reactions.expectPersisted(message) {
        count(5) // 3 👍 + 2 ❤️
        contains("👍")
        contains("❤️")
      }
    }
  }

  @Test
  fun `test command stores emojis from multiple messages`() {
    // Create multiple messages with reactions
    val messages = createMultipleMessagesWithReactions(
      count = 3,
      baseMessageText = "Multi-message test",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2, 3)),
        ReactionConfig("🎉", listOf(1, 3, 4))
      )
    )

    // Start the environment which activates the command
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    runWithScenario {
      messages.forEach { message ->
        reactions.expectCount(message, 6)
      }

      val firstMessage = messages.first()
      reactions.expectPersisted(firstMessage) {
        count(6)
        containsForUser(testUsers[1].id, "👍")
        containsForUser(testUsers[1].id, "🎉")
      }
    }
  }

  @Test
  @Ignore("We don't have lookback period for now")
  fun `test command ignores messages older than specified timeframe`() {
    // Create a message that would be considered "old"
    // In an actual test, we'd need to manipulate time or message creation date
    val oldMessage = createMessageWithReactions(
      messageText = "Old message outside timeframe",
      reactionConfig = listOf(
        ReactionConfig("👍", listOf(1, 2, 3))
      )
    )

    // TODO Manually set the message time to be outside the lookback period
    // This would require extending TestMessage to allow setting the creation time

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    // Verify the old message wasn't processed
    runWithScenario {
      reactions.expectCount(oldMessage, 0)
    }
  }

  @Test
  fun `test command handles different emoji types`() {
    // Create a message with various emoji types
    val message = createMessageWithReactions(
      messageText = "Message with different emoji types",
      reactionConfig = listOf(
        ReactionConfig("👍", listOf(1, 2)),      // Standard emoji
        ReactionConfig("🔥", listOf(3, 4)),      // Another standard emoji
        ReactionConfig("custom_emoji", listOf(1, 3)) // Custom emoji (would need proper implementation)
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    runWithScenario {
      reactions.expectPersisted(message) {
        count(6)
        contains("👍")
        contains("🔥")
        contains("custom_emoji")
      }
    }
  }
}
