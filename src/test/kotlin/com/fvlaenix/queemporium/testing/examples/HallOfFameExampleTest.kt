package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.helpers.hallOfFameContext
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

class HallOfFameExampleTest : BaseKoinTest() {

  @Test
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
        lastMessageContains("reactions")
      }
    }

    // Verify at least one message was sent
    assert(answerService.answers.isNotEmpty()) { "Expected at least one bot message" }
  }

  @Test
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

    val hofContext = envWithTime.hallOfFameContext(
      hallOfFameConnector = hallOfFameConnector,
      emojiDataConnector = emojiDataConnector,
      answerService = answerService
    )

    // Configure with lower threshold
    hofContext.configureHallOfFame("my-guild", "hall-of-fame", threshold = 3)

    // Seed reactions for first message
    hofContext.seedMessageToCount("my-guild", "general", messageIndex = 0, count = 3)

    // Seed reactions for second message
    hofContext.seedMessageToCount("my-guild", "general", messageIndex = 1, count = 4)

    // Trigger both jobs
    hofContext.triggerBothJobs()

    // Wait a bit and trigger send again to get second message
    hofContext.triggerSendJob()

    envWithTime.runScenario(answerService) {
      // Verify multiple messages were sent
      expect("should send multiple hall of fame messages") {
        messageSentCount(2) // At least 2 (could be more with forwards)
      }
    }
  }

  @Test
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

    val hofContext = envWithTime.hallOfFameContext(
      hallOfFameConnector = hallOfFameConnector,
      emojiDataConnector = emojiDataConnector,
      answerService = answerService
    )

    hofContext.configureHallOfFame("emoji-guild", "hall-of-fame", threshold = 3)

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
    hofContext.triggerBothJobs()

    envWithTime.runScenario(answerService) {
      expect("should send hall of fame message") {
        messageSentCount(1)
      }
    }
  }
}
