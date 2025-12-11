package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.MessageOrder
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit

class HallOfFameRecheckTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckMessage when Hall of Fame not configured does nothing`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Great post!")
        }
      }
    }

    scenario {
      val guild = guild("test-guild")
      val message = message(channel(guild, "general"), 0, MessageOrder.OLDEST_FIRST)

      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 10)

      hallOfFame.recheckMessage(message, guild.id)
      awaitAll()

      hallOfFame.expectNotQueued(message)
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckMessage when count below threshold does nothing`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Great post!")
        }
        channel("hof")
      }
    }

    setup {
      hallOfFame.configureBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val guild = guild("test-guild")
      val channel = channel(guild, "general")
      val message = message(channel, 0, MessageOrder.OLDEST_FIRST)

      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 5)

      hallOfFame.recheckMessage(message, guild.id)
      awaitAll()

      hallOfFame.expectNotQueued(message)
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckMessage when count meets threshold adds to queue`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Great post!")
        }
        channel("hof")
      }
    }

    setup {
      hallOfFame.configureBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val guild = guild("test-guild")
      val message = message(channel(guild, "general"), 0, MessageOrder.OLDEST_FIRST)

      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 10)

      hallOfFame.recheckMessage(message, guild.id)
      awaitAll()

      hallOfFame.expectQueued(message, isSent = false)
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckMessage when count exceeds threshold adds to queue`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Amazing!")
        }
        channel("hof")
      }
    }

    setup {
      hallOfFame.configureBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val guild = guild("test-guild")
      val message = message(channel(guild, "general"), 0, MessageOrder.OLDEST_FIRST)

      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 25)

      hallOfFame.recheckMessage(message, guild.id)
      awaitAll()

      hallOfFame.expectQueued(message, isSent = false)
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckMessage when already in queue not sent does not duplicate`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Great post!")
        }
        channel("hof")
      }
    }

    setup {
      hallOfFame.configureBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val guild = guild("test-guild")
      val message = message(channel(guild, "general"), 0, MessageOrder.OLDEST_FIRST)

      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 10)

      hallOfFame.recheckMessage(message, guild.id)
      awaitAll()

      hallOfFame.expectQueued(message, isSent = false)

      hallOfFame.recheckMessage(message, guild.id)
      awaitAll()

      hallOfFame.expectQueued(message, isSent = false)
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckGuild when not configured does nothing`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Post 1")
          message(author = "alice", text = "Post 2")
        }
      }
    }

    scenario {
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val guild = guild("test-guild")

      hallOfFameCommand.recheckGuild(guild.id)
      awaitAll()

      // No assertion needed - just verify no crash
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `recheckGuild when configured updates all messages`() = testBot {
    withVirtualTime(Instant.now())

    before {
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Post 1")
          message(author = "alice", text = "Post 2")
          message(author = "alice", text = "Post 3")
        }
        channel("hof")
      }
    }

    setup {
      hallOfFame.configureBlocking("test-guild", "hof", threshold = 5, adminUserId = "admin")
      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 7)
      hallOfFame.seedMessageToCount("test-guild", "general", 1, count = 6)
      hallOfFame.seedMessageToCount("test-guild", "general", 2, count = 8)
    }

    scenario {
      val guild = guild("test-guild")

      hallOfFame.recheckGuild(guild.id)

      val channel = channel(guild, "general")
      val messages = listOf(
        message(channel, 0, MessageOrder.OLDEST_FIRST),
        message(channel, 1, MessageOrder.OLDEST_FIRST),
        message(channel, 2, MessageOrder.OLDEST_FIRST)
      )

      hallOfFame.expectQueued(messages[0], isSent = false)
      hallOfFame.expectQueued(messages[1], isSent = false)
      hallOfFame.expectQueued(messages[2], isSent = false)
    }
  }
}
