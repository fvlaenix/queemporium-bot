package com.fvlaenix.queemporium.testing.examples

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

      expect("should reveal advent message") {
        val hasMessage = answerService!!.answers.any { it.text.contains("Day 1 of Advent!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 1 of Advent!'")
        }
      }
    }

    scenario {
      assertEquals(1, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())
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

      assertEquals(0, advent.getRevealedCount())
      assertEquals(3, advent.getUnrevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      expect("should reveal first entry") {
        val hasMessage = answerService!!.answers.any { it.text.contains("Day 1!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 1!'")
        }
      }

      assertEquals(1, advent.getRevealedCount())
      assertEquals(2, advent.getUnrevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      expect("should reveal second entry") {
        val hasMessage = answerService!!.answers.any { it.text.contains("Day 2!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 2!'")
        }
      }

      assertEquals(2, advent.getRevealedCount())
      assertEquals(1, advent.getUnrevealedCount())
    }

    scenario {
      advent.advanceToNextReveal()

      expect("should reveal third entry") {
        val hasMessage = answerService!!.answers.any { it.text.contains("Day 3!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Day 3!'")
        }
      }

      assertEquals(3, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())
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

      assertEquals(2, advent.getRevealedCount())
      assertEquals(0, advent.getUnrevealedCount())

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

      expect("should reveal past entry immediately") {
        val hasMessage = answerService!!.answers.any { it.text.contains("Late reveal!") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'Late reveal!'")
        }
      }

      assertEquals(1, advent.getRevealedCount())
    }
  }
}