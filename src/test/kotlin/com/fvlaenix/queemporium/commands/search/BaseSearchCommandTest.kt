package com.fvlaenix.queemporium.commands.search

import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.service.SearchService
import com.fvlaenix.queemporium.testing.dsl.*
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.dsl.module

abstract class BaseSearchCommandTest : BaseKoinTest() {
  protected lateinit var fixture: BotTestFixture
  protected lateinit var mockSearchService: MockSearchService
  protected lateinit var answerService: MockAnswerService

  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  @BeforeEach
  fun baseSetUp() = runTest {
    mockSearchService = createMockSearchService()

    fixture = testBotFixture {
      before {
        enableFeatures(*getFeaturesForTest())

        user("Test User")

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
        }
      }

      registerModuleBeforeFeatureLoad(module {
        single<SearchService> { mockSearchService }
      })
    }

    fixture.initialize(this@BaseSearchCommandTest)

    answerService = runWithScenario { answerService!! }

    additionalSetUp()
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
  }

  protected open fun additionalSetUp() {
  }

  protected abstract fun getFeaturesForTest(): Array<String>

  protected fun createMockSearchService(
    configure: MockSearchService.() -> Unit = {}
  ): MockSearchService {
    val service = MockSearchService()
    service.configure()
    return service
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

  protected val jda: JDA
    get() = env.jda

  protected val testGuild: Guild
    get() = GuildResolver.resolve(jda, defaultGuildName)

  protected val generalChannel: TextChannel
    get() = ChannelResolver.resolve(testGuild, defaultGeneralChannelName)

  protected val testUser: User
    get() = jda.getUsersByName("Test User", false).firstOrNull() ?: env.createUser("Test User", false)

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

    runWithScenario {
      awaitAll()
    }

    return requireNotNull(result.complete(true)) { "Failed to send search message to $channelName" }
  }

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

    runWithScenario {
      awaitAll()
    }

    return requireNotNull(result.complete(true)) { "Failed to send search message with multiple images to $channelName" }
  }

  protected data class ImageConfig(
    val fileName: String,
    val width: Int = 100,
    val height: Int = 100
  )
}
