package com.fvlaenix.queemporium.commands.logging

import ch.qos.logback.classic.Level
import com.fvlaenix.queemporium.commands.LoggerMessageCommand
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerMessageCommandTest : BaseLoggerMessageCommandTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(LoggerMessageCommand::class)
  }

  @Test
  fun `test ready event is logged`() {
    runWithScenario {
      awaitAll()
    }

    val readyLogs = getLogsContaining("Ready event got")

    assertTrue(readyLogs.isNotEmpty(), "Ready event should be logged")
    assertEquals(Level.INFO, readyLogs.first().level, "Ready event should be logged at INFO level")
  }

  @Test
  fun `test message received event is logged`() {
    clearLogs()

    val user = env.createUser("Test User", false)
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Test message content"
    ).complete(true)!!

    runWithScenario {
      awaitAll()
    }

    val receivedLogs = getLogsContaining("Received")

    assertTrue(receivedLogs.isNotEmpty(), "Message received event should be logged")

    val logMessage = receivedLogs.first().formattedMessage
    assertTrue(logMessage.contains("Test User"), "Log should contain user name")
    assertTrue(logMessage.contains(message.jumpUrl), "Log should contain message URL")
  }

  @Test
  fun `test message update event is logged`() {
    clearLogs()

    val loggerCommand = jda.registeredListeners
      .filterIsInstance<LoggerMessageCommand>()
      .firstOrNull()

    val user = env.createUser("Test User", false)
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Original message content"
    ).complete(true)!!

    clearLogs()

    val updateEvent = net.dv8tion.jda.api.events.message.MessageUpdateEvent(
      jda, 0, message
    )
    loggerCommand?.onMessageUpdate(updateEvent)

    runWithScenario {
      awaitAll()
    }

    val updateLogs = getLogsContaining("Update")

    assertTrue(updateLogs.isNotEmpty(), "Message update event should be logged")

    val logMessage = updateLogs.first().formattedMessage
    assertTrue(logMessage.contains("Test User"), "Log should contain user name")
    assertTrue(logMessage.contains(message.jumpUrl), "Log should contain message URL")
  }

  @Test
  fun `test message delete event is logged`() {
    clearLogs()

    val user = env.createUser("Test User", false)
    val message = env.sendMessage(
      "Test Guild",
      "general",
      user,
      "Message to be deleted"
    ).complete(true)!!

    clearLogs()

    message.delete().queue()

    runWithScenario {
      awaitAll()
    }

    val deleteLogs = getLogsContaining("Delete message with id")

    assertTrue(deleteLogs.isNotEmpty(), "Message delete event should be logged")

    val logMessage = deleteLogs.first().formattedMessage
    assertTrue(logMessage.contains(message.id), "Log should contain message ID")
  }
}
