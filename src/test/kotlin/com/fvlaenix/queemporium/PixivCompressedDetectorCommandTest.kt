package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.PixivCompressedDetectorCommand
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.verification.verify
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class PixivCompressedDetectorCommandTest {
  @Test
  @Ignore("Until theres will be text/fake databases")
  fun `test pixiv compressed image detection with fake database`() {
    val database = mockk<Database>()

    val databaseConfiguration: DatabaseConfiguration = mockk()
    every { databaseConfiguration.toDatabase() } returns database

    val answerService = MockAnswerService()
    val command = PixivCompressedDetectorCommand(
      databaseConfiguration,
      answerService
    )

    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general") {
        }
        addListener(command)
      }
    }

    val user = env.createUser("Test User", false)

    /*sendImage(
      fileName = "12345_p0_master1200.jpg",
      image = TestImages.createTestImage(100, 100)
    )*/

    env.sendMessage(
      "Test Guild",
      "general",
      user,
      "",
      listOf(createTestAttachment())
    )

    env.awaitAll()

    answerService.verify {
      messageCount(1)
      lastMessageContains("picture was sent with Pixiv compression")
    }
  }
}