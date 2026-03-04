package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.MessageOrder
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

class HallOfFameOldestCommandTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `oldest command approves backlog immediately with hour-based duration`() = testBot {
    withVirtualTime(Instant.parse("2026-01-10T10:00:00Z"))

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)
      enableFeature(FeatureKeys.HALL_OF_FAME_OLDEST)

      user("admin")
      user("alice")

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "candidate message")
        }
        channel("hof")
      }
    }

    setup {
      hallOfFame.seedMessageToCount("test-guild", "general", messageIndex = 0, count = 3)
      hallOfFame.configureBlocking("test-guild", "hof", threshold = 3, adminUserId = "admin")
    }

    scenario {
      val guild = guild("test-guild")
      val candidate = message(channel(guild, "general"), 0, MessageOrder.OLDEST_FIRST)

      advanceTime(1.hours)
      awaitAll()

      sendMessage("test-guild", "hof", "admin", "/shogun-sama hall-of-fame oldest 12h")
      awaitAll()

      expect("oldest command keeps hour precision in response") {
        val backlogReply = answerService?.answers
          ?.lastOrNull { it.text.contains("Backlog approved!") }
          ?: throw AssertionError("Expected backlog approval reply")
        if (!backlogReply.text.contains("12 hours")) {
          throw AssertionError("Expected hour-based duration in reply, got: ${backlogReply.text}")
        }
      }

      hallOfFame.expectQueued(candidate, isSent = false)

      advanceTime(6.hours)
      awaitAll()
      hallOfFame.expectQueued(candidate, isSent = true)
    }
  }
}
