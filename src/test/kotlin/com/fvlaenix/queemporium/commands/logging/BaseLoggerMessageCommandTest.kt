package com.fvlaenix.queemporium.commands.logging

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.LoggerMessageCommand
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * Base test class for LoggerMessageCommand tests.
 * Provides common setup and utilities for testing message logging functionality.
 */
abstract class BaseLoggerMessageCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var koin: Koin

  // Custom log handler to capture log messages
  protected val logRecords = mutableListOf<LogRecord>()
  protected lateinit var logHandler: TestLogHandler
  protected lateinit var logger: Logger

  // Flag for automatic environment start
  protected open var autoStartEnvironment: Boolean = true

  /**
   * Standard test environment setup
   */
  @BeforeEach
  fun baseSetUp() {
    // Setup Koin with the commands for this test
    koin = setupBotKoin {
      enableCommands(*getCommandsForTest())
    }

    // Setup logger and handler
    logHandler = TestLogHandler()
    logger = Logger.getLogger(LoggerMessageCommand::class.java.name)
    logger.addHandler(logHandler)
    logger.level = Level.ALL

    // Create test environment
    env = createEnvironment(autoStart = autoStartEnvironment) {
      createGuild("Test Guild") {
        withChannel("general")
      }
    }

    // Additional setup specific to concrete test
    additionalSetUp()
  }

  /**
   * Method to be overridden in subclasses for additional setup
   */
  protected open fun additionalSetUp() {
    // Does nothing by default, can be overridden in specific tests
  }

  /**
   * Starts the test environment if not already started
   */
  protected fun startEnvironment() {
    env.start()
  }

  /**
   * Returns a list of command classes to be activated for the test
   */
  protected abstract fun getCommandsForTest(): Array<KClass<*>>

  /**
   * Custom log handler to capture log messages
   */
  protected inner class TestLogHandler : Handler() {
    override fun publish(record: LogRecord) {
      logRecords.add(record)
    }

    override fun flush() {
      // No-op
    }

    override fun close() {
      // No-op
    }
  }

  /**
   * Clears captured log records
   */
  protected fun clearLogs() {
    logRecords.clear()
  }

  /**
   * Gets logs containing the specified text
   */
  protected fun getLogsContaining(text: String): List<LogRecord> {
    return logRecords.filter { it.message.contains(text) }
  }
}