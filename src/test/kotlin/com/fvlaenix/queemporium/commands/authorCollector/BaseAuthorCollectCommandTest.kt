package com.fvlaenix.queemporium.commands.authorCollector

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.AuthorCollectCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.AuthorDataConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin

/**
 * Base test class for AuthorCollectCommand tests.
 * Provides common setup and utilities for testing author collection functionality.
 */
abstract class BaseAuthorCollectCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var koin: Koin
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
  fun baseSetUp() {
    // Setup Koin with the commands for this test
    koin = setupBotKoin {
      enableCommands(AuthorCollectCommand::class)
    }

    // Setup database and connectors
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    authorDataConnector = AuthorDataConnector(database)

    // Create test environment
    env = createEnvironment(autoStart = false) {
      createGuild(defaultGuildName)
    }

    // Get references to frequently used objects
    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()

    // Create test users
    testUsers = (1..5).map { i -> env.createUser("TestUser$i", false) }

    // Register users as members of the guild
    testUsers.forEach { user ->
      env.createMember(testGuild, user)
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
   * Helper method to start the test environment
   */
  protected fun startEnvironment() {
    env.start()
  }
}