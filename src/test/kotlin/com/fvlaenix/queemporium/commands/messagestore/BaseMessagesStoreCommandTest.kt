package com.fvlaenix.queemporium.commands.messagestore

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.testing.dsl.BotTestFixture
import com.fvlaenix.queemporium.testing.dsl.BotTestScenarioContext
import com.fvlaenix.queemporium.testing.dsl.testBotFixture
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base abstract class for testing MessagesStoreCommand.
 * Provides common setup and utility methods for testing message storage functionality.
 */
abstract class BaseMessagesStoreCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var fixture: BotTestFixture
  protected lateinit var messageDataConnector: MessageDataConnector

  // Flag for automatic environment start
  protected open var autoStartEnvironment: Boolean = true

  /**
   * Standard test environment setup that runs before each test
   */
  @BeforeEach
  fun baseSetUp() = runBlocking {
    fixture = testBotFixture {
      before {
        enableFeatures(FeatureKeys.MESSAGES_STORE)

        guild("Test Guild") {
          channel("general")
        }
      }
    }

    fixture.autoStart = autoStartEnvironment
    fixture.initialize(this@BaseMessagesStoreCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    messageDataConnector = MessageDataConnector(database)

    additionalSetUp()
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
  }

  /**
   * Method to be overridden in subclasses for additional setup
   */
  protected open fun additionalSetUp() {
    // Does nothing by default, should be overridden in specific tests
  }

  protected fun <T> runWithScenario(block: suspend BotTestScenarioContext.() -> T): T = runBlocking {
    var result: T? = null
    fixture.runScenario {
      result = block()
    }
    @Suppress("UNCHECKED_CAST")
    result as T
  }

  protected val env: TestEnvironment
    get() = runWithScenario { envWithTime.environment }

  /**
   * Starts the test environment if not already started
   */
  protected fun startEnvironment() {
    runWithScenario {
      envWithTime.environment.start()
    }
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