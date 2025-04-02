package com.fvlaenix.queemporium.commands.logging

import com.fvlaenix.queemporium.commands.LoggerMessageCommand
import org.junit.jupiter.api.Test
import java.util.logging.Level
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for LoggerMessageCommand functionality.
 * Verifies that all message events are properly logged.
 */
class LoggerMessageCommandTest : BaseLoggerMessageCommandTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(LoggerMessageCommand::class)
  }
    
  @Test
  fun `test ready event is logged`() {
    // Wait for processing
    env.awaitAll()

    // Verify that a ready event was logged
    val readyLogs = getLogsContaining("Ready event got")

    assertTrue(readyLogs.isNotEmpty(), "Ready event should be logged")
    assertEquals(Level.INFO, readyLogs.first().level, "Ready event should be logged at INFO level")
  }

  @Test
  fun `test message received event is logged`() {
    // Clear logs
    clearLogs()

    // Create user and send a message
    val user = env.createUser("Test User", false)
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Test message content"
    ).complete(true)!!

    // Wait for processing
    env.awaitAll()

    // Verify that the received message was logged
    val receivedLogs = getLogsContaining("Received")

    assertTrue(receivedLogs.isNotEmpty(), "Message received event should be logged")

    // Verify log contains message details
    val logMessage = receivedLogs.first().message
    assertTrue(logMessage.contains("Test User"), "Log should contain user name")
    assertTrue(logMessage.contains(message.jumpUrl), "Log should contain message URL")
  }

  @Test
  fun `test message update event is logged`() {
    // This is more complicated to test since TestMessage doesn't support updates natively
    // For now, we'll just trigger the event manually through the command

    // Clear logs
    clearLogs()

    // Get the LoggerMessageCommand instance from listeners
    val loggerCommand = env.jda.registeredListeners
      .filterIsInstance<LoggerMessageCommand>()
      .firstOrNull()

    // Create a test message
    val user = env.createUser("Test User", false)
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Original message content"
    ).complete(true)!!

    // Clear logs after message creation to isolate update logs
    clearLogs()

    // Simulate message update event
    val updateEvent = net.dv8tion.jda.api.events.message.MessageUpdateEvent(
      env.jda, 0, message
    )
    loggerCommand?.onMessageUpdate(updateEvent)

    // Wait for processing
    env.awaitAll()

    // Verify that the update message was logged
    val updateLogs = getLogsContaining("Update")

    assertTrue(updateLogs.isNotEmpty(), "Message update event should be logged")

    // Verify log contains message details
    val logMessage = updateLogs.first().message
    assertTrue(logMessage.contains("Test User"), "Log should contain user name")
    assertTrue(logMessage.contains(message.jumpUrl), "Log should contain message URL")
  }

  @Test
  fun `test message delete event is logged`() {
    // Clear logs
    clearLogs()

    // Create user and a message to delete
    val user = env.createUser("Test User", false)
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message to be deleted"
    ).complete(true)!!

    // Clear logs after message creation to isolate deletion logs
    clearLogs()

    // Delete the message
    message.delete().queue()

    // Wait for processing
    env.awaitAll()

    // Verify that the deletion was logged
    val deleteLogs = getLogsContaining("Delete message with id")

    assertTrue(deleteLogs.isNotEmpty(), "Message delete event should be logged")

    // Verify log contains message ID
    val logMessage = deleteLogs.first().message
    assertTrue(logMessage.contains(message.id), "Log should contain message ID")
  }
}