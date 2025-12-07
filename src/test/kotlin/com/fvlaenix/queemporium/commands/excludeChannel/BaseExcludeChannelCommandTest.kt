package com.fvlaenix.queemporium.commands.excludeChannel

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.dsl.BotTestFixture
import com.fvlaenix.queemporium.testing.dsl.BotTestScenarioContext
import com.fvlaenix.queemporium.testing.dsl.testBotFixture
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base test class for ExcludeChannelCommand tests.
 * Provides common setup and utility methods for testing the exclude channel functionality.
 */
abstract class BaseExcludeChannelCommandTest : BaseKoinTest() {
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
  fun baseSetUp(): Unit = runBlocking {
    answerService = MockAnswerService()

    fixture = testBotFixture {
      withAnswerService(this@BaseExcludeChannelCommandTest.answerService)

      before {
        enableFeatures(FeatureKeys.EXCLUDE_CHANNEL)

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
        }
      }
    }

    fixture.initialize(this@BaseExcludeChannelCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)

    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()
    generalChannel = testGuild.getTextChannelsByName(defaultGeneralChannelName, false).first()

    adminUser = env.createUser("Admin User", false)
    regularUser = env.createUser("Regular User", false)

    env.createMember(testGuild, adminUser, isAdmin = true)
    env.createMember(testGuild, regularUser, isAdmin = false)
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
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
   * Helper method to send a command message with admin or non-admin privileges
   */
  protected fun sendCommand(
    channelName: String,
    command: String,
    isAdmin: Boolean
  ): Message {
    val user = if (isAdmin) adminUser else regularUser

    // Create a message with the given command
    val result = env.sendMessage(
      guildName = defaultGuildName,
      channelName = channelName,
      user = user,
      message = command
    )

    // Set the member on the message
    val message = result.complete(true)!!

    // Wait for processing
    env.awaitAll()

    return message
  }

  /**
   * Helper method to simulate a direct message (not in a guild)
   */
  protected fun sendDirectMessage(command: String): Message {
    // Create a special user for direct messages
    val dmUser = env.createUser("DM User", false)

    // Send the direct message
    val result = env.sendDirectMessage(dmUser, command)
    val message = result.complete(true)!!

    // Wait for processing
    env.awaitAll()

    return message
  }
}