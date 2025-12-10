package com.fvlaenix.queemporium.testing.examples

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.dsl.ChannelResolver
import com.fvlaenix.queemporium.testing.dsl.GuildResolver
import com.fvlaenix.queemporium.testing.dsl.MessageOrder
import com.fvlaenix.queemporium.testing.dsl.MessageResolver
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.time.VirtualClock
import com.fvlaenix.queemporium.verification.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class FixtureDslExampleTest : BaseKoinTest() {

  @Test
  fun `test ping command with new fixture DSL`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())

    val testFixture = fixture {
      enableFeature(FeatureKeys.PING)

      user("alice") {
        name("Alice")
      }

      user("bob") {
        name("Bob")
      }

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Hello everyone!")
        }
      }
    }

    val answerService = MockAnswerService()

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    val env = envWithTime.environment

    val alice = env.jda.guilds.first().members.find { it.user.name == "Alice" }?.user
      ?: throw IllegalStateException("Alice not found")

    env.sendMessage(
      guildName = "test-guild",
      channelName = "general",
      user = alice,
      message = "/shogun-sama ping"
    )

    envWithTime.awaitAll()

    answerService.verify {
      message {
        text("Pong!")
      }
    }
  }

  @Test
  fun `test with message reactions`() = runBlocking {
    val virtualClock = VirtualClock(Instant.now())

    val testFixture = fixture {
      user("alice")
      user("bob")
      user("charlie")

      guild("test-guild") {
        channel("general") {
          message(author = "alice", text = "Great post!") {
            reaction("👍") {
              users("bob", "charlie")
            }
            reaction("❤️") {
              user("bob")
            }
          }
        }
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock, autoStart = false)
    val env = envWithTime.environment

    env.start()
    envWithTime.awaitAll()

    val guild = GuildResolver.resolve(env.jda, "test-guild")
    val channel = ChannelResolver.resolve(guild, "general")
    val testChannel = channel as? com.fvlaenix.queemporium.mock.TestTextChannel
    if (testChannel != null) {
      assertEquals(1, testChannel.messages.size, "Should have 1 message")
    }

    val message = MessageResolver.resolve(channel, 0, MessageOrder.OLDEST_FIRST)
    val reactions = message.reactions

    assertEquals(2, reactions.size, "Should have 2 different reactions")

    val thumbsUpReaction = reactions.find { it.emoji.name == "👍" }
    assertEquals(2, thumbsUpReaction?.count, "Thumbs up should have 2 reactions")

    val heartReaction = reactions.find { it.emoji.name == "❤️" }
    assertEquals(1, heartReaction?.count, "Heart should have 1 reaction")
  }

  @Test
  fun `test time travel with virtual clock`() = runBlocking {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)

    val testFixture = fixture {
      user("alice")

      guild("test-guild") {
        channel("general")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock)
    val timeController = envWithTime.timeController!!

    assertEquals(startTime, timeController.getCurrentTime())

    timeController.advanceTime(5.hours)

    val expectedTime = startTime.plusMillis(5.hours.inWholeMilliseconds)
    assertEquals(expectedTime, timeController.getCurrentTime())
  }
}
