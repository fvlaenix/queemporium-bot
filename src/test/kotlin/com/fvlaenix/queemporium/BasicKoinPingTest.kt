package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test

class BasicKoinPingTest : BaseKoinTest() {
  @Test
  fun `test ping command with direct koin setup`() = testBot {
    before {
      enableFeature(FeatureKeys.PING)

      user("testUser")

      guild("testGuild") {
        channel("general")
      }
    }

    scenario {
      sendMessage(
        guildId = "testGuild",
        channelId = "general",
        userId = "testUser",
        text = "/shogun-sama ping"
      )

      awaitAll()

      expect("bot should respond with Pong") {
        messageSentCount(1)
        lastMessageContains("Pong!")
      }
    }
  }
}