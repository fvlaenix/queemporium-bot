package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.PingCommand
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.coroutine.TestCoroutineProvider
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.TestMessage
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.verification.verify
import io.mockk.mockk
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class PingTest {
  private lateinit var environment: TestEnvironment

  @BeforeEach
  fun setup() {
    val testConfigModule = module {
      single<BotCoroutineProvider> { TestCoroutineProvider() }
    }

    startKoin {
      modules(testConfigModule)
    }

    environment = TestEnvironment()
  }

  @Test
  @Ignore
  fun `test ping command`() {
    val guild = environment.createGuild("Test Guild")
    val channel = environment.createTextChannel(guild, "Test Channel")
    val answerService = MockAnswerService()

    val pingCommand = PingCommand(answerService, TestCoroutineProvider())

    environment.jda.addEventListener(pingCommand)
    environment.start()

    val message = TestMessage(environment.jda, guild, channel, 2, "/shogun-sama ping", mockk())

    val event = MessageReceivedEvent(environment.jda, 0, message)
    pingCommand.onMessageReceived(event)

    environment.awaitAll()

    assertEquals(1, answerService.answers.size)
    assertEquals("Pong!", answerService.answers[0].text)
  }

  @Test
  fun `test ping command using new lifecycle`() {
    val answerService = MockAnswerService()
    val guildName = "Test Guild"
    val channelName = "general"

    val env = createEnvironment {
      createGuild(guildName) {
        withChannel(channelName)
      }

      addListener(PingCommand(answerService, TestCoroutineProvider()))
    }

    val user = env.createUser("Test User", false)

    env.sendMessage(guildName, channelName, user, "/shogun-sama ping").queue()

    env.awaitAll()

    answerService.verify {
      message {
        text("Pong!")
      }
    }
  }

  @AfterEach
  fun tearDownKoin() {
    stopKoin()
  }
}