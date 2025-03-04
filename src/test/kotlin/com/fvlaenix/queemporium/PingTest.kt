package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.PingCommand
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.TestMessage
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.verification.verify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.internal.entities.channel.concrete.TextChannelImpl
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class PingTest {
  private fun awaitAllMessagesEvents() {
    runBlocking {
      DiscordBot.MAIN_SCOPE.coroutineContext.job.children.forEach { job -> job.join() }
    }
  }

  private fun createGuild(jda: JDA, id: Int): Guild {
    val guild = mockk<Guild>()
    every { guild.idLong } returns id.toLong()
    every { guild.name } returns "Test Guild"
    every { guild.jda } returns jda
    return guild
  }

  private fun createChannel(guild: Guild, id: Int, name: String = "Test Channel"): TextChannelImpl {
    val channel = mockk<TextChannelImpl>()
    every { channel.idLong } returns id.toLong()
    every { channel.name } returns name
    every { channel.jda } returns guild.jda
    return channel
  }

  private fun createMessage(channel: MessageChannelUnion, id: Int, text: String): Message {
    val message = mockk<Message>()
    every { message.idLong } returns id.toLong()
    every { message.contentRaw } returns text
    every { message.channel } returns channel
    return message
  }

  @Test
  fun `test ping mockk`() {
    val jda = mockk<JDA>()
    val guild = createGuild(jda, 0)
    val channel = createChannel(guild, 1, "Test Channel")
    val message = createMessage(channel, 2, "/shogun-sama ping")
    val answerService = MockAnswerService()
    val pingCommand = PingCommand(answerService)
    pingCommand.onMessageReceived(MessageReceivedEvent(jda, 0, message))
    awaitAllMessagesEvents()
    assertEquals(1, answerService.answers.size)
    assertEquals("Pong!", answerService.answers[0].text)
  }

  private lateinit var environment: TestEnvironment

  @BeforeEach
  fun setup() {
    environment = TestEnvironment()
  }

  @Test
  fun `test ping command`() {
    val guild = environment.createGuild("Test Guild")
    val channel = environment.createTextChannel(guild, "Test Channel")
    val answerService = MockAnswerService()

    val pingCommand = PingCommand(answerService)

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

      addListener(PingCommand(answerService))
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
}