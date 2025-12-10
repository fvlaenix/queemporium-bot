package com.fvlaenix.queemporium.commands.authorCollector

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.AuthorDataConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.testing.dsl.BotTestFixture
import com.fvlaenix.queemporium.testing.dsl.BotTestScenarioContext
import com.fvlaenix.queemporium.testing.dsl.GuildResolver
import com.fvlaenix.queemporium.testing.dsl.testBotFixture
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base test class for AuthorCollectCommand tests.
 * Provides common setup and utilities for testing author collection functionality.
 */
abstract class BaseAuthorCollectCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var fixture: BotTestFixture
  protected lateinit var authorDataConnector: AuthorDataConnector

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var testUsers: List<User>

  /**
   * Standard test environment setup that runs before each test
   */
  @BeforeEach
  fun baseSetUp() = runBlocking {
    fixture = testBotFixture {
      before {
        enableFeatures(FeatureKeys.AUTHOR_COLLECT)

        guild(defaultGuildName)
      }
    }

    fixture.autoStart = false
    fixture.initialize(this@BaseAuthorCollectCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    authorDataConnector = AuthorDataConnector(database)

    testGuild = GuildResolver.resolve(env.jda, defaultGuildName)

    testUsers = (1..5).map { i -> env.createUser("TestUser$i", false) }

    testUsers.forEach { user ->
      env.createMember(testGuild, user)
    }

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
   * Helper method to start the test environment
   */
  protected fun startEnvironment() {
    runWithScenario {
      envWithTime.environment.start()
    }
  }
}
