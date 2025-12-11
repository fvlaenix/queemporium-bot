package com.fvlaenix.queemporium.commands.advent

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class AdventCommandTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent calendar reveals entries on schedule`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")
      user("bob")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1 content") {
            reaction("👍") { user("bob") }
          }
          message(author = "bob", text = "Day 2 content") {
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
          messages[0] to "🎄 Day 1!",
          messages[1] to "🎁 Day 2!"
        ),
        startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
        interval = 1.days
      )

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(2)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectMessagePosted(channel("test-guild", "advent-reveals").id, "Day 1!")

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(1)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectMessagePosted(channel("test-guild", "advent-reveals").id, "Day 2!")

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent with minimum count of 2`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "First") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Second") {
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
          messages[0] to "First!",
          messages[1] to "Second!"
        ),
        startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
        interval = 1.hours
      )

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(2)
      }
    }

    scenario {
      advent.revealAllEntries()

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test advent reveals past entries immediately`() = testBot {
    val startTime = Instant.parse("2024-12-10T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Late content") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
      }
    }

    setup {
      val messageId = getMessage("test-guild", "general", 0)
      val pastTime = startTime.minusMillis(1.days.inWholeMilliseconds)

      advent.scheduleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          Triple(messageId, "Late reveal!", pastTime)
        )
      )
    }

    scenario {
      advent.advanceTime(1.hours)

      advent.expectMessagePosted(channel("test-guild", "advent-reveals").id, "Late reveal!")

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test multiple advent entries reveal in correct order`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Day 2") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Day 3") {
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
          messages[1] to "Entry 2",
          messages[2] to "Entry 3"
        ),
        startTime = startTime.plusMillis(1.hours.inWholeMilliseconds),
        interval = 1.hours
      )
    }

    scenario {
      advent.revealAllEntries()

      advent.expectQueue {
        revealedCount(3)
        unrevealedCount(0)
      }

      expect("all entries revealed in order") {
        val answers = answerService!!.answers
        val entry1Index = answers.indexOfFirst { it.text.contains("Entry 1") }
        val entry2Index = answers.indexOfFirst { it.text.contains("Entry 2") }
        val entry3Index = answers.indexOfFirst { it.text.contains("Entry 3") }

        assertTrue(entry1Index >= 0, "Entry 1 not found")
        assertTrue(entry2Index >= 0, "Entry 2 not found")
        assertTrue(entry3Index >= 0, "Entry 3 not found")
        assertTrue(entry1Index < entry2Index, "Entry 1 should be before Entry 2")
        assertTrue(entry2Index < entry3Index, "Entry 2 should be before Entry 3")
      }
    }
  }

}
