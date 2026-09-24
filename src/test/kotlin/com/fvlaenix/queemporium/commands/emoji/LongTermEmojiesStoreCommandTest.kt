package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.database.MessageEmojiData
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.mock.TestEmoji
import com.fvlaenix.queemporium.mock.TestMessage
import net.dv8tion.jda.api.entities.Message
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.koin.dsl.module
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for LongTermEmojiesStoreCommand
 */
class LongTermEmojiesStoreCommandTest : BaseEmojiStoreCommandTest() {

  override fun getFeatureKeysForTest(): Array<String> {
    return arrayOf(FeatureKeys.MESSAGES_STORE, FeatureKeys.LONG_TERM_EMOJI)
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
    runWithScenario {
      messages.forEach { message ->
        reactions.expectCount(message, 0)
      }
    }

    // Start the environment which will trigger the command
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify that emoji data was collected for all messages
    runWithScenario {
      messages.forEach { message ->
        reactions.expectCount(message, 4)
      }

      val firstMessage = messages.first()
      reactions.expectPersisted(firstMessage) {
        count(4) // 2 thumbs up + 2 hearts
        contains("👍")
        contains("❤️")
      }
    }
  }

  @Test
  fun `test long term command respects timeframe settings`() {
    // Create messages with reactions at different timestamps
    val now = OffsetDateTime.now()

    val oldMessages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 2,
      baseMessageText = "Old message",
      reactionConfigs = listOf(
        ReactionConfig("❤️", listOf(3, 4))
      ),
      timestamps = (2..3).map { daysAgo -> now.minus(daysAgo.toLong(), ChronoUnit.MONTHS) }
    )

    // The test history returns messages in reverse insertion order, like Discord's newest-first history.
    val recentMessages = createMultipleMessagesWithReactionsAndTimeStamps(
      count = 3,
      baseMessageText = "Recent message",
      reactionConfigs = listOf(
        ReactionConfig("👍", listOf(1, 2))
      ),
      timestamps = (0..2).map { hoursAgo -> now.minus(hoursAgo.toLong(), ChronoUnit.HOURS) }
    )

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    runWithScenario {
      // Verify recent messages have data
      recentMessages.forEach { message ->
        reactions.expectCount(message, 2)
      }

      // Verify old messages are not processed due to time constraint
      oldMessages.forEach { message ->
        reactions.expectCount(message, 0)
      }
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

    runWithScenario {
      koin.loadModules(listOf(shuffleConfigModule), allowOverride = true)
    }

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

    runWithScenario {
      // Verify all messages were processed, regardless of order
      messages.forEach { message ->
        reactions.expectCount(message, 2)
      }
    }
  }

  @Test
  @Timeout(60)
  fun `test shuffled scan advances past full batch and stops at age cutoff`() {
    val now = OffsetDateTime.now()
    val oldMessage = env.sendMessage(
      defaultGuildName, defaultGeneralChannelName, testUsers[0], "Outside scan window",
      emptyList(), now.minusDays(31)
    ).complete(true)!!
    val oldestRecent = env.sendMessage(
      defaultGuildName, defaultGeneralChannelName, testUsers[0], "Beyond first batch",
      emptyList(), now.minusHours(1)
    ).complete(true)!! as TestMessage
    oldestRecent.addReaction(TestEmoji("👍"), testUsers[1])

    var newest: Message? = null
    repeat(500) { index ->
      newest = env.sendMessage(
        defaultGuildName, defaultGeneralChannelName, testUsers[0], "Recent message $index",
        emptyList(), now.minusMinutes(30)
      ).complete(true)
    }
    messageEmojiDataConnector.insert(MessageEmojiData(requireNotNull(newest).id, 0))

    startEnvironment()

    assertEquals(0, messageEmojiDataConnector.get(requireNotNull(newest).id)?.count)
    assertEquals(1, messageEmojiDataConnector.get(oldestRecent.id)?.count)
    assertNull(messageEmojiDataConnector.get(oldMessage.id))
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

    runWithScenario {
      // Verify all reactions were stored
      reactions.expectPersisted(message) {
        count(12) // 6 emojis × 2 users
        contains("👍")
        contains("❤️")
        contains("😂")
        contains("🎉")
        contains("🔥")
        contains("👀")
      }
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

    runWithScenario {
      // Verify first guild messages were processed
      firstGuildMessages.forEach { message ->
        reactions.expectCount(message, 4)
      }

      // Verify second guild messages were processed
      secondGuildMessages.forEach { message ->
        reactions.expectCount(message, 3)
      }
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
