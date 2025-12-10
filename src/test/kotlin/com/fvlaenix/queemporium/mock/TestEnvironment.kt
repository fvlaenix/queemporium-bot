package com.fvlaenix.queemporium.mock

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.coroutine.TestCoroutineProvider
import com.fvlaenix.queemporium.testing.dsl.ChannelResolver
import com.fvlaenix.queemporium.testing.dsl.GuildResolver
import io.mockk.every
import io.mockk.mockk
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
import org.koin.core.context.GlobalContext
import java.time.OffsetDateTime

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
    jda.addUser(user)
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
    ImmediatelyTestRestAction.builder<PrivateChannel>(jda)
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

  fun addReactionWithEvent(message: Message, emoji: TestEmoji, user: User) {
    val reaction = (message as TestMessage).addReaction(emoji, user)
    val member = message.guild.getMember(user)
    val messageReactionAddEvent = net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent(
      jda,
      0,
      user,
      member,
      reaction,
      user.idLong,
      message.idLong
    )
    jda.notifyReactionAdd(messageReactionAddEvent)
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
      val testProvider = GlobalContext.get().get<BotCoroutineProvider>() as? TestCoroutineProvider
        ?: throw IllegalStateException("Test environment requires TestCoroutineProvider")

      runBlocking {
        testProvider.awaitRegularJobs()
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
    attachments: List<Message.Attachment> = emptyList(),
    timeCreated: OffsetDateTime = OffsetDateTime.now()
  ): MessageCreateAction {
    val guild = GuildResolver.resolve(jda, guildName)
    val channel = ChannelResolver.resolve(guild, channelName) as? TestTextChannel
      ?: throw Exception("Channel with name $channelName doesn't exist in guild $guildName")

    val message = TestMessage(
      jda,
      guild,
      channel,
      nextId(),
      message,
      user,
      attachments,
      mutableListOf(),
      timeCreated
    )

    return sendMessage(message)
  }

  /**
   * Sends a direct message from the given user
   *
   * @param user The user sending the message
   * @param message The message content
   * @param attachments Optional list of attachments
   * @param timeCreated Optional time when the message was created
   * @return The message create action
   */
  fun sendDirectMessage(
    user: User,
    message: String,
    attachments: List<Message.Attachment> = emptyList(),
    timeCreated: OffsetDateTime = OffsetDateTime.now()
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
      attachments,
      mutableListOf(),
      timeCreated
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
