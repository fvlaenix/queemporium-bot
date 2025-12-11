package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AdventExampleTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent calendar with single entry reveal`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1 surprise!")
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messageId = getMessage("test-guild", "general", 0)
      val revealTime = startTime.plusMillis(1.days.inWholeMilliseconds)

      advent.scheduleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          Triple(messageId, "🎄 Day 1 of Advent!", revealTime)
        )
      )
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectMessagePosted(channel("test-guild", "advent-reveals").id, "Day 1 of Advent!")
    }

    scenario {
      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent calendar with multiple entries`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
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

    setup {
      val messages = getMessages("calendar-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "calendar-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "🎄 Day 1!",
          messages[1] to "🎁 Day 2!",
          messages[2] to "⭐ Day 3!"
        ),
        startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
        interval = 1.days
      )

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(3)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectMessagePosted(channel("calendar-guild", "advent-reveals").id, "Day 1!")

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(2)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectMessagePosted(channel("calendar-guild", "advent-reveals").id, "Day 2!")

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(1)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectMessagePosted(channel("calendar-guild", "advent-reveals").id, "Day 3!")

      advent.expectQueue {
        revealedCount(3)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent calendar reveal all at once`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
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

    setup {
      val messages = getMessages("fast-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "fast-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "Day 1",
          messages[1] to "Day 2"
        ),
        startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
        interval = 1.hours
      )
    }

    scenario {
      advent.revealAllEntries()

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(0)
      }

      expect("should reveal all entries") {
        messageSentCount(4)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent calendar with past reveal time`() = testBot {
    val startTime = Instant.parse("2024-12-10T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("past-guild") {
        channel("general") {
          message(author = "alice", text = "Past content")
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messageId = getMessage("past-guild", "general", 0)
      val pastTime = startTime.minusMillis(1.days.inWholeMilliseconds)

      advent.scheduleEntries(
        guildId = "past-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          Triple(messageId, "Late reveal!", pastTime)
        )
      )
    }

    scenario {
      advent.advanceTime(1.hours)

      advent.expectMessagePosted(channel("past-guild", "advent-reveals").id, "Late reveal!")

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  fun `test advent calendar with command text and top 20 selection from 35 messages`() = testBot {
    val startTime = Instant.parse("2024-11-30T23:00:00Z")  // Start before first reveal
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)
      enableFeature(FeatureKeys.AUTHOR_COLLECT)
      enableFeature(FeatureKeys.MESSAGES_STORE)

      user("alice")
      user("bob")
      user("charlie")
      user("admin")

      // Create additional users for reactions (user1, user2, ..., user20)
      repeat(20) { i ->
        user("user${i + 1}")
      }

      guild("main-guild") {
        member("alice", isAdmin = true)

        channel("popular-channel") {
          // Create 35 messages with varying reaction counts
          // Messages 0-19: will have 20-1 reactions (top 20)
          // Messages 20-34: will have 0 reactions (bottom 15)

          // Top 20 messages with decreasing reactions
          message(author = "alice", text = "Top message 1") {
            reaction("👍") {
              repeat(20) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 2") {
            reaction("❤️") {
              repeat(19) { i -> user("user${i + 1}") }
            }
          }
          message(author = "charlie", text = "Top message 3") {
            reaction("🎉") {
              repeat(18) { i -> user("user${i + 1}") }
            }
          }
          message(author = "alice", text = "Top message 4") {
            reaction("⭐") {
              repeat(17) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 5") {
            reaction("🔥") {
              repeat(16) { i -> user("user${i + 1}") }
            }
          }
          message(author = "charlie", text = "Top message 6") {
            reaction("👍") {
              repeat(15) { i -> user("user${i + 1}") }
            }
          }
          message(author = "alice", text = "Top message 7") {
            reaction("❤️") {
              repeat(14) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 8") {
            reaction("🎉") {
              repeat(13) { i -> user("user${i + 1}") }
            }
          }
          message(author = "charlie", text = "Top message 9") {
            reaction("⭐") {
              repeat(12) { i -> user("user${i + 1}") }
            }
          }
          message(author = "alice", text = "Top message 10") {
            reaction("🔥") {
              repeat(11) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 11") {
            reaction("👍") {
              repeat(10) { i -> user("user${i + 1}") }
            }
          }
          message(author = "charlie", text = "Top message 12") {
            reaction("❤️") {
              repeat(9) { i -> user("user${i + 1}") }
            }
          }
          message(author = "alice", text = "Top message 13") {
            reaction("🎉") {
              repeat(8) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 14") {
            reaction("⭐") {
              repeat(7) { i -> user("user${i + 1}") }
            }
          }
          message(author = "charlie", text = "Top message 15") {
            reaction("🔥") {
              repeat(6) { i -> user("user${i + 1}") }
            }
          }
          message(author = "alice", text = "Top message 16") {
            reaction("👍") {
              repeat(5) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 17") {
            reaction("❤️") {
              repeat(4) { i -> user("user${i + 1}") }
            }
          }
          message(author = "charlie", text = "Top message 18") {
            reaction("🎉") {
              repeat(3) { i -> user("user${i + 1}") }
            }
          }
          message(author = "alice", text = "Top message 19") {
            reaction("⭐") {
              repeat(2) { i -> user("user${i + 1}") }
            }
          }
          message(author = "bob", text = "Top message 20") {
            reaction("🔥") {
              user("user1")
            }
          }

          // Bottom 15 messages with no reactions
          repeat(15) { index ->
            message(author = "alice", text = "Low priority message ${index + 1}")
          }
        }
        channel("advent-calendar")
      }
    }

    scenario {
      val command = "/shogun-sama start-advent\ncount:20 start:01-12-2024-00:00 finish:20-12-2024-00:00 year:2024"

      sendMessage(
        guildId = "main-guild",
        channelId = "advent-calendar",
        userId = "alice",
        text = command
      )
      awaitAll()

      // Give time for database operations to complete
      kotlinx.coroutines.delay(500)

      // Restart the advent loop to ensure it's running
      advent.restartAdventLoop()

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(20)
      }
    }

    scenario {
      // Advance to first reveal (Dec 1)
      advent.advanceToNextReveal()

      // Give time for the advent loop to process the reveal
      kotlinx.coroutines.delay(500)

      expect("should reveal first message (with least reactions)") {
        val answers = answerService!!.answers
        // The advent calendar sends a description like "Message number 20 from bob with 1 reactions"
        val hasMessage = answers.any { it.text.contains("Message number 20") && it.text.contains("1 reactions") }
        if (!hasMessage) {
          val answerTexts = answers.joinToString("\n") { "  - ${it.text}" }
          throw AssertionError("Expected to reveal message #20 (with 1 reaction), but it wasn't found in answers. Found ${answers.size} answers:\n$answerTexts")
        }
      }

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(19)
      }
    }

    scenario {
      // Reveal 5 more entries
      repeat(5) {
        advent.advanceToNextReveal()
      }

      advent.expectQueue {
        revealedCount(6)
        unrevealedCount(14)
      }
    }

    scenario {
      // Reveal all remaining entries
      advent.revealAllEntries()

      advent.expectQueue {
        revealedCount(20)
        unrevealedCount(0)
      }

      expect("should have revealed exactly 20 messages (top by reactions)") {
        // Each reveal sends 2 messages: description + forwarded message
        messageSentCount(40)
      }

      expect("should not reveal any low priority messages") {
        val answers = answerService!!.answers
        val hasLowPriorityMessage = answers.any { it.text.contains("Low priority message") }
        if (hasLowPriorityMessage) {
          throw AssertionError("Low priority messages should not be revealed")
        }
      }
    }
  }
}
