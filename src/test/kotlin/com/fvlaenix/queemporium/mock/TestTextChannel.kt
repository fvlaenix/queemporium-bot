package com.fvlaenix.queemporium.mock

import io.mockk.mockk
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.*
import net.dv8tion.jda.api.requests.restaction.pagination.ThreadChannelPaginationAction
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.internal.requests.RestActionImpl
import net.dv8tion.jda.internal.requests.restaction.MessageCreateActionImpl
import org.jetbrains.annotations.Unmodifiable

class TestTextChannel(
  private val testEnvironment: TestEnvironment,
  private val testJda: TestJDA,
  private val testGuild: TestGuild,
  private val idLong: Long,
  private val name: String
) : TextChannel, MessageChannelUnion, MessageChannel {
  private val messages = mutableListOf<Message>()

  override fun getJDA(): JDA = testJda
  override fun getGuild(): Guild = testGuild
  override fun delete(): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun getPermissionContainer(): IPermissionContainer {
    TODO("Not yet implemented")
  }

  // Методы для работы с сообщениями
  fun addMessage(message: Message) {
    messages.add(message)
  }

  override fun sendMessage(text: CharSequence): MessageCreateAction {
    val textMessage = text.toString()
    val message = TestMessage(
      testJda,
      this,
      testEnvironment.nextId(),
      textMessage,
      mockk(relaxed = true)
    )

    val restAction = ImmediatelyTestRestAction.builder<Message?>(jda)
      .withResult(message)
      .build()

    val action = TestMessageCreateAction(restAction)
    testEnvironment.notifyMessage(message)
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

  override fun getName(): String = name
  override fun getType(): ChannelType {
    TODO("Not yet implemented")
  }

  override fun getId(): String = idLong.toString()
  override fun getIdLong(): Long = idLong

  override fun retrieveWebhooks(): RestAction<@Unmodifiable List<Webhook?>?> {
    TODO("Not yet implemented")
  }

  override fun createWebhook(name: String): WebhookAction =
    TODO("Not yet implemented")

  override fun deleteWebhookById(id: String): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun createCopy(guild: Guild): ChannelAction<TextChannel?> {
    TODO("Not yet implemented")
  }

  override fun getManager(): TextChannelManager {
    TODO("Not yet implemented")
  }

  override fun getTopic(): String? {
    TODO("Not yet implemented")
  }

  override fun compareTo(other: GuildChannel): Int =
    name.compareTo(other.name)

  override fun getPermissionOverride(permissionHolder: IPermissionHolder): PermissionOverride? {
    TODO("Not yet implemented")
  }

  override fun getPermissionOverrides(): @Unmodifiable List<PermissionOverride?> {
    TODO("Not yet implemented")
  }

  override fun upsertPermissionOverride(permissionHolder: IPermissionHolder): PermissionOverrideAction {
    TODO("Not yet implemented")
  }

  override fun getPositionRaw(): Int {
    TODO("Not yet implemented")
  }

  override fun getMembers(): @Unmodifiable List<Member?> {
    TODO("Not yet implemented")
  }

  override fun createInvite(): InviteAction {
    TODO("Not yet implemented")
  }

  override fun retrieveInvites(): RestAction<List<Invite?>?> {
    TODO("Not yet implemented")
  }

  override fun getParentCategoryIdLong(): Long {
    TODO("Not yet implemented")
  }

  override fun isSynced(): Boolean {
    TODO("Not yet implemented")
  }

  override fun canTalk(member: Member): Boolean {
    TODO("Not yet implemented")
  }

  override fun removeReactionById(
    messageId: String,
    emoji: Emoji,
    user: User
  ): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun deleteMessagesByIds(messageIds: Collection<String?>): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun clearReactionsById(messageId: String): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun clearReactionsById(
    messageId: String,
    emoji: Emoji
  ): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun sendStickers(stickers: Collection<StickerSnowflake?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun getLatestMessageIdLong(): Long {
    TODO("Not yet implemented")
  }

  override fun getDefaultThreadSlowmode(): Int {
    TODO("Not yet implemented")
  }

  override fun createThreadChannel(
    name: String,
    isPrivate: Boolean
  ): ThreadChannelAction {
    TODO("Not yet implemented")
  }

  override fun createThreadChannel(
    name: String,
    messageId: Long
  ): ThreadChannelAction {
    TODO("Not yet implemented")
  }

  override fun retrieveArchivedPublicThreadChannels(): ThreadChannelPaginationAction {
    TODO("Not yet implemented")
  }

  override fun retrieveArchivedPrivateThreadChannels(): ThreadChannelPaginationAction {
    TODO("Not yet implemented")
  }

  override fun retrieveArchivedPrivateJoinedThreadChannels(): ThreadChannelPaginationAction {
    TODO("Not yet implemented")
  }

  override fun isNSFW(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getSlowmode(): Int {
    TODO("Not yet implemented")
  }

  override fun asPrivateChannel(): PrivateChannel {
    TODO("Not yet implemented")
  }

  override fun asTextChannel(): TextChannel {
    TODO("Not yet implemented")
  }

  override fun asNewsChannel(): NewsChannel {
    TODO("Not yet implemented")
  }

  override fun asThreadChannel(): ThreadChannel {
    TODO("Not yet implemented")
  }

  override fun asVoiceChannel(): VoiceChannel {
    TODO("Not yet implemented")
  }

  override fun asStageChannel(): StageChannel {
    TODO("Not yet implemented")
  }

  override fun asThreadContainer(): IThreadContainer {
    TODO("Not yet implemented")
  }

  override fun asGuildMessageChannel(): GuildMessageChannel {
    TODO("Not yet implemented")
  }

  override fun asAudioChannel(): AudioChannel {
    TODO("Not yet implemented")
  }
}