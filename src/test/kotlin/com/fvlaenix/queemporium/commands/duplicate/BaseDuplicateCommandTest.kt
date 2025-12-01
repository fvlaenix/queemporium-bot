package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.MockDuplicateImageService
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.service.MockAnswerService
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import org.koin.dsl.module
import kotlin.reflect.KClass

/**
 * Base abstract class for testing duplicate detection commands.
 * Provides convenient methods for test environment setup, mocks configuration, and helper functions.
 */
abstract class BaseDuplicateCommandTest : BaseKoinTest() {

  // Core components for testing
  protected lateinit var env: TestEnvironment
  protected lateinit var mockDuplicateService: MockDuplicateImageService
  protected lateinit var answerService: MockAnswerService
  protected lateinit var koin: Koin
  protected lateinit var guildInfoConnector: GuildInfoConnector

  // Default settings for test environment
  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"
  protected val defaultDuplicateChannelName = "duplicate-channel"

  // References to frequently used objects
  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var duplicateChannel: TextChannel
  protected lateinit var testUser: User

  protected open var autoStartEnvironment: Boolean = true

  /**
   * Standard test environment setup
   */
  @BeforeEach
  fun baseSetUp() {
    // Initialize services
    answerService = MockAnswerService()
    mockDuplicateService = createMockDuplicateService()

    // Setup Koin and commands
    koin = setupBotKoin {
      this.answerService = this@BaseDuplicateCommandTest.answerService
      enableCommands(*getCommandsForTest())
    }

    // Register mock service
    registerMockServiceInKoin(koin, mockDuplicateService)

    // Setup database
    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)

    // Create test environment
    env = createEnvironment(autoStart = autoStartEnvironment) {
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

    // Additional setup specific to concrete test
    additionalSetUp()
  }

  /**
   * Method to be overridden in subclasses for additional setup
   */
  protected open fun additionalSetUp() {
    // Does nothing by default, should be overridden in specific tests
  }

  protected fun startEnvironment() {
    env.start()
  }

  /**
   * Returns a list of command classes to be activated for the test
   */
  protected abstract fun getCommandsForTest(): Array<KClass<*>>

  /**
   * Creates and configures MockDuplicateImageService
   */
  protected fun createMockDuplicateService(
    configure: MockDuplicateImageService.() -> Unit = {}
  ): MockDuplicateImageService {
    val service = MockDuplicateImageService()
    service.isServerAlive = true
    service.defaultCompressSize = CompressSize(width = 100, height = null)
    service.configure()
    return service
  }

  /**
   * Registers mock service in Koin
   */
  protected fun registerMockServiceInKoin(
    koin: Koin,
    service: DuplicateImageService
  ) {
    koin.loadModules(listOf(module {
      single<DuplicateImageService> { service }
    }))
  }

  /**
   * Sends a message with an image and waits for the result
   */
  protected fun sendMessageWithImage(
    channelName: String = defaultGeneralChannelName,
    messageText: String = "Test message with image",
    fileName: String = "test_image.jpg",
    width: Int = 100,
    height: Int = 100,
    isSpoiler: Boolean = false
  ): Message {
    val result = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      messageText,
      listOf(createTestAttachment(fileName, width, height, isSpoiler))
    )

    // Wait for all asynchronous operations to complete
    env.awaitAll()

    return result.complete(true)!!
  }

  /**
   * Sends a message with multiple images and waits for the result
   */
  protected fun sendMessageWithMultipleImages(
    channelName: String = defaultGeneralChannelName,
    messageText: String = "Test message with multiple images",
    fileConfigs: List<ImageConfig>
  ): Message {
    val attachments = fileConfigs.map { config ->
      createTestAttachment(
        fileName = config.fileName,
        width = config.width,
        height = config.height,
        isSpoiler = config.isSpoiler
      )
    }

    val result = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      messageText,
      attachments
    )

    // Wait for all asynchronous operations to complete
    env.awaitAll()

    return result.complete(true)!!
  }

  /**
   * Creates a message chain - first message is an original, then send a duplicate of it
   */
  protected fun createMessageChain(
    originalText: String = "Original message",
    originalFileName: String = "original.jpg",
    duplicateText: String = "Duplicate of the original message",
    duplicateFileName: String = "duplicate.jpg"
  ): Pair<Message, Message> {
    // First send original message
    val originalMessage = sendMessageWithImage(
      messageText = originalText,
      fileName = originalFileName
    )

    // Wait for all processing to complete
    env.awaitAll()

    // Configure mock service to recognize the duplicate
    mockDuplicateService.setResponseForFile(
      duplicateFileName,
      listOf(
        DuplicateImageService.DuplicateImageData(
          messageId = originalMessage.id,
          numberInMessage = 0,
          additionalImageInfo = AdditionalImageInfo(
            fileName = originalFileName,
            isSpoiler = false,
            originalSizeWidth = 100,
            originalSizeHeight = 100
          ),
          level = 95
        )
      )
    )

    // Now send the duplicate message
    val duplicateMessage = sendMessageWithImage(
      messageText = duplicateText,
      fileName = duplicateFileName
    )

    // Wait for all processing to complete
    env.awaitAll()

    return originalMessage to duplicateMessage
  }

  /**
   * Configuration class for creating test images
   */
  protected data class ImageConfig(
    val fileName: String,
    val width: Int = 100,
    val height: Int = 100,
    val isSpoiler: Boolean = false
  )
}
