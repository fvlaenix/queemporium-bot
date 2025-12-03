package com.fvlaenix.queemporium.commands.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.LoggerMessageCommand
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import org.slf4j.LoggerFactory
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
  protected lateinit var listAppender: ListAppender<ILoggingEvent>
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
    logger = LoggerFactory.getLogger(LoggerMessageCommand::class.java) as Logger
    listAppender = ListAppender()
    listAppender.start()
    logger.addAppender(listAppender)

    // Create test environment
    env = createEnvironment(autoStart = autoStartEnvironment) {
      createGuild("Test Guild") {
        withChannel("general")
      }
    }

    // Additional setup specific to concrete test
    additionalSetUp()
  }

  @AfterEach
  fun tearDown() {
    logger.detachAppender(listAppender)
    listAppender.stop()
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
   * Clears captured log records
   */
  protected fun clearLogs() {
    listAppender.list.clear()
  }

  /**
   * Gets logs containing the specified text
   */
  protected fun getLogsContaining(text: String): List<ILoggingEvent> {
    return listAppender.list.filter { it.formattedMessage.contains(text) }
  }
}
