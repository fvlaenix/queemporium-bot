package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.log.LogLevelTestExtension
import com.fvlaenix.queemporium.testing.trace.ScenarioTestWatcher
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.GlobalContext.stopKoin
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val LOG = Logging.getLogger(MessageStoreServiceTest::class.java)

@ExtendWith(ScenarioTestWatcher::class, LogLevelTestExtension::class)
class MessageStoreServiceTest {

  @AfterEach
  fun tearDownKoin() {
    stopKoin()
  }

  @Test
  fun `test MessageStoreService initializes caches for all guilds and channels`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }
      user("bob") { name("Bob") }

      guild("test-guild-1") {
        member("alice")
        channel("general") {
          message(author = "alice", text = "Hello from guild 1!")
        }
        channel("random") {
          message(author = "bob", text = "Random message")
        }
      }

      guild("test-guild-2") {
        member("bob")
        channel("announcements") {
          message(author = "bob", text = "Important announcement")
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guilds = messageStoreService.guilds().toList()
    assertEquals(2, guilds.size)

    Unit
  }

  @Test
  fun `test independent flows iterate at their own pace`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }

      guild("test-guild") {
        member("alice")
        channel("general") {
          message(author = "alice", text = "Message 1")
          message(author = "alice", text = "Message 2")
          message(author = "alice", text = "Message 3")
          message(author = "alice", text = "Message 4")
          message(author = "alice", text = "Message 5")
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    val consumer1Messages = mutableListOf<String>()
    val consumer2Messages = mutableListOf<String>()

    channel.messages().take(3).collect { message ->
      consumer1Messages.add(message.contentRaw)
    }

    channel.messages().collect { message ->
      consumer2Messages.add(message.contentRaw)
    }

    assertEquals(3, consumer1Messages.size)
    assertEquals(5, consumer2Messages.size)

    Unit
  }

  @Test
  fun `test messages are fetched from newest to oldest`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }

      guild("test-guild") {
        member("alice")
        channel("general") {
          message(author = "alice", text = "Oldest message")
          message(author = "alice", text = "Middle message")
          message(author = "alice", text = "Newest message")
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    val messages = channel.messages().toList()

    assertEquals("Newest message", messages[0].contentRaw)
    assertEquals("Middle message", messages[1].contentRaw)
    assertEquals("Oldest message", messages[2].contentRaw)

    Unit
  }

  @Test
  fun `test MessageStoreService does not persist messages`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }

      guild("test-guild") {
        member("alice")
        channel("general") {
          message(author = "alice", text = "Test message")
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    // MessageStoreService should provide access to messages
    val messages = channel.messages().toList()
    assertEquals(1, messages.size)
    assertEquals("Test message", messages[0].contentRaw)

    // But it should NOT persist them - that's MessagesStoreCommand's responsibility
    Unit
  }

  @Test
  fun `test consumer registration and deregistration`() = runBlocking {
    val fixture = fixture {
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    messageStoreService.registerConsumer()
    messageStoreService.registerConsumer()
    messageStoreService.unregisterConsumer()
    messageStoreService.unregisterConsumer()

    Unit
  }

  @Test
  fun `test takeNext helper retrieves specified number of messages`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }

      guild("test-guild") {
        member("alice")
        channel("general") {
          message(author = "alice", text = "Message 1")
          message(author = "alice", text = "Message 2")
          message(author = "alice", text = "Message 3")
          message(author = "alice", text = "Message 4")
          message(author = "alice", text = "Message 5")
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    val messages = channel.takeNext(3)

    assertEquals(3, messages.size)
    assertEquals("Message 5", messages[0].contentRaw)
    assertEquals("Message 4", messages[1].contentRaw)
    assertEquals("Message 3", messages[2].contentRaw)

    Unit
  }

  @Test
  fun `test empty channel returns no messages`() = runBlocking {
    val fixture = fixture {
      guild("test-guild") {
        channel("empty-channel")
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    val messages = channel.messages().toList()

    assertTrue(messages.isEmpty())

    Unit
  }

  @Test
  fun `test lazy loading only fetches needed messages`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }

      guild("test-guild") {
        member("alice")
        channel("general") {
          // Create 1000 messages to test lazy loading
          repeat(1000) { i ->
            message(author = "alice", text = "Message ${i + 1}")
          }
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    // Take only 10 messages
    val messages = channel.takeNext(10)

    // Verify we got exactly 10 messages
    assertEquals(10, messages.size)
    assertEquals("Message 1000", messages[0].contentRaw) // Newest first
    assertEquals("Message 991", messages[9].contentRaw)

    // Verify that approximately 100 messages were loaded (one batch), not all 1000
    val cachedCount = messageStoreService.getCachedMessageCount(guild.id, channel.id)
    assertTrue(cachedCount >= 10, "Should have at least 10 messages cached")
    assertTrue(cachedCount <= 100, "Should have at most 100 messages cached (one batch), but had $cachedCount")

    LOG.info("Lazy loading test: took 10 messages, cached $cachedCount messages (expected ~100, not 1000)")

    Unit
  }

  @Test
  fun `test multiple concurrent consumers do not interfere`() = runBlocking {
    val fixture = fixture {
      user("alice") { name("Alice") }

      guild("test-guild") {
        member("alice")
        channel("general") {
          repeat(10) { i ->
            message(author = "alice", text = "Message ${i + 1}")
          }
        }
      }
    }

    val envWithTime = setupWithFixture(fixture, autoStart = false) { builder ->
      builder.answerService = MockAnswerService()
    }

    val messageStoreService = MessageStoreService(
      envWithTime.environment.jda,
      envWithTime.testProvider
    )

    messageStoreService.initialize()

    val guild = messageStoreService.guilds().toList().first()
    val channel = guild.channels().toList().first()

    val consumer1 = channel.messages().toList()
    val consumer2 = channel.messages().toList()
    val consumer3 = channel.messages().toList()

    assertEquals(10, consumer1.size)
    assertEquals(10, consumer2.size)
    assertEquals(10, consumer3.size)

    assertEquals(consumer1.map { it.id }, consumer2.map { it.id })
    assertEquals(consumer1.map { it.id }, consumer3.map { it.id })

    Unit
  }
}
