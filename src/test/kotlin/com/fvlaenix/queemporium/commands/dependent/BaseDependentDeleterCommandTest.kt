package com.fvlaenix.queemporium.commands.dependent

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDependency
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.testing.dsl.*
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base test class for DependentDeleterCommand tests.
 * Provides common setup and utility methods for testing the dependent message deletion functionality.
 */
abstract class BaseDependentDeleterCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var fixture: BotTestFixture
  protected lateinit var messageDataConnector: MessageDataConnector
  protected lateinit var messageDependencyConnector: MessageDependencyConnector

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var testUser: User

  /**
   * Standard test environment setup that runs before each test
   */
  @BeforeEach
  fun baseSetUp() = runTest {
    fixture = testBotFixture {
      before {
        enableFeatures(FeatureKeys.DEPENDENT_DELETER, FeatureKeys.MESSAGES_STORE)

        user("Test User")

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
        }
      }
    }

    fixture.initialize(this@BaseDependentDeleterCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    messageDataConnector = MessageDataConnector(database)
    messageDependencyConnector = MessageDependencyConnector(database)

    testGuild = GuildResolver.resolve(env.jda, defaultGuildName)
    generalChannel = ChannelResolver.resolve(testGuild, defaultGeneralChannelName)
    testUser = env.createUser("Test User", false)

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
    // Does nothing by default, can be overridden in specific tests
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

  /**
   * Helper method to create a message and store it in the database
   */
  protected fun createAndStoreMessage(
    channelName: String = defaultGeneralChannelName,
    messageText: String = "Test message"
  ): Message {
    // Send a message
    val message = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      messageText
    ).complete(true)

    env.awaitAll()

    return requireNotNull(message) { "Failed to create message in $channelName" }
  }

  /**
   * Helper method to create a dependency between messages
   */
  protected fun createDependency(targetMessageId: String, dependentMessageId: String) {
    messageDependencyConnector.addDependency(
      MessageDependency(
        targetMessage = targetMessageId,
        dependentMessage = dependentMessageId
      )
    )
  }

  protected fun deleteMessage(message: Message) {
    message.guild
      .getTextChannelById(message.channelId)!!
      .deleteMessageById(message.id).submit()
    env.awaitAll()
  }
}
