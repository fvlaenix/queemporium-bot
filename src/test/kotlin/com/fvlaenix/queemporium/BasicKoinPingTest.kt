package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.PingCommand
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test

class BasicKoinPingTest : BaseKoinTest() {
  @Test
  fun `test ping command with direct koin setup`() {
    val answerService = MockAnswerService()

    setupBotKoin {
      this.answerService = answerService
      enableCommands(PingCommand::class)
    }

    val env = createEnvironment {
      createGuild("Test Guild") {
        withChannel("general")
      }
    }

    val user = env.createUser("Test User", false)
    env.sendMessage("Test Guild", "general", user, "/shogun-sama ping").queue()

    env.awaitAll()

    answerService.verify {
      messageCount(1)
      lastMessageContains("Pong!")
    }
  }
}