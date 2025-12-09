package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit

class HallOfFameExampleTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test hall of fame with scenario DSL and helpers`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice") { name("Alice") }
      user("bob") { name("Bob") }

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "This is an amazing post!")
        }
        channel("hall-of-fame")
      }
    }

    setup {
      hallOfFame.configureHallOfFameBlocking("test-guild", "hall-of-fame", threshold = 5, adminUserId = "admin")
      hallOfFame.seedMessageToCount("test-guild", "general", messageIndex = 0, count = 5)
    }

    scenario {
      hallOfFame.triggerRetrieveJob()
      hallOfFame.triggerSendJob()

      expect("should send hall of fame message") {
        val hasMessage = answerService!!.answers.any { it.text.contains("reactions") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'reactions'")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test multiple messages reaching hall of fame`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      user("bob")

      guild("my-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "First great post!")
          message(author = "bob", text = "Second great post!")
        }
        channel("hall-of-fame")
      }
    }

    setup {
      hallOfFame.configureHallOfFameBlocking("my-guild", "hall-of-fame", threshold = 3, adminUserId = "admin")
      hallOfFame.seedMessageToCount("my-guild", "general", messageIndex = 0, count = 3)
      hallOfFame.seedMessageToCount("my-guild", "general", messageIndex = 1, count = 4)
    }

    scenario {
      advanceTime(kotlin.time.Duration.parse("10h"))
      awaitAll()

      advanceTime(kotlin.time.Duration.parse("8h"))
      awaitAll()

      expect("should send multiple hall of fame messages") {
        val count = answerService!!.answers.size
        if (count < 2) {
          throw AssertionError("Expected at least 2 messages, got $count. Messages: ${answerService!!.answers}")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test hall of fame with different emojis`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      user("bob")
      user("charlie")

      guild("emoji-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Amazing content!")
        }
        channel("hall-of-fame")
      }
    }

    setup {
      hallOfFame.configureHallOfFameBlocking("emoji-guild", "hall-of-fame", threshold = 3, adminUserId = "admin")

      hallOfFame.seedEmojiReactions(
        guildId = "emoji-guild",
        channelId = "general",
        messageIndex = 0,
        emoji = "⭐",
        userIds = listOf("alice", "bob")
      )

      hallOfFame.seedEmojiReactions(
        guildId = "emoji-guild",
        channelId = "general",
        messageIndex = 0,
        emoji = "❤️",
        userIds = listOf("charlie")
      )
    }

    scenario {
      advanceTime(kotlin.time.Duration.parse("10h"))
      awaitAll()
      advanceTime(kotlin.time.Duration.parse("4h"))
      awaitAll()

      expect("should send hall of fame message") {
        val hasMessage = answerService!!.answers.isNotEmpty()
        if (!hasMessage) {
          throw AssertionError("No message sent")
        }
      }
    }
  }
}