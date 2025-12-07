package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.database.EmojiData
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.EmojiDataTable
import com.fvlaenix.queemporium.features.FeatureKeys
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    // Before command execution, there should be no emoji data
    val beforeCount = messageEmojiDataConnector.get(message.id)?.count ?: 0
    assertEquals(
      0, beforeCount,
      "Before command execution, there should be no emoji data stored"
    )

    // Start the environment which activates the command
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    // Check that emoji data was stored
    val emojiData = messageEmojiDataConnector.get(message.id)

    // Should be 5 reactions (3 👍 + 2 ❤️)
    assertNotNull(emojiData, "Emoji data should be stored")
    assertEquals(5, emojiData.count, "Should store all 5 reactions")

    // Check that specific emoji data was stored
    val storedEmojis = emojiDataConnector.getMessagesAboveThreshold(
      guildId = testGuild.id,
      threshold = 1
    )

    assertTrue(
      storedEmojis.contains(message.id),
      "Message should be included in messages with emojis above threshold"
    )
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

    // Verify all messages have emoji data stored
    messages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertNotNull(emojiData, "Emoji data should be stored for message ${message.id}")
      assertEquals(6, emojiData.count, "Each message should have 6 reactions stored")
    }

    // Verify that emoji data for individual users is correctly stored
    val firstMessageId = messages.first().id
    val firstUserEmojis = emojiDataConnector.getEmojisForMessageByUser(firstMessageId, testUsers[1].id)

    assertEquals(2, firstUserEmojis.size, "User should have 2 emojis on the first message")
    assertTrue(firstUserEmojis.any { it.emojiId == "👍" }, "User should have thumbs up reaction")
    assertTrue(firstUserEmojis.any { it.emojiId == "🎉" }, "User should have party reaction")
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
    val oldMessageData = messageEmojiDataConnector.get(oldMessage.id)
    assertEquals(null, oldMessageData, "Old message should not have emoji data stored")
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

    // Verify emoji data was stored
    val emojiData = messageEmojiDataConnector.get(message.id)
    assertNotNull(emojiData, "Emoji data should be stored")
    assertEquals(6, emojiData.count, "Should store all 6 reactions including custom emojis")

    // Verify specific emoji types
    val storedEmojis = emojiDataConnector.getEmojisForMessage(message.id)
    assertTrue(storedEmojis.any { it.emojiId == "👍" }, "Should store standard emoji")
    assertTrue(storedEmojis.any { it.emojiId == "🔥" }, "Should store another standard emoji")
    assertTrue(storedEmojis.any { it.emojiId == "custom_emoji" }, "Should store custom emoji")
  }
}

// Extension functions for testing - these would be added to your project
private fun EmojiDataConnector.getEmojisForMessageByUser(messageId: String, userId: String): List<EmojiData> {
  return transaction(database) {
    EmojiDataTable.select {
      (EmojiDataTable.messageId eq messageId) and (EmojiDataTable.authorId eq userId)
    }.map {
      EmojiData(
        messageId = it[EmojiDataTable.messageId],
        emojiId = it[EmojiDataTable.emojiId],
        authorId = it[EmojiDataTable.authorId]
      )
    }
  }
}

internal fun EmojiDataConnector.getEmojisForMessage(messageId: String): List<EmojiData> {
  return transaction(database) {
    EmojiDataTable.select { EmojiDataTable.messageId eq messageId }
      .map {
        EmojiData(
          messageId = it[EmojiDataTable.messageId],
          emojiId = it[EmojiDataTable.emojiId],
          authorId = it[EmojiDataTable.authorId]
        )
      }
  }
}
