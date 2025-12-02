package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.commands.advent.AdventDataConnector
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.helpers.adventContext
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AdventExampleTest : BaseKoinTest() {

  @Test
  fun `test advent calendar with single entry reveal`() = runBlocking {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1 surprise!")
        }
        channel("advent-reveals")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val adventDataConnector = AdventDataConnector(databaseConfig.toDatabase())
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())

    val adventContext = envWithTime.adventContext(
      adventDataConnector = adventDataConnector,
      answerService = answerService
    )

    // Get the message ID from the first message
    val guild = envWithTime.environment.jda.getGuildsByName("test-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages
    val messageId = messages[0].id

    // Schedule a single advent entry for reveal in 1 day
    val revealTime = startTime.plusMillis(1.days.inWholeMilliseconds)
    adventContext.scheduleEntries(
      guildId = "test-guild",
      postChannelId = "advent-reveals",
      entries = listOf(
        Triple(messageId, "🎄 Day 1 of Advent!", revealTime)
      )
    )

    // Advance to the reveal time
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      // Verify message was revealed
      expect("should reveal advent message") {
        lastMessageContains("Day 1 of Advent!")
      }
    }

    // Verify the entry is marked as revealed
    assertEquals(1, adventContext.getRevealedCount())
    assertEquals(0, adventContext.getUnrevealedCount())
  }

  @Test
  fun `test advent calendar with multiple entries`() = runBlocking {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")
      user("bob")

      guild("calendar-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1 content")
          message(author = "bob", text = "Day 2 content")
          message(author = "alice", text = "Day 3 content")
        }
        channel("advent-reveals")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val adventDataConnector = AdventDataConnector(databaseConfig.toDatabase())

    val adventContext = envWithTime.adventContext(
      adventDataConnector = adventDataConnector,
      answerService = answerService
    )

    // Get message IDs
    val guild = envWithTime.environment.jda.getGuildsByName("calendar-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages

    // Schedule multiple entries with 1 day intervals
    adventContext.scheduleMultipleEntries(
      guildId = "calendar-guild",
      postChannelId = "advent-reveals",
      entries = listOf(
        messages[0].id to "🎄 Day 1!",
        messages[1].id to "🎁 Day 2!",
        messages[2].id to "⭐ Day 3!"
      ),
      startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
      interval = 1.days
    )

    // Initially nothing revealed
    assertEquals(0, adventContext.getRevealedCount())
    assertEquals(3, adventContext.getUnrevealedCount())

    // Advance to first reveal
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      expect("should reveal first entry") {
        lastMessageContains("Day 1!")
      }
    }

    assertEquals(1, adventContext.getRevealedCount())
    assertEquals(2, adventContext.getUnrevealedCount())

    // Advance to second reveal
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      expect("should reveal second entry") {
        lastMessageContains("Day 2!")
      }
    }

    assertEquals(2, adventContext.getRevealedCount())
    assertEquals(1, adventContext.getUnrevealedCount())

    // Advance to third reveal
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      expect("should reveal third entry") {
        lastMessageContains("Day 3!")
      }

      assertEquals(3, adventContext.getRevealedCount())
      assertEquals(0, adventContext.getUnrevealedCount())
    }
  }

  @Test
  fun `test advent calendar reveal all at once`() = runBlocking {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("fast-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1")
          message(author = "alice", text = "Day 2")
        }
        channel("advent-reveals")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val adventDataConnector = AdventDataConnector(databaseConfig.toDatabase())

    val adventContext = envWithTime.adventContext(
      adventDataConnector = adventDataConnector,
      answerService = answerService
    )

    // Get message IDs
    val guild = envWithTime.environment.jda.getGuildsByName("fast-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages

    // Schedule entries
    adventContext.scheduleMultipleEntries(
      guildId = "fast-guild",
      postChannelId = "advent-reveals",
      entries = listOf(
        messages[0].id to "Day 1",
        messages[1].id to "Day 2"
      ),
      startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
      interval = 1.hours
    )

    // Reveal all at once
    adventContext.revealAllEntries()

    // Verify all revealed
    assertEquals(2, adventContext.getRevealedCount())
    assertEquals(0, adventContext.getUnrevealedCount())

    envWithTime.runScenario(answerService) {
      expect("should reveal all entries") {
        messageSentCount(4) // 2 description messages + 2 forwards
      }
    }
  }

  @Test
  fun `test advent calendar with past reveal time`() = runBlocking {
    val startTime = Instant.parse("2024-12-10T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("past-guild") {
        channel("general") {
          message(author = "alice", text = "Past content")
        }
        channel("advent-reveals")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    val adventDataConnector = AdventDataConnector(databaseConfig.toDatabase())

    val adventContext = envWithTime.adventContext(
      adventDataConnector = adventDataConnector,
      answerService = answerService
    )

    // Get message ID
    val guild = envWithTime.environment.jda.getGuildsByName("past-guild", true).first()
    val channel = guild.getTextChannelsByName("general", true).first()
    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages

    // Schedule entry in the past (before current time)
    val pastTime = startTime.minusMillis(1.days.inWholeMilliseconds)
    adventContext.scheduleEntries(
      guildId = "past-guild",
      postChannelId = "advent-reveals",
      entries = listOf(
        Triple(messages[0].id, "Late reveal!", pastTime)
      )
    )

    // The advent job should immediately reveal past entries
    // Just advance a tiny bit to let the job run
    adventContext.advanceTime(1.hours)

    envWithTime.runScenario(answerService) {
      expect("should reveal past entry immediately") {
        lastMessageContains("Late reveal!")
      }
    }

    assertEquals(1, adventContext.getRevealedCount())
  }
}
