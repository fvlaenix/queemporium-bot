package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.PixivCompressedDetectorCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test

class PixivCompressedDetectorCommandTest : BaseKoinTest() {
  @Test
  fun `test pixiv compressed image detection with fake database`() {
    val answerService = MockAnswerService()

    val koin = setupBotKoin {
      this.answerService = answerService
      enableCommands(PixivCompressedDetectorCommand::class)
    }

    val databaseConfig = koin.get<DatabaseConfiguration>()
    val database = databaseConfig.toDatabase()
    val guildInfoConnector = GuildInfoConnector(database)

    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general") {
        }
        withChannel("duplicate-channel") {
        }
      }
    }

    val testGuild = env.jda.getGuildsByName("Test Guild", false).first()
    val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()

    guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)

    val user = env.createUser("Test User", false)

    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "",
      listOf(createTestAttachment("12345_p0_master1200.jpg"))
    )

    env.awaitAll()

    answerService.verify {
      messageCount(1)
      lastMessageContains("picture was sent with Pixiv compression")
    }
  }
}