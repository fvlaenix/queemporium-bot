package com.fvlaenix.queemporium.commands.authorMapping

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.CorrectAuthorMappingConnector
import com.fvlaenix.queemporium.database.CorrectAuthorMappingTable
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.dsl.*
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Base test class for AuthorMappingCommand tests.
 * Provides common setup and utilities for testing author mapping functionality.
 */
abstract class BaseAuthorMappingCommandTest : BaseKoinTest() {
  // Core components for testing
  protected lateinit var fixture: BotTestFixture
  protected lateinit var mockAnswerService: MockAnswerService
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
  fun baseSetUp() = runTest {
    mockAnswerService = MockAnswerService()

    fixture = testBotFixture {
      withAnswerService(this@BaseAuthorMappingCommandTest.mockAnswerService)

      before {
        enableFeatures(FeatureKeys.AUTHOR_MAPPING)

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
          channel(defaultDuplicateChannelName)
        }
      }
    }

    fixture.initialize(this@BaseAuthorMappingCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)
    authorMappingConnector = CorrectAuthorMappingConnector(database)

    testGuild = GuildResolver.resolve(env.jda, defaultGuildName)
    generalChannel = ChannelResolver.resolve(testGuild, defaultGeneralChannelName)
    duplicateChannel = ChannelResolver.resolve(testGuild, defaultDuplicateChannelName)

    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    testUser = env.createUser("Test User", false)
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

    return requireNotNull(result.complete(true)) { "sendMessage should return a Message for $channelName" }
  }
}
