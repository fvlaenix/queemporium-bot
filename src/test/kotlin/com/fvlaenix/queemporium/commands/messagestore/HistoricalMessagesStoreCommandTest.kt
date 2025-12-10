package com.fvlaenix.queemporium.commands.messagestore

import com.fvlaenix.queemporium.testing.dsl.GuildResolver
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for historical message processing in MessagesStoreCommand.
 * These tests verify the bot's ability to process messages that existed before it started.
 */
class HistoricalMessagesStoreCommandTest : BaseMessagesStoreCommandTest() {

  // Override to disable auto-start for historical tests
  override var autoStartEnvironment: Boolean = false

  @Test
  fun `test historical messages are processed on startup`() {
    // Create a user
    val user = env.createUser("Test User", false)

    val now = OffsetDateTime.now()
    val timeframes = listOf(
      now.minus(30, ChronoUnit.DAYS),
      now.minus(15, ChronoUnit.DAYS),
      now.minus(7, ChronoUnit.DAYS)
    )

    val messageContents = listOf(
      "First historical message (30 days ago)",
      "Second historical message (15 days ago)",
      "Third historical message (7 days ago)"
    )

    val messages = messageContents.zip(timeframes).map { (content, time) ->
      env.sendMessage(
        "Test Guild",
        "general",
        user,
        content,
        emptyList(),
        time
      ).complete(true)!!
    }

    // Clear the database to simulate messages that haven't been processed yet
    val messageIds = messages.map { it.id }
    cleanupDatabase(messageIds)

    // Verify messages are not in the database before startup
    messages.forEach { message ->
      val storedMessage = messageDataConnector.get(message.id)
      assertNull(storedMessage, "Message should not be in database before startup")
    }

    // Now start the environment which should trigger processing of historical messages
    startEnvironment()

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify that historical messages were processed and stored
    messages.forEach { message ->
      val storedMessage = messageDataConnector.get(message.id)
      assertNotNull(storedMessage, "Historical message should be stored in database after startup")
      assertEquals(message.contentRaw, storedMessage.text, "Stored historical message text should match")
      assertEquals(
        message.timeCreated.toEpochSecond(),
        storedMessage.epoch,
        "Stored message epoch should match the created time"
      )
    }
  }

  @Test
  fun `test large number of historical messages are processed correctly`() {
    // Create a user
    val user = env.createUser("Test User", false)

    val now = OffsetDateTime.now()

    // Create a larger number of messages with varying timestamps
    val messageCount = 20
    val messages = (1..messageCount).map { index ->
      val daysAgo = 7 + (index % 24)
      val messageTime = now.minus(daysAgo.toLong(), ChronoUnit.DAYS)

      env.sendMessage(
        "Test Guild",
        "general",
        user,
        "Historical message #$index (${daysAgo} days ago)",
        emptyList(),
        messageTime
      ).complete(true)!!
    }

    // Clear the database
    val messageIds = messages.map { it.id }
    cleanupDatabase(messageIds)

    // Start the environment
    startEnvironment()

    // Wait for processing to complete
    env.awaitAll()

    // Count how many messages were properly stored
    val storedCount = messages.count { message ->
      val stored = messageDataConnector.get(message.id)
      stored != null && stored.epoch == message.timeCreated.toEpochSecond()
    }

    // Assert that all messages were processed
    assertEquals(messageCount, storedCount, "All historical messages should be processed with correct timestamps")
  }

  @Test
  fun `test messages from different channels are processed`() {
    // Create a second channel
    env.createTextChannel(
      GuildResolver.resolve(env.jda, "Test Guild"),
      "second-channel"
    )

    val now = OffsetDateTime.now()

    // Create a user
    val user = env.createUser("Test User", false)

    // Create messages in different channels with different timestamps
    val firstChannelMessage = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message in first channel",
      emptyList(),
      now.minus(14, ChronoUnit.DAYS)
    ).complete(true)!!

    val secondChannelMessage = env.sendMessage(
      "Test Guild",
      "second-channel",
      user,
      "Message in second channel",
      emptyList(),
      now.minus(21, ChronoUnit.DAYS)
    ).complete(true)!!

    // Clear the database
    cleanupDatabase(listOf(firstChannelMessage.id, secondChannelMessage.id))

    // Start the environment
    startEnvironment()

    // Wait for processing
    env.awaitAll()

    // Verify both messages were processed
    val firstStoredMessage = messageDataConnector.get(firstChannelMessage.id)
    val secondStoredMessage = messageDataConnector.get(secondChannelMessage.id)

    assertNotNull(firstStoredMessage, "Message from first channel should be stored")
    assertNotNull(secondStoredMessage, "Message from second channel should be stored")
    assertEquals(firstChannelMessage.channelId, firstStoredMessage.channelId)
    assertEquals(secondChannelMessage.channelId, secondStoredMessage.channelId)

    assertEquals(
      firstChannelMessage.timeCreated.toEpochSecond(), firstStoredMessage.epoch,
      "First channel message epoch should match the created time"
    )
    assertEquals(
      secondChannelMessage.timeCreated.toEpochSecond(), secondStoredMessage.epoch,
      "Second channel message epoch should match the created time"
    )
  }
}
