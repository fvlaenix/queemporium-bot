package com.fvlaenix.queemporium.testing.reference

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.scenario.runScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Reference test demonstrating simple command testing pattern.
 *
 * Use this as a template for:
 * - Basic command/response testing
 * - Single user interactions
 * - Synchronous message verification
 */
class SimpleCommandReferenceTest : BaseKoinTest() {

  @Test
  fun `reference - simple command response`() = runBlocking {
    // 1. Setup: Create mock answer service
    val answerService = MockAnswerService()

    // 2. Define test fixture with minimal requirements
    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    // 3. Initialize test environment
    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    // 4. Execute test scenario
    envWithTime.runScenario(answerService) {
      // Send command
      sendMessage(
        guildId = "testGuild",
        channelId = "testChannel",
        userId = "testUser",
        text = "/shogun-sama ping"
      )

      // Wait for processing
      awaitAll()

      // Verify response
      expect("bot should respond with Pong") {
        messageSentCount(1)
        lastMessageContains("Pong!")
      }
    }
  }

  @Test
  fun `reference - multiple users interaction`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      // Define multiple users
      user("alice")
      user("bob")

      guild("testGuild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // First user sends command
      sendMessage("testGuild", "general", "alice", "/shogun-sama ping")
      awaitAll()

      // Second user sends command
      sendMessage("testGuild", "general", "bob", "/shogun-sama ping")
      awaitAll()

      // Verify two responses
      expect("bot should respond to both users") {
        messageSentCount(2)
      }
    }
  }

  @Test
  fun `reference - multiple channels`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      user("testUser")

      guild("testGuild") {
        channel("general")
        channel("announcements")
      }
    }

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // Send to first channel
      sendMessage("testGuild", "general", "testUser", "/shogun-sama ping")
      awaitAll()

      // Send to second channel
      sendMessage("testGuild", "announcements", "testUser", "/shogun-sama ping")
      awaitAll()

      // Verify responses
      expect {
        messageSentCount(2)
      }
    }
  }
}
