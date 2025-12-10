package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.MockDuplicateImageService
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.dsl.*
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.dsl.module

abstract class BaseDuplicateCommandTest : BaseKoinTest() {
  protected lateinit var fixture: BotTestFixture
  protected lateinit var mockDuplicateService: MockDuplicateImageService
  protected lateinit var answerService: MockAnswerService
  protected lateinit var guildInfoConnector: GuildInfoConnector

  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"
  protected val defaultDuplicateChannelName = "duplicate-channel"

  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var duplicateChannel: TextChannel
  protected lateinit var testUser: User

  protected open var autoStartEnvironment: Boolean = true

  @BeforeEach
  fun baseSetUp() = runBlocking {
    mockDuplicateService = createMockDuplicateService()

    fixture = testBotFixture {
      before {
        enableFeatures(*getFeaturesForTest())

        user("Test User")

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
          channel(defaultDuplicateChannelName)
        }
      }

      registerModuleBeforeFeatureLoad(module {
        single<DuplicateImageService> { mockDuplicateService }
      })
    }

    fixture.autoStart = autoStartEnvironment
    fixture.initialize(this@BaseDuplicateCommandTest)

    answerService = runWithScenario { answerService!! }

    val databaseConfig =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.configuration.DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    guildInfoConnector = GuildInfoConnector(database)

    testGuild = GuildResolver.resolve(env.jda, defaultGuildName)
    generalChannel = ChannelResolver.resolve(testGuild, defaultGeneralChannelName)
    duplicateChannel = ChannelResolver.resolve(testGuild, defaultDuplicateChannelName)

    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    testUser = env.createUser("Test User", false)

    additionalSetUp()
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
  }

  protected open fun additionalSetUp() {
  }

  protected fun startEnvironment() {
    runWithScenario {
      envWithTime.environment.start()
    }
  }

  protected abstract fun getFeaturesForTest(): Array<String>

  protected fun createMockDuplicateService(
    configure: MockDuplicateImageService.() -> Unit = {}
  ): MockDuplicateImageService {
    val service = MockDuplicateImageService()
    service.isServerAlive = true
    service.defaultCompressSize = CompressSize(width = 100, height = null)
    service.configure()
    return service
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

    env.awaitAll()

    return result.complete(true)!!
  }

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

    env.awaitAll()

    return result.complete(true)!!
  }

  protected fun createMessageChain(
    originalText: String = "Original message",
    originalFileName: String = "original.jpg",
    duplicateText: String = "Duplicate of the original message",
    duplicateFileName: String = "duplicate.jpg"
  ): Pair<Message, Message> {
    val originalMessage = sendMessageWithImage(
      messageText = originalText,
      fileName = originalFileName
    )

    env.awaitAll()

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

    val duplicateMessage = sendMessageWithImage(
      messageText = duplicateText,
      fileName = duplicateFileName
    )

    env.awaitAll()

    return originalMessage to duplicateMessage
  }

  protected data class ImageConfig(
    val fileName: String,
    val width: Int = 100,
    val height: Int = 100,
    val isSpoiler: Boolean = false
  )
}
