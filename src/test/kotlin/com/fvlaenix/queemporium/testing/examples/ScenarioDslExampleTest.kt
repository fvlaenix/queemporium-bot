package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class ScenarioDslExampleTest : BaseKoinTest() {

  @Test
  fun `test ping command with scenario DSL`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      user("alice") { name("Alice") }
      user("bob") { name("Bob") }

      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime =
      setupWithFixture(testFixture, virtualClock) { builder: com.fvlaenix.queemporium.koin.BotConfigBuilder ->
        builder.answerService = answerService
      }

    envWithTime.runScenario(answerService) {
      // Send ping command
      sendMessage(
        guildId = "test-guild",
        channelId = "general",
        userId = "alice",
        text = "/shogun-sama ping"
      )

      // Wait for processing
      awaitAll()

      // Verify response
      expect("bot should respond with Pong!") {
        lastMessageContains("Pong!")
      }
    }
  }

  @Test
  fun `test multiple messages with scenario DSL`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      user("alice")
      user("bob")

      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime =
      setupWithFixture(testFixture, virtualClock) { builder: com.fvlaenix.queemporium.koin.BotConfigBuilder ->
        builder.answerService = answerService
      }

    envWithTime.runScenario(answerService) {
      // First message
      sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
      awaitAll()

      // Second message
      sendMessage("test-guild", "general", "bob", "/shogun-sama ping")
      awaitAll()

      // Verify both responses
      expect {
        messageSentCount(2)
      }
    }
  }

  @Test
  fun `test reactions with scenario DSL`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("alice")
      user("bob")
      user("charlie")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "First message")
          message(author = "bob", text = "Second message")
        }
      }
    }

    val envWithTime =
      setupWithFixture(testFixture, virtualClock) { builder: com.fvlaenix.queemporium.koin.BotConfigBuilder ->
        builder.answerService = answerService
      }

    envWithTime.runScenario(answerService) {
      // Add reactions to first message
      addReaction("test-guild", "general", 0, "👍", "bob")
      addReaction("test-guild", "general", 0, "👍", "charlie")
      addReaction("test-guild", "general", 0, "❤️", "alice")

      // Add reaction to second message
      addReaction("test-guild", "general", 1, "🎉", "alice")

      awaitAll()

      // Expectations would verify reactions were added
      expect {
        // Since we don't directly check reactions yet, just verify no crashes
        noMessagesSent()
      }
    }
  }

  @Test
  fun `test time advancement with scenario DSL`() = runBlocking {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime =
      setupWithFixture(testFixture, virtualClock) { builder: com.fvlaenix.queemporium.koin.BotConfigBuilder ->
        builder.answerService = answerService
      }

    // Verify starting time before scenario
    assertEquals(startTime, envWithTime.timeController!!.getCurrentTime())

    envWithTime.runScenario(answerService) {
      // Advance time by 4 hours
      advanceTime(4.hours)
    }

    // Verify time advanced after scenario
    val expectedTime = startTime.plusMillis(4.hours.inWholeMilliseconds)
    assertEquals(expectedTime, envWithTime.timeController!!.getCurrentTime())
  }

  @Test
  fun `test combined scenario with messages and expectations`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      user("alice")
      user("bob")

      guild("my-guild") {
        channel("chat")
        channel("announcements")
      }
    }

    val envWithTime =
      setupWithFixture(testFixture, virtualClock) { builder: com.fvlaenix.queemporium.koin.BotConfigBuilder ->
        builder.answerService = answerService
      }

    envWithTime.runScenario(answerService) {
      // Send message to chat channel
      sendMessage("my-guild", "chat", "alice", "/shogun-sama ping")
      awaitAll()

      // Verify one message was sent
      expect("should have one message") {
        messageSentCount(1)
      }

      // Send message to announcements channel
      sendMessage("my-guild", "announcements", "bob", "/shogun-sama ping")
      awaitAll()

      // Verify total of two messages
      expect("should have two messages") {
        messageSentCount(2)
      }

      // Verify specific message
      expect("last message should contain Pong") {
        lastMessageContains("Pong!")
      }
    }
  }
}
