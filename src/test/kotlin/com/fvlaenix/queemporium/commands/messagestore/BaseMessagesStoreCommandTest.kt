package com.fvlaenix.queemporium.commands.messagestore

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.MessagesStoreCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin

/**
 * Base abstract class for testing MessagesStoreCommand.
 * Provides common setup and utility methods for testing message storage functionality.
 */
abstract class BaseMessagesStoreCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var koin: Koin
  protected lateinit var messageDataConnector: MessageDataConnector

  // Flag for automatic environment start
  protected open var autoStartEnvironment: Boolean = true

  /**
   * Standard test environment setup that runs before each test
   */
  @BeforeEach
  fun baseSetUp() {
    // Setup Koin with the MessagesStoreCommand
    koin = setupBotKoin {
      enableCommands(MessagesStoreCommand::class)
    }

    // Setup database and connector
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    messageDataConnector = MessageDataConnector(database)

    // Create test environment with configurable auto-start
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
    // Does nothing by default, should be overridden in specific tests
  }

  /**
   * Starts the test environment if not already started
   */
  protected fun startEnvironment() {
    env.start()
  }

  /**
   * Cleans up the database by deleting all message records
   */
  protected fun cleanupDatabase(messageIds: List<String>) {
    messageIds.forEach { messageId ->
      messageDataConnector.delete(messageId)
    }
  }
}