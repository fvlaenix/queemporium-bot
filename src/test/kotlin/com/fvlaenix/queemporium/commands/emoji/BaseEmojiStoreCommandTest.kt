package com.fvlaenix.queemporium.commands.emoji

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.commands.LongTermEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.configuration.commands.OnlineEmojiesStoreCommandConfig
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.database.MessageEmojiDataConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEmoji
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.TestMessage
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import org.koin.dsl.module
import kotlin.reflect.KClass

/**
 * Base test class for emoji store commands.
 * Provides common setup and utilities for testing OnlineEmojiesStoreCommand and LongTermEmojiesStoreCommand
 */
abstract class BaseEmojiStoreCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var koin: Koin
  protected lateinit var messageDataConnector: MessageDataConnector
  protected lateinit var messageEmojiDataConnector: MessageEmojiDataConnector
  protected lateinit var emojiDataConnector: EmojiDataConnector

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var testUsers: List<User>

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

    // Register configs for emoji commands
    registerEmojiCommandConfigs(koin)

    // Setup database
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    messageDataConnector = MessageDataConnector(database)
    messageEmojiDataConnector = MessageEmojiDataConnector(database)
    emojiDataConnector = EmojiDataConnector(database)

    // Create test environment
    env = createEnvironment(autoStart = autoStartEnvironment) {
      createGuild(defaultGuildName) {
        withChannel(defaultGeneralChannelName)
      }
    }

    // Get references to frequently used objects
    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()
    generalChannel = testGuild.getTextChannelsByName(defaultGeneralChannelName, false).first()

    // Create test users
    testUsers = (1..5).map { i -> env.createUser("TestUser$i", false) }

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
   * Returns a list of command classes to be activated for the test
   */
  protected abstract fun getCommandsForTest(): Array<KClass<*>>

  /**
   * Registers emoji command configurations in Koin
   */
  private fun registerEmojiCommandConfigs(koin: Koin) {
    // Create configuration modules for both emoji commands
    val onlineEmojiConfigModule = module {
      single {
        OnlineEmojiesStoreCommandConfig(
          distanceInDays = 7,
          guildThreshold = 2,
          channelThreshold = 2,
          messageThreshold = 4,
          emojisThreshold = 4
        )
      }
    }

    val longTermEmojiConfigModule = module {
      single {
        LongTermEmojiesStoreCommandConfig(
          distanceInDays = 30,
          guildThreshold = 2,
          channelThreshold = 2,
          messageThreshold = 4,
          emojisThreshold = 4,
          isShuffle = false
        )
      }
    }

    // Load the modules into Koin
    koin.loadModules(listOf(onlineEmojiConfigModule, longTermEmojiConfigModule))
  }

  /**
   * Creates a message with reactions and waits for processing
   */
  protected fun createMessageWithReactions(
    channelName: String = defaultGeneralChannelName,
    messageText: String = "Test message with reactions",
    reactionConfig: List<ReactionConfig> = emptyList()
  ): Message {
    // Send the message
    val message = env.sendMessage(
      defaultGuildName,
      channelName,
      testUsers[0], // Author is the first test user
      messageText
    ).complete(true)!! as TestMessage

    // Add reactions according to configuration
    reactionConfig.forEach { config ->
      val emoji = TestEmoji(config.emojiName)
      config.userIndices.forEach { userIndex ->
        if (userIndex >= 0 && userIndex < testUsers.size) {
          message.addReaction(emoji, testUsers[userIndex])
        }
      }
    }

    // Wait for processing to complete
    env.awaitAll()

    return message
  }

  /**
   * Creates multiple messages with reactions for testing bulk behavior
   */
  protected fun createMultipleMessagesWithReactions(
    count: Int = 5,
    channelName: String = defaultGeneralChannelName,
    baseMessageText: String = "Test message",
    reactionConfigs: List<ReactionConfig> = listOf(
      ReactionConfig("👍", listOf(1, 2)), // Basic default reactions
      ReactionConfig("❤️", listOf(3, 4))
    )
  ): List<Message> {
    return (1..count).map { i ->
      createMessageWithReactions(
        channelName = channelName,
        messageText = "$baseMessageText $i",
        reactionConfig = reactionConfigs
      )
    }
  }

  /**
   * Configuration class for adding reactions
   */
  protected data class ReactionConfig(
    val emojiName: String,
    val userIndices: List<Int> // Indices of users from testUsers list
  )
}