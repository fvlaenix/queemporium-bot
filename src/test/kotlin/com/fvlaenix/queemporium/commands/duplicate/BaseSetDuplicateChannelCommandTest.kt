package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.SetDuplicateChannelCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin

/**
 * Base test class for SetDuplicateChannelCommand tests.
 * Provides common setup for testing the duplicate channel setting functionality.
 */
abstract class BaseSetDuplicateChannelCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var answerService: MockAnswerService
  protected lateinit var koin: Koin
  protected lateinit var guildInfoConnector: GuildInfoConnector

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var adminUser: User
  protected lateinit var regularUser: User

  /**
   * Standard test environment setup that runs before each test
   */
  @BeforeEach
  fun baseSetUp() {
    // Initialize services
    answerService = MockAnswerService()

    // Setup Koin with SetDuplicateChannelCommand
    koin = setupBotKoin {
      this.answerService = this@BaseSetDuplicateChannelCommandTest.answerService
      enableCommands(SetDuplicateChannelCommand::class)
    }

    // Setup database
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)

    // Create test environment
    env = createEnvironment {
      createGuild(defaultGuildName) {
        withChannel(defaultGeneralChannelName)
      }
    }

    // Get references to frequently used objects
    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()
    generalChannel = testGuild.getTextChannelsByName(defaultGeneralChannelName, false).first()

    // Create users (admin and regular)
    adminUser = env.createUser("Admin User", false)
    regularUser = env.createUser("Regular User", false)

    env.createMember(testGuild, adminUser, isAdmin = true)
    env.createMember(testGuild, regularUser, isAdmin = false)
  }
}