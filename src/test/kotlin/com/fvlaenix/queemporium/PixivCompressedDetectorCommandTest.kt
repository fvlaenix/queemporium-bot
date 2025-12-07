package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.commands.PixivCompressedDetectorCommand
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test

class PixivCompressedDetectorCommandTest : BaseKoinTest() {
  @Test
  fun `test pixiv compressed image detection with fake database`() = testBot {
    before {
      enableCommands(PixivCompressedDetectorCommand::class)

      user("Test User")

      guild("Test Guild") {
        channel("general")
        channel("duplicate-channel")
      }
    }

    setup {
      val databaseConfig = org.koin.core.context.GlobalContext.get()
        .get<com.fvlaenix.queemporium.configuration.DatabaseConfiguration>()
      val database = databaseConfig.toDatabase()
      val guildInfoConnector = GuildInfoConnector(database)

      val testGuild = envWithTime.environment.jda.getGuildsByName("Test Guild", false).first()
      val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()

      guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)
    }

    scenario {
      val env = envWithTime.environment
      val user = env.createUser("Test User", false)

      env.sendMessage(
        "Test Guild",
        "general",
        user,
        "",
        listOf(createTestAttachment("12345_p0_master1200.jpg"))
      )

      awaitAll()

      expect("should detect pixiv compression") {
        messageSentCount(1)
        lastMessageContains("picture was sent with Pixiv compression")
      }
    }
  }
}
