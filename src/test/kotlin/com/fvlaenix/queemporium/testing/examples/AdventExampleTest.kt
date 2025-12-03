package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.commands.advent.AdventDataConnector
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageData
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
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AdventExampleTest : BaseKoinTest() {

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

    println("DEBUG: Setting up fixture with virtual clock at $startTime")
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

    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

    // Schedule a single advent entry for reveal in 1 day
    val revealTime = startTime.plusMillis(1.days.inWholeMilliseconds)
    println("DEBUG: Scheduling entry for reveal at $revealTime (messageId: $messageId)")
    adventContext.scheduleEntries(
      guildId = "test-guild",
      postChannelId = "advent-reveals",
      entries = listOf(
        Triple(messageId, "🎄 Day 1 of Advent!", revealTime)
      )
    )

    // Restart Advent loop after scheduling
    val adventCommand =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.commands.advent.AdventCommand>()
    val readyEvent = io.mockk.mockk<net.dv8tion.jda.api.events.session.ReadyEvent>()
    io.mockk.every { readyEvent.jda } returns envWithTime.environment.jda
    adventCommand.onEvent(readyEvent)

    println("DEBUG: Entries in DB: ${adventDataConnector.getAdvents()}")
    println("DEBUG: Current time before advance: ${virtualClock.getCurrentTime()}")

    // Advance to the reveal time
    adventContext.advanceToNextReveal()

    println("DEBUG: Current time after advance: ${virtualClock.getCurrentTime()}")
    println("DEBUG: Messages sent: ${answerService.answers.size}")
    println("DEBUG: Answers: ${answerService.answers}")

    envWithTime.runScenario(answerService) {
      // Verify message was revealed
      expect("should reveal advent message") {
        // We check answerService directly because the last message might be the forward (with empty text)
        val hasMessage = answerService.answers.any { it.text.contains("Day 1 of Advent!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 1 of Advent!'")
        }
      }
    }

    // Verify the entry is marked as revealed
    assertEquals(1, adventContext.getRevealedCount())
    assertEquals(0, adventContext.getUnrevealedCount())
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
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
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())
    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

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

    // Restart Advent loop after scheduling
    val adventCommand =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.commands.advent.AdventCommand>()
    val readyEvent = io.mockk.mockk<net.dv8tion.jda.api.events.session.ReadyEvent>()
    io.mockk.every { readyEvent.jda } returns envWithTime.environment.jda
    adventCommand.onEvent(readyEvent)

    // Initially nothing revealed
    assertEquals(0, adventContext.getRevealedCount())
    assertEquals(3, adventContext.getUnrevealedCount())

    // Advance to first reveal
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      expect("should reveal first entry") {
        val hasMessage = answerService.answers.any { it.text.contains("Day 1!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 1!'")
        }
      }
    }

    assertEquals(1, adventContext.getRevealedCount())
    assertEquals(2, adventContext.getUnrevealedCount())

    // Advance to second reveal
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      expect("should reveal second entry") {
        val hasMessage = answerService.answers.any { it.text.contains("Day 2!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 2!'")
        }
      }
    }

    assertEquals(2, adventContext.getRevealedCount())
    assertEquals(1, adventContext.getUnrevealedCount())

    // Advance to third reveal
    adventContext.advanceToNextReveal()

    envWithTime.runScenario(answerService) {
      expect("should reveal third entry") {
        val hasMessage = answerService.answers.any { it.text.contains("Day 3!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 3!'")
        }
      }

      assertEquals(3, adventContext.getRevealedCount())
      assertEquals(0, adventContext.getUnrevealedCount())
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
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
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())
    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

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

    // Restart Advent loop after scheduling
    val adventCommand =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.commands.advent.AdventCommand>()
    val readyEvent = io.mockk.mockk<net.dv8tion.jda.api.events.session.ReadyEvent>()
    io.mockk.every { readyEvent.jda } returns envWithTime.environment.jda
    adventCommand.onEvent(readyEvent)

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
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
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
    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())
    populateMessageData(messageDataConnector, messages, guild.id, channel.id)

    // Schedule entry in the past (before current time)
    val pastTime = startTime.minusMillis(1.days.inWholeMilliseconds)
    adventContext.scheduleEntries(
      guildId = "past-guild",
      postChannelId = "advent-reveals",
      entries = listOf(
        Triple(messages[0].id, "Late reveal!", pastTime)
      )
    )

    // Restart Advent loop after scheduling
    val adventCommand =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.commands.advent.AdventCommand>()
    val readyEvent = io.mockk.mockk<net.dv8tion.jda.api.events.session.ReadyEvent>()
    io.mockk.every { readyEvent.jda } returns envWithTime.environment.jda
    adventCommand.onEvent(readyEvent)

    // The advent job should immediately reveal past entries
    // Just advance a tiny bit to let the job run
    adventContext.advanceTime(1.hours)

    envWithTime.runScenario(answerService) {
      expect("should reveal past entry immediately") {
        val hasMessage = answerService.answers.any { it.text.contains("Late reveal!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Late reveal!'")
        }
      }
    }

    assertEquals(1, adventContext.getRevealedCount())
  }
}