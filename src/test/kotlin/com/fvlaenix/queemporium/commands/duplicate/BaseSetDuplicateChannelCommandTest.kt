package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.dsl.*
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base test class for SetDuplicateChannelCommand tests.
 * Provides common setup for testing the duplicate channel setting functionality.
 */
abstract class BaseSetDuplicateChannelCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var fixture: BotTestFixture
  protected lateinit var answerService: MockAnswerService
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
  fun baseSetUp(): Unit = runTest {
    answerService = MockAnswerService()

    fixture = testBotFixture {
      withAnswerService(this@BaseSetDuplicateChannelCommandTest.answerService)

      before {
        enableFeatures(FeatureKeys.SET_DUPLICATE_CHANNEL)

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
        }
      }
    }

    fixture.initialize(this@BaseSetDuplicateChannelCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)

    testGuild = GuildResolver.resolve(env.jda, defaultGuildName)
    generalChannel = ChannelResolver.resolve(testGuild, defaultGeneralChannelName)

    adminUser = env.createUser("Admin User", false)
    regularUser = env.createUser("Regular User", false)

    env.createMember(testGuild, adminUser, isAdmin = true)
    env.createMember(testGuild, regularUser, isAdmin = false)
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
  }

  protected fun <T> runWithScenario(block: suspend BotTestScenarioContext.() -> T): T {
    var result: T? = null
    runTest {
      fixture.runScenario {
        result = block()
      }
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
  }

  protected val env: TestEnvironment
    get() = runWithScenario { envWithTime.environment }
}
