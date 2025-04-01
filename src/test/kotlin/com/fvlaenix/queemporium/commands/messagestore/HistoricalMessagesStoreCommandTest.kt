package com.fvlaenix.queemporium.commands.messagestore

import org.junit.jupiter.api.Test
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

    // Create several messages before the command starts processing
    val messageContents = listOf(
      "First historical message",
      "Second historical message",
      "Third historical message"
    )

    val messages = messageContents.map { content ->
      env.sendMessage(
        "Test Guild",
        "general",
        user,
        content
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
    }
  }

  @Test
  fun `test large number of historical messages are processed correctly`() {
    // Create a user
    val user = env.createUser("Test User", false)

    // Create a larger number of messages
    val messageCount = 20
    val messages = (1..messageCount).map { index ->
      env.sendMessage(
        "Test Guild",
        "general",
        user,
        "Historical message #$index"
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
      messageDataConnector.get(message.id) != null
    }

    // Assert that all messages were processed
    assertEquals(messageCount, storedCount, "All historical messages should be processed")
  }

  @Test
  fun `test messages from different channels are processed`() {
    // Create a second channel
    val secondChannel = env.createTextChannel(
      env.jda.getGuildsByName("Test Guild", false).first(),
      "second-channel"
    )

    // Create a user
    val user = env.createUser("Test User", false)

    // Create messages in different channels
    val firstChannelMessage = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message in first channel"
    ).complete(true)!!

    val secondChannelMessage = env.sendMessage(
      "Test Guild",
      "second-channel",
      user,
      "Message in second channel"
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
  }
}