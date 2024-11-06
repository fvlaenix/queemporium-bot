package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.commands.PingCommand
import com.fvlaenix.queemporium.service.MockAnswerService
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
  fun `test ping`() {
    val jda = mockk<JDA>()
    val guild = createGuild(jda, 0)
    val channel = createChannel(guild, 1, "Test Channel")
    val message = createMessage(channel, 2, "/shogun-sama ping")
    val answerService = MockAnswerService()
    val pingCommand = PingCommand(answerService)
    pingCommand.onMessageReceived(MessageReceivedEvent(jda, 0, message))
    awaitAllMessagesEvents()
    assertEquals(1, answerService.answers.size)
    assertEquals("Pong!", answerService.answers[0])
  }
}