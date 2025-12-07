package com.fvlaenix.queemporium.commands.emoji

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
import com.fvlaenix.queemporium.testing.dsl.BotTestFixture
import com.fvlaenix.queemporium.testing.dsl.BotTestScenarioContext
import com.fvlaenix.queemporium.testing.dsl.testBotFixture
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.dsl.module

abstract class BaseEmojiStoreCommandTest : BaseKoinTest() {
  protected lateinit var fixture: BotTestFixture
  protected lateinit var messageDataConnector: MessageDataConnector
  protected lateinit var messageEmojiDataConnector: MessageEmojiDataConnector
  protected lateinit var emojiDataConnector: EmojiDataConnector

  protected val defaultGuildName = "Test Guild"
  protected val defaultGeneralChannelName = "general"

  protected lateinit var testGuild: Guild
  protected lateinit var generalChannel: TextChannel
  protected lateinit var testUsers: List<User>

  protected open var autoStartEnvironment: Boolean = true

  @BeforeEach
  fun baseSetUp() = runBlocking {
    fixture = testBotFixture {
      before {
        enableFeatures(*getFeatureKeysForTest())

        user("TestUser1")
        user("TestUser2")
        user("TestUser3")
        user("TestUser4")
        user("TestUser5")

        guild(defaultGuildName) {
          channel(defaultGeneralChannelName)
        }
      }

      registerModuleBeforeFeatureLoad(module {
        single {
          OnlineEmojiesStoreCommandConfig(
            distanceInDays = 7,
            guildThreshold = 2,
            channelThreshold = 2,
            messageThreshold = 4,
            emojisThreshold = 4
          )
        }
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
      })
    }

    fixture.autoStart = autoStartEnvironment
    fixture.initialize(this@BaseEmojiStoreCommandTest)

    val databaseConfig = org.koin.core.context.GlobalContext.get().get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    messageDataConnector = MessageDataConnector(database)
    messageEmojiDataConnector = MessageEmojiDataConnector(database)
    emojiDataConnector = EmojiDataConnector(database)

    testGuild = env.jda.getGuildsByName(defaultGuildName, false).first()
    generalChannel = testGuild.getTextChannelsByName(defaultGeneralChannelName, false).first()

    testUsers = (1..5).map { i -> env.createUser("TestUser$i", false) }

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

  protected abstract fun getFeatureKeysForTest(): Array<String>

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

  protected fun createMessageWithReactions(
    channelName: String = defaultGeneralChannelName,
    messageText: String = "Test message with reactions",
    reactionConfig: List<ReactionConfig> = emptyList()
  ): Message {
    val message = env.sendMessage(
      defaultGuildName,
      channelName,
      testUsers[0],
      messageText
    ).complete(true)!! as TestMessage

    reactionConfig.forEach { config ->
      val emoji = TestEmoji(config.emojiName)
      config.userIndices.forEach { userIndex ->
        if (userIndex >= 0 && userIndex < testUsers.size) {
          message.addReaction(emoji, testUsers[userIndex])
        }
      }
    }

    env.awaitAll()

    return message
  }

  protected fun createMultipleMessagesWithReactions(
    count: Int = 5,
    channelName: String = defaultGeneralChannelName,
    baseMessageText: String = "Test message",
    reactionConfigs: List<ReactionConfig> = listOf(
      ReactionConfig("👍", listOf(1, 2)),
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

  protected data class ReactionConfig(
    val emojiName: String,
    val userIndices: List<Int>
  )
}
