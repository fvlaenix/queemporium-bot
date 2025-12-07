package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test

class PingTest : BaseKoinTest() {

  @Test
  fun `test ping command`() = testBot {
    before {
      enableFeature(FeatureKeys.PING)

      user("Test User")

      guild("Test Guild") {
        channel("general")
      }
    }

    scenario {
      sendMessage("Test Guild", "general", "Test User", "/shogun-sama ping")
      awaitAll()

      expect("bot should respond with Pong") {
        messageSentCount(1)
        lastMessageContains("Pong!")
      }
    }
  }
}
