package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.commands.OnlineEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.database.HallOfFameState
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.MessageOrder
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.koin.dsl.module
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

class HallOfFameLargeHistoryIntegrationTest : BaseKoinTest() {
  private val recentCandidatesCount = 10
  private val oldCandidatesCount = 5
  private val fillerMessagesCount = 120
  private val initialHistoryCount = recentCandidatesCount + oldCandidatesCount + fillerMessagesCount

  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  fun `large history backlog and incoming messages are processed together`() = testBot {
    val baseInstant = Instant.parse("2026-02-01T12:00:00Z")
    withVirtualTime(baseInstant)

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 120,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.MESSAGES_STORE)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)
      enableFeature(FeatureKeys.HALL_OF_FAME_OLDEST)

      user("admin")
      user("alice")
      user("bob")
      repeat(12) { i ->
        user("reactor${i + 1}")
      }

      guild("big-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          repeat(recentCandidatesCount) { i ->
            message(author = if (i % 2 == 0) "alice" else "bob", text = "recent-candidate-$i") {
              timeCreated(timeAtDaysAgo(baseInstant, (i + 1).toLong()))
              reaction("⭐") {
                users("reactor1", "reactor2", "reactor3", "reactor4", "reactor5")
              }
            }
          }

          repeat(oldCandidatesCount) { i ->
            message(author = if (i % 2 == 0) "alice" else "bob", text = "old-candidate-$i") {
              timeCreated(timeAtDaysAgo(baseInstant, (70 + i).toLong()))
              reaction("⭐") {
                users("reactor1", "reactor2", "reactor3", "reactor4", "reactor5")
              }
            }
          }

          repeat(fillerMessagesCount) { i ->
            message(author = if (i % 2 == 0) "alice" else "bob", text = "history-filler-$i") {
              timeCreated(timeAtDaysAgo(baseInstant, ((i % 40) + 1).toLong()))
            }
          }
        }
        channel("hof")
      }
    }

    scenario {
      val guild = guild("big-guild")
      val general = channel(guild, "general")

      val databaseConfiguration: DatabaseConfiguration = koin.get()
      val connector = HallOfFameConnector(databaseConfiguration.toDatabase())

      hallOfFame.configureHallOfFame("big-guild", "hof", threshold = 5, adminUserId = "admin")
      sendMessage("big-guild", "hof", "admin", "/shogun-sama hall-of-fame oldest 30d")
      awaitAll()

      expect("backlog candidates are selected from a large history set") {
        val toSendCount = (0 until (recentCandidatesCount + oldCandidatesCount)).count { index ->
          val candidate = message(general, index, MessageOrder.OLDEST_FIRST)
          connector.getMessage(candidate.id)?.state == HallOfFameState.TO_SEND
        }
        if (toSendCount < recentCandidatesCount) {
          throw AssertionError("Expected many backlog candidates to be TO_SEND, got $toSendCount")
        }
      }

      advanceTime(6.hours)
      awaitAll()

      sendMessage("big-guild", "general", "alice", "incoming-live-candidate")
      awaitAll()

      repeat(5) { i ->
        addReaction("big-guild", "general", initialHistoryCount, "⭐", "reactor${i + 1}")
        awaitAll()
      }

      expect("incoming candidate is posted while backlog posting is active") {
        val incomingMessage = message(general, 0, MessageOrder.NEWEST_FIRST)
        val state = connector.getMessage(incomingMessage.id)?.state
        if (state != HallOfFameState.POSTED) {
          throw AssertionError("Expected incoming message to be POSTED, got $state")
        }
      }

      advanceTime(12.hours)
      awaitAll()

      expect("backlog continues to post over subsequent cycles") {
        val postedCandidates = (0 until (recentCandidatesCount + oldCandidatesCount)).count { index ->
          val candidate = message(general, index, MessageOrder.OLDEST_FIRST)
          connector.getMessage(candidate.id)?.state == HallOfFameState.POSTED
        }
        if (postedCandidates < 2) {
          throw AssertionError("Expected at least 2 backlog posts over subsequent cycles, got $postedCandidates")
        }
      }

      expect("hall of fame announcement count reflects backlog plus live traffic") {
        val hofAnnouncements = answerService?.answers
          ?.filter { it.text.contains("reactions") }
          ?: throw AssertionError("Mock answer service is not available")

        if (hofAnnouncements.size < 3) {
          throw AssertionError("Expected at least 3 Hall of Fame announcements, got ${hofAnnouncements.size}")
        }
      }
    }
  }

  private fun timeAtDaysAgo(baseInstant: Instant, daysAgo: Long): OffsetDateTime {
    val secondsInDay = 24L * 60L * 60L
    return OffsetDateTime.ofInstant(baseInstant.minusSeconds(daysAgo * secondsInDay), ZoneOffset.UTC)
  }
}
