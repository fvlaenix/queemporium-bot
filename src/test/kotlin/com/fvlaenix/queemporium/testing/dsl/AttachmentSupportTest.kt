package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Simple test to verify attachment support in the DSL.
 */
class AttachmentSupportTest : BaseKoinTest() {

  @Test
  fun `should support sending messages with attachments`() = runBlocking {
    val answerService = MockAnswerService()

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)
      user("alice") { name("Alice") }
      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, VirtualClock(Instant.now())) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // Send a message with attachments - this should work now!
      sendMessage(
        "test-guild",
        "general",
        "alice",
        "/shogun-sama ping",
        listOf(createTestAttachment("test.jpg"))
      )
      awaitAll()

      expect("should have received ping response") {
        messageSentCount(1)
        lastMessageContains("Pong!")
      }
    }
  }
}
