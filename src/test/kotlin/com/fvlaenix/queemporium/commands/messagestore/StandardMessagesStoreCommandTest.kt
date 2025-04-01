package com.fvlaenix.queemporium.commands.messagestore

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for standard behaviors of MessagesStoreCommand with auto-started environment.
 * These tests verify real-time message storage and deletion functionality.
 */
class StandardMessagesStoreCommandTest : BaseMessagesStoreCommandTest() {

  // In this class, autoStartEnvironment is true by default (inherited from base class)

  @Test
  fun `test message is stored when sent`() {
    // Create a user
    val user = env.createUser("Test User", false)

    // Send a test message
    val messageContent = "Test message for storage"
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      messageContent
    ).complete(true)!!

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify the message was stored in the database
    val storedMessage = messageDataConnector.get(message.id)

    // Assert that the message exists and has correct data
    assertNotNull(storedMessage, "Message should be stored in database")
    assertEquals(message.id, storedMessage.messageId, "Stored message ID should match")
    assertEquals(messageContent, storedMessage.text, "Stored message text should match")
    assertEquals(message.author.id, storedMessage.authorId, "Stored message author should match")
    assertEquals(message.guildId, storedMessage.guildId, "Stored message guild ID should match")
    assertEquals(message.channelId, storedMessage.channelId, "Stored message channel ID should match")
  }

  @Test
  fun `test message is deleted from database when message is deleted`() {
    // Create a user
    val user = env.createUser("Test User", false)

    // Send a test message
    val messageContent = "Test message that will be deleted"
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      messageContent
    ).complete(true)!!

    // Wait for the message to be stored
    env.awaitAll()

    // Verify message was initially stored
    val storedMessageBeforeDeletion = messageDataConnector.get(message.id)
    assertNotNull(storedMessageBeforeDeletion, "Message should be stored in database initially")

    // Delete the message
    message.delete().complete()

    // Wait for the deletion to be processed
    env.awaitAll()

    // Verify the message was deleted from the database
    val storedMessageAfterDeletion = messageDataConnector.get(message.id)
    assertEquals(null, storedMessageAfterDeletion, "Message should be deleted from database after deletion")
  }

  @Test
  fun `test message with attachments is stored correctly`() {
    // Create a user
    val user = env.createUser("Test User", false)

    // Create a test attachment
    val attachment = com.fvlaenix.queemporium.mock.createTestAttachment("test_image.jpg")

    // Send a message with attachment
    val messageContent = "Message with attachment"
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      messageContent,
      listOf(attachment)
    ).complete(true)!!

    // Wait for processing
    env.awaitAll()

    // Verify the message was stored with correct data
    val storedMessage = messageDataConnector.get(message.id)

    assertNotNull(storedMessage, "Message with attachment should be stored")
    assertEquals(message.id, storedMessage.messageId, "Stored message ID should match")
    assertEquals(messageContent, storedMessage.text, "Stored message text should match")
    assertEquals(message.author.id, storedMessage.authorId, "Stored message author should match")
  }

  @Test
  fun `test multiple messages are stored independently`() {
    // Create a user
    val user = env.createUser("Test User", false)

    // Send multiple messages
    val messages = listOf(
      "First test message",
      "Second test message",
      "Third test message"
    ).map { content ->
      env.sendMessage(
        "Test Guild",
        "general",
        user,
        content
      ).complete(true)!!
    }

    // Wait for processing
    env.awaitAll()

    // Verify all messages are stored correctly
    messages.forEach { message ->
      val storedMessage = messageDataConnector.get(message.id)
      assertNotNull(storedMessage, "Message should be stored: ${message.contentRaw}")
      assertEquals(message.contentRaw, storedMessage.text, "Stored message text should match")
    }
  }
}