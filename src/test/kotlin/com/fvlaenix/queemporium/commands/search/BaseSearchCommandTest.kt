package com.fvlaenix.queemporium.commands.search

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.service.SearchService
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import org.koin.dsl.module
import kotlin.reflect.KClass

/**
 * Base abstract class for testing commands that use the SearchService.
 * Provides convenient methods for test environment setup, mocks configuration, and helper functions.
 */
abstract class BaseSearchCommandTest : BaseKoinTest() {

  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var mockSearchService: MockSearchService
  protected lateinit var answerService: MockAnswerService
  protected lateinit var koin: Koin

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var testUser: User

  /**
   * Standard test environment setup
   */
  @BeforeEach
  fun baseSetUp() {
    // Initialize services
    answerService = MockAnswerService()
    mockSearchService = createMockSearchService()

    // Setup Koin and commands
    koin = setupBotKoin {
      this.answerService = this@BaseSearchCommandTest.answerService
      enableCommands(*getCommandsForTest())
    }

    // Register mock service
    registerMockServiceInKoin(koin, mockSearchService)

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
    // Does nothing by default, should be overridden in specific tests
  }

  /**
   * Returns a list of command classes to be activated for the test
   */
  protected abstract fun getCommandsForTest(): Array<KClass<*>>

  /**
   * Creates and configures MockSearchService
   */
  protected fun createMockSearchService(
    configure: MockSearchService.() -> Unit = {}
  ): MockSearchService {
    val service = MockSearchService()
    service.configure()
    return service
  }

  /**
   * Registers mock service in Koin
   */
  protected fun registerMockServiceInKoin(
    koin: Koin,
    service: SearchService
  ) {
    koin.loadModules(listOf(module {
      single<SearchService> { service }
    }))
  }

  /**
   * Sends a message with the search command and an image attachment
   */
  protected fun sendSearchMessage(
    channelName: String = defaultGeneralChannelName,
    commandText: String = "/shogun-sama search",
    fileName: String = "test_image.jpg",
    width: Int = 100,
    height: Int = 100
  ): Message {
    val attachment = createTestAttachment(
      fileName = fileName,
      width = width,
      height = height
    )

    val result = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      commandText,
      listOf(attachment)
    )

    // Wait for all asynchronous operations to complete
    env.awaitAll()

    return result.complete(true)!!
  }

  /**
   * Sends a message with the search command and multiple image attachments
   */
  protected fun sendSearchMessageWithMultipleImages(
    channelName: String = defaultGeneralChannelName,
    commandText: String = "/shogun-sama search",
    fileConfigs: List<ImageConfig>
  ): Message {
    val attachments = fileConfigs.map { config ->
      createTestAttachment(
        fileName = config.fileName,
        width = config.width,
        height = config.height
      )
    }

    val result = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      commandText,
      attachments
    )

    // Wait for all asynchronous operations to complete
    env.awaitAll()

    return result.complete(true)!!
  }

  /**
   * Configuration class for creating test images
   */
  protected data class ImageConfig(
    val fileName: String,
    val width: Int = 100,
    val height: Int = 100
  )
}
