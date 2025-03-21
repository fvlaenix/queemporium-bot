package com.fvlaenix.queemporium.commands.authorMapping

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.AuthorMappingCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.CorrectAuthorMappingConnector
import com.fvlaenix.queemporium.database.CorrectAuthorMappingTable
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin

/**
 * Base test class for AuthorMappingCommand tests.
 * Provides common setup and utilities for testing author mapping functionality.
 */
abstract class BaseAuthorMappingCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var mockAnswerService: MockAnswerService
  protected lateinit var koin: Koin
  protected lateinit var guildInfoConnector: GuildInfoConnector
  protected lateinit var authorMappingConnector: CorrectAuthorMappingConnector

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"
  protected val defaultDuplicateChannelName = "duplicate-channel"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var duplicateChannel: TextChannel
  protected lateinit var testUser: User

  /**
   * Standard test environment setup that runs before each test
   */
  @BeforeEach
  fun baseSetUp() {
    // Initialize services
    mockAnswerService = MockAnswerService()

    // Setup Koin and commands
    koin = setupBotKoin {
      this.answerService = this@BaseAuthorMappingCommandTest.mockAnswerService
      enableCommands(AuthorMappingCommand::class)
    }

    // Setup database
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)
    authorMappingConnector = CorrectAuthorMappingConnector(database)

    // Create test environment
    env = createEnvironment {
      createGuild(defaultGuildName) {
        withChannel(defaultGeneralChannelName)
        withChannel(defaultDuplicateChannelName)
      }
    }

    // Get references to frequently used objects
    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()
    generalChannel = testGuild.getTextChannelsByName(defaultGeneralChannelName, false).first()
    duplicateChannel = testGuild.getTextChannelsByName(defaultDuplicateChannelName, false).first()

    // Configure duplicate channel
    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    // Create test user
    testUser = env.createUser("Test User", false)
  }

  /**
   * Helper method to add an author mapping to the database
   */
  protected fun addAuthorMapping(incorrectNames: List<String>, correctNames: List<String>) {
    transaction(authorMappingConnector.database) {
      CorrectAuthorMappingTable.insert {
        it[from] = incorrectNames.joinToString("/")
        it[to] = correctNames.joinToString("/")
      }
    }
  }

  /**
   * Helper method to send a message with an attachment
   */
  protected fun sendMessageWithAttachment(
    channelName: String = defaultGeneralChannelName,
    messageText: String,
    fileName: String = "test_image.jpg"
  ): Message {
    val result = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      messageText,
      listOf(createTestAttachment(fileName))
    )

    // Wait for processing
    env.awaitAll()

    return result.complete(true)!!
  }
}