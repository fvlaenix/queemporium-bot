package com.fvlaenix.queemporium.commands.dependent

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.DependentDeleterCommand
import com.fvlaenix.queemporium.commands.MessagesStoreCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageDependency
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin

/**
 * Base test class for DependentDeleterCommand tests.
 * Provides common setup and utility methods for testing the dependent message deletion functionality.
 */
abstract class BaseDependentDeleterCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var answerService: MockAnswerService
  protected lateinit var koin: Koin
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
  fun baseSetUp() {
    // Initialize services
    answerService = MockAnswerService()

    // Setup Koin with DependentDeleterCommand
    koin = setupBotKoin {
      this.answerService = this@BaseDependentDeleterCommandTest.answerService
      enableCommands(DependentDeleterCommand::class, MessagesStoreCommand::class)
    }

    // Setup database and connectors
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    messageDataConnector = MessageDataConnector(database)
    messageDependencyConnector = MessageDependencyConnector(database)

    // Create test environment
    env = createEnvironment {
      createGuild(defaultGuildName) {
        withChannel(defaultGeneralChannelName)
      }
    }

    // Get references to frequently used objects
    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()
    generalChannel = testGuild.getTextChannelsByName(defaultGeneralChannelName, false).first()

    // Create test user
    testUser = env.createUser("Test User", false)

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
    ).complete(true)!!

    env.awaitAll()

    return message
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