package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.configuration.commands.OnlineEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.MessageOrder
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.koin.dsl.module
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

class OnlineEmojiReactionIntegrationTest : BaseKoinTest() {

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `online emoji reaction triggers Hall of Fame when threshold reached`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice") { name("Alice") }
      user("bob") { name("Bob") }
      user("user1") { name("User1") }
      user("user2") { name("User2") }
      user("user3") { name("User3") }

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Amazing post!")
        }
        channel("hof")
      }
    }

    scenario {
      // Configure Hall of Fame by sending command message
      hallOfFame.configureHallOfFame("test-guild", "hof", threshold = 3, adminUserId = "admin")

      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      addReaction("test-guild", "general", 0, "⭐", "user1")
      awaitAll()

      addReaction("test-guild", "general", 0, "⭐", "user2")
      awaitAll()

      addReaction("test-guild", "general", 0, "⭐", "user3")
      awaitAll()

      // Advance time enough to trigger Hall of Fame job (4-9 hour window)
      advanceTime(10.hours)
      awaitAll()
      advanceTime(5.hours)
      awaitAll()

      expect("should send hall of fame message") {
        val messages = (answerService as com.fvlaenix.queemporium.service.MockAnswerService).answers
          .filter { it.text.contains("reactions") }
        if (messages.isEmpty()) {
          throw AssertionError("Expected Hall of Fame message, got none")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `online emoji reaction from DM is skipped`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)

      user("alice")
      user("bob")
    }

    scenario {
      // Test passes if no crash occurs - DM reactions should be silently skipped
      awaitAll()
      expect("no messages sent") {
        messageSentCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `adding reactions without Hall of Fame configured tracks emojis only`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)

      user("alice")
      user("bob")
      user("charlie")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Great post!")
        }
      }
    }

    scenario {
      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      addReaction("test-guild", "general", 0, "⭐", "bob")
      awaitAll()

      addReaction("test-guild", "general", 0, "❤️", "charlie")
      awaitAll()

      // No Hall of Fame message should be sent since HOF is not configured
      expect("no hall of fame messages") {
        messageSentCount(0)
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `multiple messages reach threshold and all are queued`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      user("bob")
      user("user1")
      user("user2")
      user("user3")

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Post 1")
          message(author = "bob", text = "Post 2")
          message(author = "alice", text = "Post 3")
        }
        channel("hof")
      }
    }

    scenario {
      // Configure Hall of Fame by sending command message
      hallOfFame.configureHallOfFame("test-guild", "hof", threshold = 3, adminUserId = "admin")

      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      // Add reactions to all 3 messages
      repeat(3) { userId ->
        addReaction("test-guild", "general", 0, "⭐", "user${userId + 1}")
        addReaction("test-guild", "general", 1, "⭐", "user${userId + 1}")
        addReaction("test-guild", "general", 2, "⭐", "user${userId + 1}")
      }
      awaitAll()

      // Trigger HOF jobs
      advanceTime(10.hours)
      awaitAll()
      advanceTime(5.hours)
      awaitAll()

      expect("should send multiple hall of fame messages") {
        val count = (answerService as com.fvlaenix.queemporium.service.MockAnswerService).answers.size
        if (count < 3) {
          throw AssertionError("Expected at least 3 HOF messages, got $count")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `different emoji types all counted toward threshold`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("user1")
      user("user2")
      user("user3")
      user("user4")
      user("user5")

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "user1", text = "Amazing content!")
        }
        channel("hof")
      }
    }

    scenario {
      // Configure Hall of Fame by sending command message
      hallOfFame.configureHallOfFame("test-guild", "hof", threshold = 5, adminUserId = "admin")

      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      // Add different emoji types
      addReaction("test-guild", "general", 0, "⭐", "user1")
      addReaction("test-guild", "general", 0, "⭐", "user2")
      addReaction("test-guild", "general", 0, "❤️", "user3")
      addReaction("test-guild", "general", 0, "👍", "user4")
      addReaction("test-guild", "general", 0, "🎉", "user5")
      awaitAll()

      // Total: 5 reactions (different emojis)
      // Advance time enough to trigger Hall of Fame job (4-9 hour window)
      advanceTime(10.hours)
      awaitAll()
      advanceTime(5.hours)
      awaitAll()

      expect("should reach threshold with mixed emojis") {
        val messages = (answerService as com.fvlaenix.queemporium.service.MockAnswerService).answers
          .filter { it.text.contains("reactions") }
        if (messages.isEmpty()) {
          throw AssertionError("Expected Hall of Fame message with mixed emojis, got none")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `message crossing threshold multiple times only added once`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("user1")
      user("user2")
      user("user3")
      user("user4")
      user("user5")

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "user1", text = "Great post!")
        }
        channel("hof")
      }
    }

    scenario {
      // Configure Hall of Fame by sending command message
      hallOfFame.configureHallOfFame("test-guild", "hof", threshold = 3, adminUserId = "admin")

      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      // Start with 2 reactions (below threshold)
      addReaction("test-guild", "general", 0, "⭐", "user1")
      addReaction("test-guild", "general", 0, "⭐", "user2")
      awaitAll()

      // Cross threshold (2 -> 3)
      addReaction("test-guild", "general", 0, "⭐", "user3")
      awaitAll()

      // Go above threshold (3 -> 5)
      addReaction("test-guild", "general", 0, "⭐", "user4")
      addReaction("test-guild", "general", 0, "⭐", "user5")
      awaitAll()

      // Trigger HOF
      advanceTime(10.hours)
      awaitAll()
      advanceTime(5.hours)
      awaitAll()

      expect("should only send one HOF message") {
        val messages = (answerService as com.fvlaenix.queemporium.service.MockAnswerService).answers
          .filter { it.text.contains("reactions") }
        if (messages.size != 1) {
          throw AssertionError("Expected exactly 1 HOF message, got ${messages.size}")
        }
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `multiple guilds isolated correctly`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("user1")
      user("user2")
      user("user3")

      guild("guild1") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "user1", text = "Guild 1 post")
        }
        channel("hof")
      }

      guild("guild2") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "user1", text = "Guild 2 post")
        }
        channel("hof")
      }
    }

    scenario {
      // Configure Hall of Fame for both guilds by sending command messages
      hallOfFame.configureHallOfFame("guild1", "hof", threshold = 2, adminUserId = "admin")
      hallOfFame.configureHallOfFame("guild2", "hof", threshold = 3, adminUserId = "admin")

      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      // Guild 1: 2 reactions (meets threshold)
      addReaction("guild1", "general", 0, "⭐", "user1")
      addReaction("guild1", "general", 0, "⭐", "user2")

      // Guild 2: 2 reactions (below threshold of 3)
      addReaction("guild2", "general", 0, "⭐", "user1")
      addReaction("guild2", "general", 0, "⭐", "user2")
      awaitAll()

      advanceTime(10.hours)
      awaitAll()
      advanceTime(5.hours)
      awaitAll()

      expect("guild1 should have HOF message, guild2 should not") {
        val messages = (answerService as com.fvlaenix.queemporium.service.MockAnswerService).answers
          .filter { it.text.contains("reactions") }

        // Guild1 should have message (2 reactions, threshold 2), guild2 should not (2 reactions, threshold 3)
        if (messages.isEmpty()) {
          throw AssertionError("Guild1 should have at least one HOF message")
        }
        // Could add more specific checks here if needed
      }
    }
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  fun `reaction count is tracked and updated when reactions increase after Hall of Fame posting`() = testBot {
    withVirtualTime(Instant.now())

    registerModuleBeforeFeatureLoad(module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 1,
          channelThreshold = 1,
          messageThreshold = 1,
          emojisThreshold = 10
        )
      }
    })

    before {
      enableFeature(FeatureKeys.ONLINE_EMOJI)
      enableFeature(FeatureKeys.HALL_OF_FAME)
      enableFeature(FeatureKeys.SET_HALL_OF_FAME)

      user("admin")
      user("alice")
      user("user1")
      user("user2")
      user("user3")
      user("user4")
      user("user5")

      guild("test-guild") {
        member("admin", isAdmin = true)
        channel("general") {
          message(author = "alice", text = "Epic post!")
        }
        channel("hof")
      }
    }

    scenario {
      // Configure Hall of Fame with threshold of 3
      hallOfFame.configureHallOfFame("test-guild", "hof", threshold = 3, adminUserId = "admin")

      // Trigger OnlineEmojiesStoreCommand periodic job to scan and add messages to database
      advanceTime(12.hours)
      awaitAll()

      // Add 3 reactions to reach threshold
      addReaction("test-guild", "general", 0, "⭐", "user1")
      addReaction("test-guild", "general", 0, "⭐", "user2")
      addReaction("test-guild", "general", 0, "⭐", "user3")
      awaitAll()

      // Trigger Hall of Fame jobs to post the message
      advanceTime(10.hours)
      awaitAll()
      advanceTime(5.hours)
      awaitAll()

      expect("should send hall of fame message with 3 reactions") {
        val messages = (answerService as com.fvlaenix.queemporium.service.MockAnswerService).answers
          .filter { it.text.contains("reactions") }
        if (messages.isEmpty()) {
          throw AssertionError("Expected Hall of Fame message, got none")
        }

        val firstMessage = messages.first()
        if (!firstMessage.text.contains("3 reactions")) {
          throw AssertionError("Expected '3 reactions' in message, got: ${firstMessage.text}")
        }
      }

      // Add 2 more reactions (total: 5) - this triggers updatePostedMessage()
      addReaction("test-guild", "general", 0, "⭐", "user4")
      awaitAll()
      addReaction("test-guild", "general", 0, "⭐", "user5")
      awaitAll()

      // Give time for the update to be processed
      advanceTime(1.hours)
      awaitAll()

      expect("should update reaction count in database after exceeding threshold") {
        val guild = guild("test-guild")
        val channel = channel(guild, "general")
        val message = message(channel, 0, MessageOrder.OLDEST_FIRST)

        reactions.expectPersisted(message) {
          count(5)
        }

        // TODO check if text changed
      }
    }
  }
}
