package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer
import net.dv8tion.jda.api.entities.channel.concrete.*
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.annotations.Unmodifiable

class TestPrivateChannel(
  private val testEnvironment: TestEnvironment,
  private val testJda: TestJDA,
  private val idLong: Long,
  private val user: User
) : PrivateChannel, MessageChannelUnion {

  private val messages = mutableListOf<Message>()

  override fun getType(): ChannelType = ChannelType.PRIVATE
  override fun getName(): String = "private-${user.name}"
  override fun getId(): String = idLong.toString()
  override fun getIdLong(): Long = idLong
  override fun getJDA(): JDA = testJda

  override fun delete(): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun getUser(): User = user

  override fun retrieveUser(): RestAction<User?> {
    TODO("Not yet implemented")
  }

  // Channel utilities
  fun addMessage(message: Message) {
    messages.add(message)
  }

  override fun sendMessage(text: CharSequence): MessageCreateAction {
    val textMessage = text.toString()
    val message = TestMessage(
      testJda,
      null, // No guild for private channels
      this,
      testEnvironment.nextId(),
      textMessage,
      testJda.selfUser // Bot sends the message
    )

    val restAction = ImmediatelyTestRestAction.builder<Message?>(jda)
      .withResult(message)
      .build()

    val action = TestMessageCreateAction(restAction)
    testEnvironment.notifyMessageSend(message)
    messages.add(message)
    return action
  }

  override fun retrieveMessageById(id: Long): RestAction<Message> {
    val message = messages.find { it.idLong == id }
    return if (message != null) {
      ImmediatelyTestRestAction.builder<Message>(jda).withResult(message).build()
    } else {
      ImmediatelyTestRestAction.builder<Message>(jda).withError(RuntimeException("Message not found")).build()
    }
  }

  override fun getHistory(): MessageHistory {
    TODO("Not yet implemented")
  }

  override fun getIterableHistory(): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun getLatestMessageIdLong(): Long {
    return messages.lastOrNull()?.idLong ?: 0L
  }

  override fun getLatestMessageId(): String {
    return messages.last().id
  }

  override fun sendMessageFormat(p0: String, vararg p1: Any?): MessageCreateAction {

    TODO("Not yet implemented")
  }

  override fun sendFiles(vararg p0: FileUpload?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun sendFiles(p0: Collection<FileUpload?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun retrieveMessageById(p0: String): RestAction<Message?> {
    TODO("Not yet implemented")
  }

  override fun pinMessageById(p0: String): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun unpinMessageById(p0: String): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun retrievePinnedMessages(): RestAction<@Unmodifiable List<Message?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveReactionUsersById(p0: String, p1: Emoji): ReactionPaginationAction {
    TODO("Not yet implemented")
  }

  override fun canTalk(): Boolean {
    return true
  }

  override fun asPrivateChannel(): PrivateChannel {
    return this
  }

  override fun asTextChannel(): TextChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to TextChannel")
  }

  override fun asNewsChannel(): NewsChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to NewsChannel")
  }

  override fun asThreadChannel(): ThreadChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to ThreadChannel")
  }

  override fun asVoiceChannel(): VoiceChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to VoiceChannel")
  }

  override fun asStageChannel(): StageChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to StageChannel")
  }

  override fun asThreadContainer(): IThreadContainer {
    throw IllegalStateException("Cannot convert PrivateChannel to IThreadContainer")
  }

  override fun asGuildMessageChannel(): GuildMessageChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to GuildMessageChannel")
  }

  override fun asAudioChannel(): AudioChannel {
    throw IllegalStateException("Cannot convert PrivateChannel to AudioChannel")
  }
}