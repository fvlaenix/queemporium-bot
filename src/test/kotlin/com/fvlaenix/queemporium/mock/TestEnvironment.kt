package com.fvlaenix.queemporium.mock

import com.fvlaenix.queemporium.DiscordBot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
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
    val id = nextId()
    val guild = TestGuild(jda, id, name)
    jda.addGuild(guild)
    return guild
  }

  fun createTextChannel(guild: Guild, name: String): MessageChannelUnion {
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

  fun createPrivateChannel(user: User): PrivateChannel {
    // Check if channel already exists for this user
    val existingChannel = jda.privateChannelCache.find { it?.user?.idLong == user.idLong }
    if (existingChannel != null) {
      return existingChannel
    }

    // Create new private channel
    val id = nextId()
    val privateChannel = TestPrivateChannel(this, jda, id, user)

    // Store in JDA
    jda.addPrivateChannel(privateChannel)

    // Configure user to return this channel
    val userOpenPrivateChannelAction = ImmediatelyTestRestAction.builder<PrivateChannel>(jda)
      .withResult(privateChannel)
      .build()

    return privateChannel
  }

  /**
   * Gets the private channel for the given user, creating it if it doesn't exist
   *
   * @param user The user whose private channel to get
   * @return The private channel
   */
  fun getPrivateChannel(user: User): PrivateChannel {
    val existingChannel = jda.privateChannelCache.find { it?.user?.idLong == user.idLong }
    return existingChannel ?: createPrivateChannel(user)
  }

  fun notifyMessageSend(message: Message) {
    val messageReceivedEvent = MessageReceivedEvent(jda, 0, message)
    jda.notifyMessageSend(messageReceivedEvent)
  }

  fun notifyMessageDelete(message: Message) {
    val messageDeletedEvent = MessageDeleteEvent(jda, 0, message.idLong, message.channel)
    jda.notifyMessageDeleted(messageDeletedEvent)
  }

  fun createMember(
    guild: Guild,
    user: User,
    isAdmin: Boolean = false,
    isRoleAdmin: Boolean = false
  ): Member {
    val member = TestUtils.createMockMember(user, guild, isAdmin, isRoleAdmin)
    (guild as TestGuild).addMember(member)
    return member
  }

  fun addListener(listener: ListenerAdapter) {
    listeners.add(listener)
  }

  fun addReaction(message: TestMessage, user: User, emojiName: String): TestMessage {
    val emoji = TestEmoji(emojiName)
    message.addReaction(emoji, user)
    return message
  }

  fun addReactions(message: TestMessage, emojiName: String, users: List<User>): TestMessage {
    val emoji = TestEmoji(emojiName)
    users.forEach { user ->
      message.addReaction(emoji, user)
    }
    return message
  }

  fun start() {
    require(!isStarted) { "Bot is already started" }
    isStarted = true

    listeners.forEach { listener ->
      jda.addEventListener(listener)
    }

    val readyEvent = mockk<ReadyEvent>()
    every { readyEvent.jda } returns jda
    listeners.forEach { listener ->
      listener.onReady(readyEvent)
    }
    awaitAll()
  }

  fun awaitAll() {
    runBlocking {
      var isReady = false
      while (!isReady) {
        isReady = true
        DiscordBot.MAIN_SCOPE.coroutineContext.job.children.forEach { isReady = false; it.join() }
      }
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
    notifyMessageSend(message)

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

  /**
   * Sends a direct message from the given user
   *
   * @param user The user sending the message
   * @param message The message content
   * @param attachments Optional list of attachments
   * @return The message create action
   */
  fun sendDirectMessage(
    user: User,
    message: String,
    attachments: List<Message.Attachment> = emptyList()
  ): MessageCreateAction {
    // Get or create private channel for this user
    val privateChannel = getPrivateChannel(user) as TestPrivateChannel

    // Create the message
    val testMessage = TestMessage(
      jda,
      null, // No guild for private messages
      privateChannel,
      nextId(),
      message,
      user,
      attachments
    )

    // Add to channel and notify
    privateChannel.addMessage(testMessage)
    notifyMessageSend(testMessage)

    // Create and return action
    val restAction = ImmediatelyTestRestAction.builder<Message?>(jda)
      .withResult(testMessage)
      .build()

    return TestMessageCreateAction(restAction)
  }
}