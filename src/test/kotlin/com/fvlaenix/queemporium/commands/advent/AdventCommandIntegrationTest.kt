package com.fvlaenix.queemporium.commands.advent

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AdventCommandIntegrationTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent with custom year parameter`() = testBot {
    val startTime = Instant.parse("2025-01-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Message from 2023") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Message from 2023 v2") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messages = getMessages("test-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "Entry 1",
          messages[1] to "Entry 2"
        ),
        startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
        interval = 1.hours
      )

      assertEquals(0, advent.getRevealedCount())
      assertEquals(2, advent.getUnrevealedCount())
    }

    scenario {
      advent.revealAllEntries()

      assertEquals(2, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent with exact minimum count of 2`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "First message") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Second message") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messages = getMessages("test-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "First",
          messages[1] to "Second"
        ),
        startTime = startTime,
        interval = 1.days
      )

      assertEquals(0, advent.getRevealedCount())
      assertEquals(2, advent.getUnrevealedCount())
    }

    scenario {
      advent.revealAllEntries()

      assertEquals(2, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent with entries in different channels`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")
      user("bob")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "General message") {
            reaction("👍") { user("bob") }
          }
        }
        channel("random") {
          message(author = "bob", text = "Random message") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val generalMessage = getMessage("test-guild", "general", 0)
      val randomMessage = getMessage("test-guild", "random", 0)

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          generalMessage to "From general",
          randomMessage to "From random"
        ),
        startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
        interval = 2.hours
      )

      assertEquals(0, advent.getRevealedCount())
      assertEquals(2, advent.getUnrevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      expect("first entry revealed") {
        val hasMessage = answerService!!.answers.any { it.text.contains("From general") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'From general'")
        }
      }

      assertEquals(1, advent.getRevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      expect("second entry revealed") {
        val hasMessage = answerService!!.answers.any { it.text.contains("From random") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'From random'")
        }
      }

      assertEquals(2, advent.getRevealedCount())
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent with very short intervals`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Quick 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Quick 2") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Quick 3") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messages = getMessages("test-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "Quick 1",
          messages[1] to "Quick 2",
          messages[2] to "Quick 3"
        ),
        startTime = startTime,
        interval = kotlin.time.Duration.parse("1ms")
      )
    }

    scenario {
      advent.revealAllEntries()

      assertEquals(3, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())

      expect("all messages revealed quickly") {
        messageSentCount(6)
      }
    }
  }


  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent handles entry revealing when bot restarts`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Persistent") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Persistent 2") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messages = getMessages("test-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "Persistent Entry 1",
          messages[1] to "Persistent Entry 2"
        ),
        startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
        interval = 1.hours,
        restartLoop = false
      )

      assertEquals(0, advent.getRevealedCount())
      assertEquals(2, advent.getUnrevealedCount())
    }

    scenario {
      advent.restartAdventLoop()

      advent.advanceToNextReveal()

      expect("entry revealed after restart") {
        val hasMessage = answerService!!.answers.any { it.text.contains("Persistent Entry 1") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Persistent Entry 1'")
        }
      }

      assertEquals(1, advent.getRevealedCount())
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent with long duration intervals`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Week 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Week 2") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messages = getMessages("test-guild", "general")

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "Weekly 1",
          messages[1] to "Weekly 2"
        ),
        startTime = startTime.plusMillis(7.days.inWholeMilliseconds),
        interval = 7.days
      )

      assertEquals(0, advent.getRevealedCount())
      assertEquals(2, advent.getUnrevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      assertEquals(1, advent.getRevealedCount())
      assertEquals(1, advent.getUnrevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      assertEquals(2, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())
    }
  }
}
