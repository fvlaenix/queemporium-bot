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

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(2)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      expect("first entry revealed") {
        val hasMessage = answerService!!.answers.any { it.text.contains("From general") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'From general'")
        }
      }

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(1)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      expect("second entry revealed") {
        val hasMessage = answerService!!.answers.any { it.text.contains("From random") }
        if (!hasMessage) {
          throw AssertionError("No message found containing 'From random'")
        }
      }

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(0)
      }
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

      advent.expectQueue {
        revealedCount(3)
        unrevealedCount(0)
      }

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

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(2)
      }
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

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(1)
      }
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

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(2)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectQueue {
        revealedCount(1)
        unrevealedCount(1)
      }
    }

    scenario {
      advent.advanceToNextReveal()

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test post-right-now posts next entry immediately`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }
      user("alice")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Day 1 content") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Day 2 content") {
            reaction("👍") { user("alice") }
          }
          message(author = "alice", text = "Day 3 content") {
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
          messages[2] to "Entry 3"
        ),
        startTime = startTime.plusMillis(10.days.inWholeMilliseconds),
        interval = 1.days
      )

      advent.expectQueue {
        revealedCount(0)
        unrevealedCount(3)
      }
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_POST_RIGHT_NOW)
      awaitAll()

      advent.expectMessagePosted(channel("test-guild", "advent-reveals").id, "Entry 1")

      advent.expectQueue {
        revealedCount(2)
        unrevealedCount(1)
      }

      awaitAll()

      expect("success message sent") {
        val answers = answerService!!.answers
        val successMessage = answers.find { it.text.contains("Posted Advent entry #1") }
        assertTrue(successMessage != null, "Should send success message")
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test post-right-now with no advent configured`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }

      guild("test-guild") {
        channel("advent-reveals")
        addAdmin("admin")
      }
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_POST_RIGHT_NOW)
      awaitAll()

      expect("error message sent") {
        val answers = answerService!!.answers
        val errorMessage = answers.find { it.text.contains("Advent is not configured") }
        assertTrue(errorMessage != null, "Should send error message when advent not configured")
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test list shows all unrevealed entries`() = testBot {
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
          messages[2] to "Entry 3"
        ),
        startTime = startTime.plusMillis(1.days.inWholeMilliseconds),
        interval = 1.days
      )
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_LIST)
      awaitAll()

      expect("list response contains all entries") {
        val answers = answerService!!.answers
        val listMessage = answers.find { it.text.contains("Advent queue") }
        assertTrue(listMessage != null, "Should send list message")
        assertTrue(listMessage!!.text.contains("#1"), "Should contain entry #1")
        assertTrue(listMessage.text.contains("#2"), "Should contain entry #2")
        assertTrue(listMessage.text.contains("#3"), "Should contain entry #3")
        assertTrue(listMessage.text.contains("UTC"), "Should contain UTC timezone")
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `test list with no advent configured`() = testBot {
    val startTime = Instant.parse("2024-12-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      enableFeature(FeatureKeys.ADVENT)

      user("admin") { isBot(false) }

      guild("test-guild") {
        channel("advent-reveals")
        addAdmin("admin")
      }
    }

    scenario {
      sendMessage("test-guild", "advent-reveals", "admin", AdventCommand.COMMAND_LIST)
      awaitAll()

      expect("error message sent") {
        val answers = answerService!!.answers
        val errorMessage = answers.find { it.text.contains("Advent is not configured") }
        assertTrue(errorMessage != null, "Should send error message when advent not configured")
      }
    }
  }
}
