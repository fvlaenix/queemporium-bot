package com.fvlaenix.queemporium.testing.reference

import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.scenario.runScenario
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Reference test demonstrating fixture DSL patterns.
 *
 * Use this as a template for:
 * - Creating complex fixture setups
 * - Working with messages and reactions
 * - Organizing test data
 */
class FixturePatternReferenceTest : BaseKoinTest() {

  @Test
  fun `reference - fixture with pre-existing messages`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("alice")
      user("bob")

      guild("testGuild") {
        channel("general") {
          // Pre-create messages in fixture
          message(author = "alice", text = "Hello everyone!")
          message(author = "bob", text = "Hi Alice!")
        }
      }
    }

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // Messages already exist - can add reactions to them
      addReaction(
        guildId = "testGuild",
        channelId = "general",
        messageIndex = 0,  // First message (Alice's)
        emoji = "👋",
        userId = "bob"
      )

      awaitAll()

      // Can verify scenarios involving existing messages
      expect {
        noMessagesSent()  // No bot messages, only reactions
      }
    }
  }

  @Test
  fun `reference - fixture with reactions already added`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("alice")
      user("bob")
      user("charlie")

      guild("testGuild") {
        channel("general") {
          message(author = "alice", text = "Check this out!") {
            // Pre-add reactions in fixture
            reaction("👍") {
              users("bob", "charlie")
            }
            reaction("❤️") {
              user("alice")
            }
          }
        }
      }
    }

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    // The message already has reactions from fixture
    // You can test behavior that depends on existing reactions
    envWithTime.runScenario(answerService) {
      // Add more reactions
      addReaction("testGuild", "general", 0, "🎉", "bob")

      awaitAll()
    }
  }

  @Test
  fun `reference - multiple guilds and channels`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("alice")
      user("bob")

      // First guild
      guild("guild1") {
        channel("general")
        channel("announcements")
      }

      // Second guild
      guild("guild2") {
        channel("general")
        channel("support")
      }
    }

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // Test interactions across guilds
      sendMessage("guild1", "general", "alice", "Hello from guild1")
      sendMessage("guild2", "general", "bob", "Hello from guild2")

      awaitAll()
    }
  }

  @Test
  fun `reference - organized fixture with multiple messages and reactions`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      // Define all users upfront
      user("alice")
      user("bob")
      user("charlie")
      user("dave")

      guild("testGuild") {
        channel("general") {
          // Message 1: Popular post
          message(author = "alice", text = "Amazing news!") {
            reaction("🎉") {
              users("bob", "charlie", "dave")
            }
            reaction("👍") {
              users("bob", "charlie")
            }
          }

          // Message 2: Question
          message(author = "bob", text = "Anyone online?")

          // Message 3: Response with reaction
          message(author = "charlie", text = "I'm here!") {
            reaction("👋") {
              user("bob")
            }
          }
        }

        channel("announcements") {
          // Important announcement
          message(author = "alice", text = "Server maintenance tonight") {
            reaction("📢") {
              users("bob", "charlie", "dave")
            }
          }
        }
      }
    }

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    // Test can now work with this pre-populated state
    envWithTime.runScenario(answerService) {
      // Add more reactions to existing messages
      addReaction("testGuild", "general", 0, "❤️", "dave")

      // Send new messages
      sendMessage("testGuild", "general", "dave", "Great post Alice!")

      awaitAll()
    }
  }

  @Test
  fun `reference - extracting common fixtures`() = runBlocking {
    val answerService = MockAnswerService()

    // You can extract common fixture patterns
    val testFixture = createStandardTestGuild()

    val envWithTime = setupWithFixture(testFixture) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      sendMessage("testGuild", "general", "alice", "Using extracted fixture")
      awaitAll()
    }
  }

  // Helper function for reusable fixtures
  private fun createStandardTestGuild() = fixture {
    user("alice")
    user("bob")

    guild("testGuild") {
      channel("general")
      channel("announcements")
    }
  }
}
