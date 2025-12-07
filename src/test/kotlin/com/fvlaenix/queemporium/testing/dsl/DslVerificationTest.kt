package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswer
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import com.fvlaenix.queemporium.verification.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Tests to verify the DSL itself works correctly and catches errors.
 * These tests ensure that when the DSL detects a problem, it fails appropriately.
 */
class DslVerificationTest : BaseKoinTest() {

  // ========================================
  // Tests for messageCount() verification
  // ========================================

  @Test
  fun `messageCount should pass when count matches`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
      awaitAll()

      expect("should have exactly 1 message") {
        messageSentCount(1)
      }
    }
  }

  @Test
  fun `messageCount should fail when count doesn't match`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    val exception = assertFails {
      envWithTime.runScenario(answerService) {
        sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
        awaitAll()

        expect("should fail with wrong count") {
          messageSentCount(5) // Wrong count!
        }
      }
    }

    assertTrue(
      exception.message?.contains("Expected 5 bot messages sent, but got 1") == true,
      "Expected error message about wrong count, got: ${exception.message}"
    )
  }

  @Test
  fun `messageCount should fail when expecting messages but none sent`() {
    val answerService = MockAnswerService()

    val exception = assertFails {
      answerService.verify {
        messageCount(1)
      }
    }

    assertTrue(
      exception.message?.contains("Expected 1 messages, but found 0") == true,
      "Expected error about no messages, got: ${exception.message}"
    )
  }

  // ========================================
  // Tests for lastMessageContains() verification
  // ========================================

  @Test
  fun `lastMessageContains should pass when text found`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
      awaitAll()

      expect("should contain Pong") {
        lastMessageContains("Pong!")
      }
    }
  }

  @Test
  fun `lastMessageContains should fail when text not found`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    val exception = assertFails {
      envWithTime.runScenario(answerService) {
        sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
        awaitAll()

        expect("should fail with wrong text") {
          lastMessageContains("NonexistentText")
        }
      }
    }

    assertTrue(
      exception.message?.contains("does not contain 'NonexistentText'") == true,
      "Expected error about missing text, got: ${exception.message}"
    )
  }

  @Test
  fun `lastMessageContains should fail when no messages exist`() {
    val answerService = MockAnswerService()

    val exception = assertFails {
      answerService.verify {
        lastMessageContains("anything")
      }
    }

    assertTrue(
      exception.message?.contains("No messages found") == true,
      "Expected error about no messages, got: ${exception.message}"
    )
  }

  // ========================================
  // Tests for messagesContain() verification
  // ========================================

  @Test
  fun `messagesContain should pass when any message has text`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "Hello World", emptyList()))
    answerService.answers.add(MockAnswer("channel2", "Goodbye World", emptyList()))

    answerService.verify {
      messagesContain("Hello")
      messagesContain("Goodbye")
    }
  }

  @Test
  fun `messagesContain should fail when no message has text`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "Hello", emptyList()))
    answerService.answers.add(MockAnswer("channel2", "World", emptyList()))

    val exception = assertFails {
      answerService.verify {
        messagesContain("Goodbye")
      }
    }

    assertTrue(
      exception.message?.contains("No message contains text: Goodbye") == true,
      "Expected error about missing text, got: ${exception.message}"
    )
  }

  // ========================================
  // Tests for messageAt() verification
  // ========================================

  @Test
  fun `messageAt should pass when message at index contains text`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "First message", emptyList()))
    answerService.answers.add(MockAnswer("channel2", "Second message", emptyList()))
    answerService.answers.add(MockAnswer("channel3", "Third message", emptyList()))

    answerService.verify {
      messageAt(0, "First")
      messageAt(1, "Second")
      messageAt(2, "Third")
    }
  }

  @Test
  fun `messageAt should fail when index out of bounds`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "Only message", emptyList()))

    val exception = assertFails {
      answerService.verify {
        messageAt(5, "anything")
      }
    }

    assertTrue(
      exception.message?.contains("No message at index 5") == true,
      "Expected error about index out of bounds, got: ${exception.message}"
    )
  }

  @Test
  fun `messageAt should fail when text not in message at index`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "First message", emptyList()))
    answerService.answers.add(MockAnswer("channel2", "Second message", emptyList()))

    val exception = assertFails {
      answerService.verify {
        messageAt(0, "Second") // Wrong text for index 0
      }
    }

    assertTrue(
      exception.message?.contains("does not contain: Second") == true,
      "Expected error about wrong text, got: ${exception.message}"
    )
  }

  // ========================================
  // Tests for isEmpty() verification
  // ========================================

  @Test
  fun `isEmpty should pass when no messages`() {
    val answerService = MockAnswerService()

    answerService.verify {
      isEmpty()
    }
  }

  @Test
  fun `isEmpty should fail when messages exist`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "Message exists", emptyList()))

    val exception = assertFails {
      answerService.verify {
        isEmpty()
      }
    }

    assertTrue(
      exception.message?.contains("Expected no messages, but found 1") == true,
      "Expected error about messages existing, got: ${exception.message}"
    )
  }

  // ========================================
  // Tests for noMessagesSent() verification
  // ========================================

  @Test
  fun `noMessagesSent should pass when no messages`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // Don't send any messages
      awaitAll()

      expect("should have no messages") {
        noMessagesSent()
      }
    }
  }

  @Test
  fun `noMessagesSent should fail when messages sent`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    val exception = assertFails {
      envWithTime.runScenario(answerService) {
        sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
        awaitAll()

        expect("should fail when messages sent") {
          noMessagesSent()
        }
      }
    }

    assertTrue(
      exception.message?.contains("Expected 0 bot messages sent, but got 1") == true,
      "Expected error about messages sent, got: ${exception.message}"
    )
  }

  // ========================================
  // Tests for verification with multiple messages
  // ========================================

  @Test
  fun `verify should work with multiple messages`() {
    val answerService = MockAnswerService()
    answerService.answers.add(MockAnswer("channel1", "Hello World", emptyList()))
    answerService.answers.add(MockAnswer("channel2", "Goodbye World", emptyList()))

    answerService.verify {
      messageCount(2)
      messagesContain("Hello")
      messagesContain("Goodbye")
    }
  }

  // ========================================
  // Tests for multiple messages
  // ========================================

  @Test
  fun `verification should work with multiple messages`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      user("bob")
      user("charlie")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
      awaitAll()
      sendMessage("test-guild", "general", "bob", "/shogun-sama ping")
      awaitAll()
      sendMessage("test-guild", "general", "charlie", "/shogun-sama ping")
      awaitAll()

      expect("should have 3 messages") {
        messageSentCount(3)
        lastMessageContains("Pong!")
      }
    }
  }

  // ========================================
  // Tests for fixture building
  // ========================================

  @Test
  fun `fixture should create guilds and channels correctly`() = runBlocking {
    val testFixture = fixture {
      guild("guild1") {
        channel("channel1")
        channel("channel2")
      }
      guild("guild2") {
        channel("channel3")
      }
    }

    assertEquals(2, testFixture.guilds.size, "Should have 2 guilds")
    assertEquals("guild1", testFixture.guilds[0].name)
    assertEquals("guild2", testFixture.guilds[1].name)
    assertEquals(2, testFixture.guilds[0].channels.size, "Guild1 should have 2 channels")
    assertEquals(1, testFixture.guilds[1].channels.size, "Guild2 should have 1 channel")
  }

  @Test
  fun `fixture should create users correctly`() {
    val testFixture = fixture {
      user("alice") { name("Alice") }
      user("bob") { name("Bob") }
    }

    assertEquals(2, testFixture.users.size, "Should have 2 users")
    assertEquals("Alice", testFixture.users["alice"]?.name)
    assertEquals("Bob", testFixture.users["bob"]?.name)
  }

  @Test
  fun `fixture should handle messages with reactions`() {
    val testFixture = fixture {
      user("alice")
      user("bob")
      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Hello") {
            reaction("👍") {
              user("bob")
            }
          }
        }
      }
    }

    val message = testFixture.guilds[0].channels[0].messages[0]
    assertEquals("Hello", message.text)
    assertEquals("alice", message.author)
    assertEquals(1, message.reactions.size)
    assertEquals("👍", message.reactions[0].emoji)
    assertEquals(listOf("bob"), message.reactions[0].users)
  }

  // ========================================
  // Tests for feature enablement
  // ========================================

  @Test
  fun `fixture should enable features correctly`() {
    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      enableFeature(FeatureKeys.SEARCH)
    }

    assertTrue(testFixture.enabledFeatures.contains(FeatureKeys.PING))
    assertTrue(testFixture.enabledFeatures.contains(FeatureKeys.SEARCH))
    assertEquals(2, testFixture.enabledFeatures.size)
  }

  @Test
  fun `fixture should enable multiple features at once`() {
    val testFixture = fixture {
      enableFeatures(FeatureKeys.PING, FeatureKeys.SEARCH, FeatureKeys.LOGGER)
    }

    assertTrue(testFixture.enabledFeatures.contains(FeatureKeys.PING))
    assertTrue(testFixture.enabledFeatures.contains(FeatureKeys.SEARCH))
    assertTrue(testFixture.enabledFeatures.contains(FeatureKeys.LOGGER))
    assertEquals(3, testFixture.enabledFeatures.size)
  }

  // ========================================
  // Edge cases
  // ========================================

  @Test
  fun `verification should handle empty answer service`() {
    val answerService = MockAnswerService()

    answerService.verify {
      isEmpty()
      messageCount(0)
    }
  }

  @Test
  fun `fixture can be created with minimal configuration`() {
    val testFixture = fixture {
      // Minimal fixture
    }

    assertEquals(0, testFixture.guilds.size)
    assertEquals(0, testFixture.users.size)
    assertEquals(0, testFixture.enabledFeatures.size)
  }

  @Test
  fun `expect block should support custom description`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice")
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      sendMessage("test-guild", "general", "alice", "/shogun-sama ping")
      awaitAll()

      expect("custom expectation description") {
        messageSentCount(1)
      }
    }
  }
}
