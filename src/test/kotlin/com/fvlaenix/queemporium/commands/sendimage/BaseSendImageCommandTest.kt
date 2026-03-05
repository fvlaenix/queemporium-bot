package com.fvlaenix.queemporium.commands.sendimage

import com.fvlaenix.queemporium.database.ImageMappingConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.MockS3FileService
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.service.S3FileResult
import com.fvlaenix.queemporium.service.S3FileService
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

abstract class BaseSendImageCommandTest : BaseKoinTest() {
  protected lateinit var fixture: BotTestFixture
  protected lateinit var mockImageMappingConnector: ImageMappingConnector
  protected lateinit var mockS3FileService: MockS3FileService
  protected lateinit var answerService: MockAnswerService
  private val s3ExceptionsByPath = mutableMapOf<String, Exception>()

  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  @BeforeEach
  fun baseSetUp() = runTest {
    mockS3FileService = createMockS3FileService()

    fixture = testBotFixture {
      before {
        enableFeatures(*getFeaturesForTest())

        user("Test User")

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
        }
      }

      registerModuleBeforeFeatureLoad(module {
        single<S3FileService> {
          object : S3FileService {
            override suspend fun fetchFile(s3Path: String): S3FileResult {
              val configuredException = s3ExceptionsByPath[s3Path]
              if (configuredException != null) {
                throw configuredException
              }
              return mockS3FileService.fetchFile(s3Path)
            }
          }
        }
      })
    }

    fixture.initialize(this@BaseSendImageCommandTest)

    answerService = runWithScenario { answerService!! }

    val databaseConfig =
      org.koin.core.context.GlobalContext.get().get<com.fvlaenix.queemporium.configuration.DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    mockImageMappingConnector = ImageMappingConnector(database)

    additionalSetUp()
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
  }

  protected open fun additionalSetUp() {
  }

  protected abstract fun getFeaturesForTest(): Array<String>

  protected fun createMockS3FileService(
    configure: MockS3FileService.() -> Unit = {}
  ): MockS3FileService {
    val service = MockS3FileService()
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

  protected fun sendImageMessage(
    channelName: String = defaultGeneralChannelName,
    key: String
  ): Message {
    val result = env.sendMessage(
      defaultGuildName,
      channelName,
      testUser,
      "/shogun-sama image $key",
      emptyList()
    )

    runWithScenario {
      awaitAll()
    }

    return requireNotNull(result.complete(true)) { "Failed to send image message to $channelName" }
  }

  protected fun sendImageDirectMessage(key: String): Message {
    val dmUser = env.createUser("DM User", false)
    val result = env.sendDirectMessage(dmUser, "/shogun-sama image $key")

    runWithScenario {
      awaitAll()
    }

    return requireNotNull(result.complete(true)) { "Failed to send direct image message" }
  }

  protected fun setS3ExceptionForPath(s3Path: String, exception: Exception) {
    s3ExceptionsByPath[s3Path] = exception
  }
}
