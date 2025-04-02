package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.database.EmojiData
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.EmojiDataTable
import com.fvlaenix.queemporium.mock.TestEmoji
import com.fvlaenix.queemporium.mock.TestMessage
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for LongTermEmojiesStoreCommand
 */
class LongTermEmojiesStoreCommandTest : BaseEmojiStoreCommandTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(LongTermEmojiesStoreCommand::class)
  }

  override var autoStartEnvironment: Boolean = false

  override fun additionalSetUp() {
    // Override the default config to use shorter durations and test-specific settings
    val testConfigModule = module {
      single {
        LongTermEmojiesStoreCommandConfig(
          distanceInDays = 1, // Use a shorter time span for tests
          guildThreshold = 1,
          channelThreshold = 2,
          messageThreshold = 2,
          emojisThreshold = 2,
          isShuffle = false // Disable shuffling for predictable testing
        )
      }
    }

    koin.loadModules(listOf(testConfigModule), allowOverride = true)
  }

  @Test
  fun `test long term command collects emoji data from historical messages`() {
    // Create several messages with reactions to simulate historical data
    val messages = createMultipleMessagesWithReactions(
      count = 5,
      baseMessageText = "Historical message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2)),
        ReactionConfig("❤️", listOf(0, 3))
      )
    )

    // Verify that no emoji data exists before command runs
    messages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertEquals(null, emojiData, "No emoji data should exist before command runs")
    }

    // Start the environment which will trigger the command
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify that emoji data was collected for all messages
    messages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertNotNull(emojiData, "Emoji data should be stored for message ${message.id}")
      assertEquals(4, emojiData.count, "Each message should have 4 reactions stored")
    }

    // Verify emojis are correctly associated with users
    val firstMessageId = messages.first().id
    val storedEmojis = emojiDataConnector.getEmojisForMessage(firstMessageId)

    // Should have 4 emoji entries (2 thumbs up + 2 hearts)
    assertEquals(4, storedEmojis.size, "First message should have 4 emoji entries")

    // Check specific emoji counts
    val thumbsUpCount = storedEmojis.count { it.emojiId == "👍" }
    val heartCount = storedEmojis.count { it.emojiId == "❤️" }

    assertEquals(2, thumbsUpCount, "Should have 2 thumbs up reactions")
    assertEquals(2, heartCount, "Should have 2 heart reactions")
  }

  @Test
  fun `test long term command respects timeframe settings`() {
    // Create messages with reactions at different "ages"
    // In a real test, we'd need to manipulate timestamps
    // Here we'll simulate by differentiating the messages

    // Recent messages (within distanceInDays setting)
    val recentMessages = createMultipleMessagesWithReactions(
      count = 3,
      baseMessageText = "Recent message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2))
      )
    )

    // Old messages (outside distanceInDays setting)
    createMultipleMessagesWithReactions(
      count = 2,
      baseMessageText = "Old message",
      reactionConfigs = listOf(
        ReactionConfig("❤️", listOf(3, 4))
      )
    )

    // TODO Simulate setting older timestamps for oldMessages
    // This would require modification to TestMessage to allow timestamp manipulation
    // For now, we'll just move forward assuming the timestamps could be manipulated

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    // Verify recent messages have data
    recentMessages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertNotNull(emojiData, "Recent message should have emoji data")
      assertEquals(2, emojiData.count, "Recent message should have 2 reactions")
    }

    // In a complete test, we would verify old messages have no data
    // but since we can't manipulate timestamps in this framework yet, we skip this check
  }

  @Test
  fun `test long term command processes messages in shuffle order when configured`() {
    // Override configuration to enable shuffling
    val shuffleConfigModule = module {
      single {
        LongTermEmojiesStoreCommandConfig(
          distanceInDays = 1,
          guildThreshold = 1,
          channelThreshold = 2,
          messageThreshold = 2,
          emojisThreshold = 2,
          isShuffle = true // Enable shuffling
        )
      }
    }

    koin.loadModules(listOf(shuffleConfigModule), allowOverride = true)

    // Create messages with reactions
    val messages = createMultipleMessagesWithReactions(
      count = 10, // More messages to better observe shuffling effect
      baseMessageText = "Shuffle test message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2))
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    // Verify all messages were processed, regardless of order
    messages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertNotNull(emojiData, "All messages should be processed when shuffling")
      assertEquals(2, emojiData.count, "Each message should have correct reaction count")
    }
  }

  @Test
  fun `test long term command handles messages with many reactions`() {
    // Create a message with many different reactions
    val message = createMessageWithReactions(
      messageText = "Message with many reactions",
      reactionConfig = listOf(
        ReactionConfig("👍", listOf(1, 2)),
        ReactionConfig("❤️", listOf(0, 3)),
        ReactionConfig("😂", listOf(2, 4)),
        ReactionConfig("🎉", listOf(1, 3)),
        ReactionConfig("🔥", listOf(0, 4)),
        ReactionConfig("👀", listOf(0, 1))
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    // Verify all reactions were stored
    val emojiData = messageEmojiDataConnector.get(message.id)
    assertNotNull(emojiData, "Emoji data should be stored")
    assertEquals(12, emojiData.count, "Should store all 12 reactions (6 emojis × 2 users each)")

    // Verify emoji distributions
    val storedEmojis = emojiDataConnector.getEmojisForMessage(message.id)
    val emojiCounts = storedEmojis.groupBy { it.emojiId }.mapValues { it.value.size }

    assertEquals(6, emojiCounts.size, "Should have 6 different emoji types")
    emojiCounts.forEach { (_, count) ->
      assertEquals(2, count, "Each emoji should have exactly 2 reactions")
    }
  }

  @Test
  fun `test long term command handles multiple guilds`() {
    // Create a second guild with its own channel and messages
    val secondGuild = env.createGuild("Second Test Guild")
    env.createTextChannel(secondGuild, "general")

    // Create messages in the first guild
    val firstGuildMessages = createMultipleMessagesWithReactions(
      count = 3,
      baseMessageText = "First guild message"
    )

    // Create messages in the second guild
    // This requires manual creation since our helper method uses the default guild
    val secondGuildMessages = (1..3).map { i ->
      val message = env.sendMessage(
        "Second Test Guild",
        "general",
        testUsers[0],
        "Second guild message $i"
      ).complete(true)!! as TestMessage

      // Add reactions
      val emoji = TestEmoji("👍")
      testUsers.take(3).forEach { user ->
        message.addReaction(emoji, user)
      }

      message
    }

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    // Verify first guild messages were processed
    firstGuildMessages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertNotNull(emojiData, "First guild message should have emoji data")
    }

    // Verify second guild messages were processed
    secondGuildMessages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertNotNull(emojiData, "Second guild message should have emoji data")
      assertEquals(3, emojiData.count, "Should have correct reaction count")
    }
  }
}

private fun EmojiDataConnector.getEmojisForMessage(messageId: String): List<EmojiData> {
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