package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.helpers.hallOfFameContext
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit

class HallOfFameExampleTest : BaseKoinTest() {

  private fun populateMessageData(
    connector: MessageDataConnector,
    messages: List<net.dv8tion.jda.api.entities.Message>,
    guildId: String,
    channelId: String
  ) {
    messages.forEach { msg ->
      connector.add(
        MessageData(
          messageId = msg.id,
          guildId = guildId,
          channelId = channelId,
          text = msg.contentRaw,
          url = msg.jumpUrl,
          authorId = msg.author.id,
          epoch = msg.timeCreated.toEpochSecond() * 1000
        )
      )
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test hall of fame with scenario DSL and helpers`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("alice") { name("Alice") }
      user("bob") { name("Bob") }

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "This is an amazing post!")
        }
        channel("hall-of-fame")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    // Get database connectors from Koin
    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val hallOfFameConnector = HallOfFameConnector(databaseConfig.toDatabase())
    val emojiDataConnector = EmojiDataConnector(databaseConfig.toDatabase())
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())

    // Populate message data
    val guild = envWithTime.environment.jda.getGuildsByName("test-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages
    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

    // Create Hall of Fame context
    val hofContext = envWithTime.hallOfFameContext(
      hallOfFameConnector = hallOfFameConnector,
      emojiDataConnector = emojiDataConnector,
      answerService = answerService
    )

    // Configure Hall of Fame with threshold of 5
    hofContext.configureHallOfFame("test-guild", "hall-of-fame", threshold = 5)

    // Seed 5 emoji reactions on the first message
    hofContext.seedMessageToCount("test-guild", "general", messageIndex = 0, count = 5)

    // Trigger the retrieve job (runs every 9 hours)
    // This updates the database with messages above threshold
    hofContext.triggerRetrieveJob()

    // Trigger the send job (runs every 4 hours)
    // This forwards the message to hall-of-fame channel
    hofContext.triggerSendJob()

    envWithTime.runScenario(answerService) {
      // Verify the message was sent
      expect("should send hall of fame message") {
        val hasMessage = answerService.answers.any { it.text.contains("reactions") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'reactions'")
        }
      }
    }

    // Verify at least one message was sent
    assert(answerService.answers.isNotEmpty()) { "Expected at least one bot message" }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test multiple messages reaching hall of fame`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.HALL_OF_FAME)

      user("alice")
      user("bob")

      guild("my-guild") {
        channel("general") {
          message(author = "alice", text = "First great post!")
          message(author = "bob", text = "Second great post!")
        }
        channel("hall-of-fame")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val hallOfFameConnector = HallOfFameConnector(databaseConfig.toDatabase())
    val emojiDataConnector = EmojiDataConnector(databaseConfig.toDatabase())
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())

    // Populate message data
    val guild = envWithTime.environment.jda.getGuildsByName("my-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages
    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

    val hofContext = envWithTime.hallOfFameContext(
      hallOfFameConnector = hallOfFameConnector,
      emojiDataConnector = emojiDataConnector,
      answerService = answerService
    )

    // Configure with lower threshold
    hofContext.configureHallOfFame(guild.id, "hall-of-fame", threshold = 3)

    // Seed reactions for first message
    hofContext.seedMessageToCount(guild.id, "general", messageIndex = 0, count = 3)

    // Seed reactions for second message
    hofContext.seedMessageToCount(guild.id, "general", messageIndex = 1, count = 4)

    // Advance time to let jobs run
    // Retrieve job runs every 9h, Send job every 4h
    // We need to advance enough to trigger both multiple times if needed
    // 9h (Retrieve) -> 12h (Send 1st) -> 18h (Retrieve) -> 16h (Send 2nd)

    // First Retrieve at 9h
    envWithTime.timeController!!.advanceTime(kotlin.time.Duration.parse("10h"))
    envWithTime.awaitAll()

    // Send at 12h and 16h
    envWithTime.timeController!!.advanceTime(kotlin.time.Duration.parse("8h"))
    envWithTime.awaitAll()

    envWithTime.runScenario(answerService) {
      // Verify multiple messages were sent
      expect("should send multiple hall of fame messages") {
        val count = answerService.answers.size
        if (count < 2) {
          throw AssertionError("Expected at least 2 messages, got $count. Messages: ${answerService.answers}")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test hall of fame with different emojis`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.HALL_OF_FAME)

      user("alice")
      user("bob")
      user("charlie")

      guild("emoji-guild") {
        channel("general") {
          message(author = "alice", text = "Amazing content!")
        }
        channel("hall-of-fame")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val hallOfFameConnector = HallOfFameConnector(databaseConfig.toDatabase())
    val emojiDataConnector = EmojiDataConnector(databaseConfig.toDatabase())
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())

    // Populate message data
    val guild = envWithTime.environment.jda.getGuildsByName("emoji-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages
    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

    val hofContext = envWithTime.hallOfFameContext(
      hallOfFameConnector = hallOfFameConnector,
      emojiDataConnector = emojiDataConnector,
      answerService = answerService
    )

    hofContext.configureHallOfFame(guild.id, "hall-of-fame", threshold = 3)

    // Add different emoji types from different users
    hofContext.seedEmojiReactions(
      guildId = "emoji-guild",
      channelId = "general",
      messageIndex = 0,
      emoji = "⭐",
      userIds = listOf("alice", "bob")
    )

    hofContext.seedEmojiReactions(
      guildId = "emoji-guild",
      channelId = "general",
      messageIndex = 0,
      emoji = "❤️",
      userIds = listOf("charlie")
    )

    // Total: 3 reactions (2 stars + 1 heart)
    // Advance time to let jobs run
    // Retrieve job runs at 9h. Send job runs at 4h, 8h, 12h.
    // We need to advance past 9h so Retrieve runs, and then reach 12h so Send runs.
    envWithTime.timeController!!.advanceTime(kotlin.time.Duration.parse("10h"))
    envWithTime.awaitAll()
    envWithTime.timeController!!.advanceTime(kotlin.time.Duration.parse("4h"))
    envWithTime.awaitAll()

    envWithTime.runScenario(answerService) {
      expect("should send hall of fame message") {
        val hasMessage = answerService.answers.isNotEmpty()
        if (!hasMessage) {
          throw AssertionError("No message sent")
        }
      }
    }
  }
}