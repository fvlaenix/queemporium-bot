package com.fvlaenix.queemporium.mock

import com.fvlaenix.queemporium.DiscordBot
import io.mockk.mockk
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction

class TestEnvironment {
  private var currentId = 0L
  val jda = TestJDA()
  private var isStarted = false
  private val listeners = mutableListOf<ListenerAdapter>()
  private val users = mutableMapOf<Long, TestUser>()

  fun nextId(): Long = currentId++

  fun createGuild(name: String): Guild {
    require(!isStarted) { "Cannot modify server state after bot start" }
    val id = nextId()
    val guild = TestGuild(jda, id, name)
    jda.addGuild(guild)
    return guild
  }

  fun createTextChannel(guild: Guild, name: String): MessageChannelUnion {
    require(!isStarted) { "Cannot modify server state after bot start" }
    val id = nextId()
    val channel = TestTextChannel(this, jda, guild as TestGuild, id, name)
    guild.addTextChannel(channel)
    return channel
  }

  fun createUser(
    name: String,
    isBot: Boolean = false,
    discriminator: String = "0000"
  ): TestUser {
    val id = nextId()
    val user = TestUser(jda, id, name, isBot, discriminator)
    users[id] = user
    return user
  }

  fun notifyMessage(message: Message) {
    require(isStarted) { "Cannot send messages before bot start" }
    val messageReceivedEvent = MessageReceivedEvent(jda, 0, message)
    jda.notifyMessageSend(messageReceivedEvent)
  }

  fun addListener(listener: ListenerAdapter) {
    require(!isStarted) { "Cannot add listeners after bot start" }
    listeners.add(listener)
  }

  fun start() {
    require(!isStarted) { "Bot is already started" }
    isStarted = true

    // Регистрируем слушателей
    listeners.forEach { listener ->
      jda.addEventListener(listener)
    }

    // Отправляем ReadyEvent всем слушателям
    val readyEvent = mockk<ReadyEvent>()
    listeners.forEach { listener ->
      listener.onReady(readyEvent)
    }
  }

  fun awaitAll() {
    runBlocking {
      DiscordBot.MAIN_SCOPE.coroutineContext.job.children.forEach { it.join() }
    }
  }

  fun sendMessage(
    message: Message
  ): TestMessageCreateAction {
    val channel = message.channel as TestTextChannel

    val restAction = ImmediatelyTestRestAction.builder<Message?>(jda)
      .withResult(message)
      .build()

    val action = TestMessageCreateAction(restAction)

    channel.addMessage(message)
    notifyMessage(message)

    return action
  }

  fun sendMessage(
    guildName: String,
    channelName: String,
    user: User,
    message: String,
    attachments: List<Message.Attachment> = emptyList()
  ): MessageCreateAction {
    val guild = jda.getGuildsByName(guildName, true).firstOrNull()
      ?: throw Exception("Guild with name $guildName doesn't exist")
    val channel = guild.getTextChannelsByName(channelName, true).firstOrNull() as? TestTextChannel
      ?: throw Exception("Channel with name $channelName doesn't exist in guild $guildName")

    val message = TestMessage(
      jda,
      guild,
      channel,
      nextId(),
      message,
      user,
      attachments
    )

    return sendMessage(message)
  }
}