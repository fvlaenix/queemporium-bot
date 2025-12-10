package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.database.MessageEmojiData
import com.fvlaenix.queemporium.database.MessageEmojiDataConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.ChannelResolver
import com.fvlaenix.queemporium.testing.dsl.MessageOrder
import com.fvlaenix.queemporium.testing.dsl.MessageResolver
import com.fvlaenix.queemporium.testing.dsl.testBot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val databaseConfig: DatabaseConfiguration by koin.inject()
      val database = databaseConfig.toDatabase()
      val hallOfFameConnector = HallOfFameConnector(database)
      val messageEmojiDataConnector = MessageEmojiDataConnector(database)

      val guild = guild("test-guild")
      val channel = channel(guild, "general")
      val message = message(channel, 0, MessageOrder.OLDEST_FIRST)

      messageEmojiDataConnector.insert(MessageEmojiData(message.id, 10))

      hallOfFameCommand.recheckMessage(message.id, guild.id)
      awaitAll()

      val hofMessage = hallOfFameConnector.getMessage(message.id)
      assertNull(hofMessage, "Message should not be added to Hall of Fame when not configured")
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
      hallOfFame.configureHallOfFameBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val databaseConfig: DatabaseConfiguration by koin.inject()
      val database = databaseConfig.toDatabase()
      val hallOfFameConnector = HallOfFameConnector(database)
      val messageEmojiDataConnector = MessageEmojiDataConnector(database)

      val guild = guild("test-guild")
      val channel = channel(guild, "general")
      val message = message(channel, 0, MessageOrder.OLDEST_FIRST)

      messageEmojiDataConnector.insert(MessageEmojiData(message.id, 5))

      hallOfFameCommand.recheckMessage(message.id, guild.id)
      awaitAll()

      val hofMessage = hallOfFameConnector.getMessage(message.id)
      assertNull(hofMessage, "Message with count below threshold should not be added")
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
      hallOfFame.configureHallOfFameBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val databaseConfig: DatabaseConfiguration by koin.inject()
      val database = databaseConfig.toDatabase()
      val hallOfFameConnector = HallOfFameConnector(database)
      val messageEmojiDataConnector = MessageEmojiDataConnector(database)

      val guild = guild("test-guild")
      val channel = ChannelResolver.resolve(guild, "general")
      val message = MessageResolver.resolve(channel, 0, MessageOrder.OLDEST_FIRST)

      messageEmojiDataConnector.insert(MessageEmojiData(message.id, 10))

      hallOfFameCommand.recheckMessage(message.id, guild.id)
      awaitAll()

      val hofMessage = hallOfFameConnector.getMessage(message.id)
      assertNotNull(hofMessage, "Message meeting threshold should be added to Hall of Fame")
      assertEquals(false, hofMessage.isSent, "Message should be queued (not sent yet)")
      assertEquals(guild.id, hofMessage.guildId)
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
      hallOfFame.configureHallOfFameBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val databaseConfig: DatabaseConfiguration by koin.inject()
      val database = databaseConfig.toDatabase()
      val hallOfFameConnector = HallOfFameConnector(database)
      val messageEmojiDataConnector = MessageEmojiDataConnector(database)

      val guild = guild("test-guild")
      val channel = ChannelResolver.resolve(guild, "general")
      val message = MessageResolver.resolve(channel, 0, MessageOrder.OLDEST_FIRST)

      messageEmojiDataConnector.insert(MessageEmojiData(message.id, 25))

      hallOfFameCommand.recheckMessage(message.id, guild.id)
      awaitAll()

      val hofMessage = hallOfFameConnector.getMessage(message.id)
      assertNotNull(hofMessage, "Message exceeding threshold should be added to Hall of Fame")
      assertEquals(false, hofMessage.isSent)
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
      hallOfFame.configureHallOfFameBlocking("test-guild", "hof", threshold = 10, adminUserId = "admin")
    }

    scenario {
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val databaseConfig: DatabaseConfiguration by koin.inject()
      val database = databaseConfig.toDatabase()
      val hallOfFameConnector = HallOfFameConnector(database)
      val messageEmojiDataConnector = MessageEmojiDataConnector(database)

      val guild = guild("test-guild")
      val channel = ChannelResolver.resolve(guild, "general")
      val message = MessageResolver.resolve(channel, 0, MessageOrder.OLDEST_FIRST)

      messageEmojiDataConnector.insert(MessageEmojiData(message.id, 10))

      hallOfFameCommand.recheckMessage(message.id, guild.id)
      awaitAll()

      val hofMessage1 = hallOfFameConnector.getMessage(message.id)
      assertNotNull(hofMessage1)

      hallOfFameCommand.recheckMessage(message.id, guild.id)
      awaitAll()

      val hofMessage2 = hallOfFameConnector.getMessage(message.id)
      assertNotNull(hofMessage2)
      assertEquals(hofMessage1.messageId, hofMessage2.messageId)
      assertEquals(hofMessage1.isSent, hofMessage2.isSent)
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

      runBlocking {
        hallOfFameCommand.recheckGuild(guild.id)
      }
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
      hallOfFame.configureHallOfFameBlocking("test-guild", "hof", threshold = 5, adminUserId = "admin")
      hallOfFame.seedMessageToCount("test-guild", "general", 0, count = 7)
      hallOfFame.seedMessageToCount("test-guild", "general", 1, count = 6)
      hallOfFame.seedMessageToCount("test-guild", "general", 2, count = 8)
    }

    scenario {
      val hallOfFameCommand: HallOfFameCommand by koin.inject()
      val databaseConfig: DatabaseConfiguration by koin.inject()
      val database = databaseConfig.toDatabase()
      val hallOfFameConnector = HallOfFameConnector(database)
      val guild = guild("test-guild")

      runBlocking {
        hallOfFameCommand.recheckGuild(guild.id)
      }
      awaitAll()

      val channel = channel(guild, "general")
      val messages = listOf(
        message(channel, 0, MessageOrder.OLDEST_FIRST),
        message(channel, 1, MessageOrder.OLDEST_FIRST),
        message(channel, 2, MessageOrder.OLDEST_FIRST)
      )

      assertNotNull(hallOfFameConnector.getMessage(messages[0].id), "Message 0 should be in HOF")
      assertNotNull(hallOfFameConnector.getMessage(messages[1].id), "Message 1 should be in HOF")
      assertNotNull(hallOfFameConnector.getMessage(messages[2].id), "Message 2 should be in HOF")
    }
  }
}
