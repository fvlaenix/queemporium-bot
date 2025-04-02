package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.mock.TestEmoji
import com.fvlaenix.queemporium.mock.TestMessage
import net.dv8tion.jda.api.entities.Message
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
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

  @Test
  fun `test long term command collects emoji data from historical messages`() {
    // Create several messages with reactions to simulate historical data
    val now = OffsetDateTime.now()
    val messages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 5,
      baseMessageText = "Historical message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2)),
        ReactionConfig("❤️", listOf(0, 3))
      ),
      timestamps = (1..5).map { daysAgo -> now.minus(daysAgo.toLong(), ChronoUnit.DAYS) }
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
    // Create messages with reactions at different timestamps
    val now = OffsetDateTime.now()

    val recentMessages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 3,
      baseMessageText = "Recent message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2))
      ),
      timestamps = (0..2).map { hoursAgo -> now.minus(hoursAgo.toLong(), ChronoUnit.HOURS) }
    )

    val oldMessages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 2,
      baseMessageText = "Old message",
      reactionConfigs = listOf(
        ReactionConfig("❤️", listOf(3, 4))
      ),
      timestamps = (2..3).map { daysAgo -> now.minus(daysAgo.toLong(), ChronoUnit.MONTHS) }
    )

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

    // Verify old messages are not processed due to time constraint
    oldMessages.forEach { message ->
      val emojiData = messageEmojiDataConnector.get(message.id)
      assertEquals(null, emojiData, "Old messages outside timeframe should not be processed")
    }
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

    // Create messages with reactions within the time window
    val now = OffsetDateTime.now()
    val messages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 10, // More messages to better observe shuffling effect
      baseMessageText = "Shuffle test message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2))
      ),
      timestamps = (0..9).map { hoursAgo -> now.minus(hoursAgo.toLong(), ChronoUnit.HOURS) }
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
    // Create a message with many different reactions, within time window
    val now = OffsetDateTime.now()
    val message = createMessageWithReactionsAndTimeStamp(
      messageText = "Message with many reactions",
      reactionConfig = listOf(
        ReactionConfig("👍", listOf(1, 2)),
        ReactionConfig("❤️", listOf(0, 3)),
        ReactionConfig("😂", listOf(2, 4)),
        ReactionConfig("🎉", listOf(1, 3)),
        ReactionConfig("🔥", listOf(0, 4)),
        ReactionConfig("👀", listOf(0, 1))
      ),
      timestamp = now.minus(2, ChronoUnit.HOURS)
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

    val now = OffsetDateTime.now()

    // Create messages in the first guild with timestamps
    val firstGuildMessages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 3,
      baseMessageText = "First guild message",
      timestamps = (1..3).map { hoursAgo -> now.minus(hoursAgo.toLong(), ChronoUnit.HOURS) }
    )

    // Create messages in the second guild
    val secondGuildUsers = (1..3).map { i ->
      env.createUser("SecondGuildUser$i", false)
    }

    // This requires manual creation since our helper method uses the default guild
    val secondGuildMessages = (1..3).map { i ->
      val message = env.sendMessage(
        "Second Test Guild",
        "general",
        secondGuildUsers[0],
        "Second guild message $i",
        emptyList(),
        now.minus(i.toLong(), ChronoUnit.HOURS)
      ).complete(true)!!

      // Add reactions
      val emoji = TestEmoji("👍")
      secondGuildUsers.forEach { user ->
        (message as TestMessage).addReaction(emoji, user)
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

  /**
   * Creates a message with reactions at a specific timestamp
   */
  private fun createMessageWithReactionsAndTimeStamp(
    channelName: String = defaultGeneralChannelName,
    messageText: String = "Test message with reactions",
    reactionConfig: List<ReactionConfig> = emptyList(),
    timestamp: OffsetDateTime
  ): Message {
    // Send the message with specific timestamp
    val message = env.sendMessage(
      defaultGuildName,
      channelName,
      testUsers[0], // Author is the first test user
      messageText,
      emptyList(),
      timestamp
    ).complete(true)!! as TestMessage

    // Add reactions according to configuration
    reactionConfig.forEach { config ->
      val emoji = TestEmoji(config.emojiName)
      config.userIndices.forEach { userIndex ->
        if (userIndex >= 0 && userIndex < testUsers.size) {
          message.addReaction(emoji, testUsers[userIndex])
        }
      }
    }

    // Wait for processing to complete
    env.awaitAll()

    return message
  }

  /**
   * Creates multiple messages with reactions and specific timestamps
   */
  private fun createMultipleMessagesWithReactionsAndTimeStamps(
    count: Int = 5,
    channelName: String = defaultGeneralChannelName,
    baseMessageText: String = "Test message",
    reactionConfigs: List<ReactionConfig> = listOf(
      ReactionConfig("👍", listOf(1, 2)), // Basic default reactions
      ReactionConfig("❤️", listOf(3, 4))
    ),
    timestamps: List<OffsetDateTime>
  ): List<Message> {
    require(timestamps.size >= count) { "Must provide at least $count timestamps" }

    return (0 until count).map { i ->
      createMessageWithReactionsAndTimeStamp(
        channelName = channelName,
        messageText = "$baseMessageText $i",
        reactionConfig = reactionConfigs,
        timestamp = timestamps[i]
      )
    }
  }
}