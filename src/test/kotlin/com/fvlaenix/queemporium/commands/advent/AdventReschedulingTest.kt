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

class AdventReschedulingTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test rescheduling with multiple remaining entries`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }
      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Entry 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 2") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 3") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 4") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
        addAdmin("admin")
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
          messages[2] to "Entry 3",
          messages[3] to "Entry 4"
        ),
        startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
        interval = 1.days
      )
    }

    setup {
      val originalEntries = adventDataConnector.getAdvents()
        .filter { !it.isRevealed }
        .sortedBy { it.epoch }

      val originalFinalEpoch = originalEntries.last().epoch

      envWithTime.storageForTests["originalFinalEpoch"] = originalFinalEpoch
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_POST_RIGHT_NOW)
      awaitAll()

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(2)
      }

      expect("success message sent") {
        val answers = answerService!!.answers
        val successMessage = answers.find { it.text.contains("Posted Advent entry #1") }
        assertTrue(successMessage != null, "Should send success message")
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test rescheduling with single remaining entry`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }
      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Entry 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 2") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
        addAdmin("admin")
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
        startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
        interval = 1.days
      )
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_POST_RIGHT_NOW)
      awaitAll()

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test rescheduling when all epochs are in the past`() = testBot {
    val startTime = Instant.parse("2024-12-10T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }
      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Entry 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 2") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 3") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
        addAdmin("admin")
      }
    }

    setup {
      val messages = getMessages("test-guild", "general")
      val pastTime = startTime.minusMillis(5.days.inWholeMilliseconds)

      advent.scheduleMultipleEntries(
        guildId = "test-guild",
        postChannelId = "advent-reveals",
        entries = listOf(
          messages[0] to "Entry 1",
          messages[1] to "Entry 2",
          messages[2] to "Entry 3"
        ),
        startTime = pastTime,
        interval = 1.hours
      )
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_POST_RIGHT_NOW)
      awaitAll()

      advent.expectQueue {
        revealedCount(3)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test rescheduling maintains even distribution`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }
      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Entry 1") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 2") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 3") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 4") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Entry 5") {
            reaction("👍") { user("alice") }
          }
        }
        channel("advent-reveals")
        addAdmin("admin")
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
          messages[2] to "Entry 3",
          messages[3] to "Entry 4",
          messages[4] to "Entry 5"
        ),
        startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
        interval = 1.days
      )
    }

    setup {
      val originalFinalEpoch = adventDataConnector.getAdvents()
        .filter { !it.isRevealed }
        .maxOf { it.epoch }

      envWithTime.storageForTests["originalFinalEpoch"] = originalFinalEpoch
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_POST_RIGHT_NOW)
      awaitAll()

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(3)
      }

      expect("success message sent") {
        val answers = answerService!!.answers
        val successMessage = answers.find { it.text.contains("Posted Advent entry #1") }
        assertTrue(successMessage != null, "Should send success message with position")
      }
    }
  }
}
