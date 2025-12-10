package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DslIntegrationTest : BaseKoinTest() {

  @Test
  fun `DSL helpers should resolve entities in setup and scenario`() = testBot {
    before {
      enableFeature(FeatureKeys.PING)
      user("alice")
      user("bob")
      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Oldest")
          message(author = "bob", text = "Newest")
        }
      }
    }

    setup {
      // Test resolvers in setup context
      val guild = guild("test-guild")
      val channel = channel(guild, "general")
      val msg1 = message(channel, 0) // Oldest
      val msg2 = latestMessage(channel) // Newest
    }

    scenario {
      // Test resolvers in scenario context
      val guild = guild("test-guild")
      val channel = channel("test-guild", "general")
      val msg1 = message(channel, 0)
      val msg2 = latestMessage(channel)

      expect("DSL helpers work") {
        // Just verify we can access them
        assertNotNull(guild)
        assertEquals("test-guild", guild.name)
        assertEquals("general", channel.name)
        assertEquals("Oldest", msg1.contentRaw)
        assertEquals("Newest", msg2.contentRaw)
      }
    }
  }
}