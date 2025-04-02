package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.messages.MessagePoll
import net.dv8tion.jda.api.entities.messages.MessageSnapshot
import net.dv8tion.jda.api.entities.sticker.StickerItem
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction
import net.dv8tion.jda.api.utils.AttachedFile
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.jetbrains.annotations.Unmodifiable
import java.time.OffsetDateTime
import java.util.*

class TestMessage(
  private val testJda: JDA,
  private val testGuild: Guild?,
  private val testChannel: MessageChannelUnion,
  private val idLong: Long,
  private val content: String,
  private val author: User,
  private val attachments: List<Message.Attachment> = mutableListOf(),
  private val reactions: MutableList<TestMessageReaction> = mutableListOf()
) : Message {
  override fun getJDA(): JDA = testJda

  override fun isPinned(): Boolean {
    TODO("Not yet implemented")
  }

  override fun pin(): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun unpin(): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun addReaction(emoji: Emoji): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  fun addReaction(emoji: TestEmoji, user: User): TestMessageReaction {
    val existingReaction = reactions.find { it.emoji.name == emoji.name }
    if (existingReaction != null) {
      existingReaction.addUser(user)
      return existingReaction
    }

    val reaction = TestMessageReaction(testJda, this, emoji, mutableListOf(user))
    reactions.add(reaction)
    return reaction
  }

  override fun getReactions(): List<MessageReaction> = reactions.toList()

  override fun getReaction(emoji: Emoji): MessageReaction? {
    return reactions.find { it.emoji.name == emoji.name }
  }

  override fun clearReactions(): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun clearReactions(emoji: Emoji): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun removeReaction(emoji: Emoji): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun removeReaction(
    emoji: Emoji,
    user: User
  ): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun retrieveReactionUsers(emoji: Emoji): ReactionPaginationAction {
    TODO("Not yet implemented")
  }

  override fun suppressEmbeds(suppressed: Boolean): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun crosspost(): RestAction<Message?> {
    TODO("Not yet implemented")
  }

  override fun isSuppressedEmbeds(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getFlags(): EnumSet<Message.MessageFlag?> {
    TODO("Not yet implemented")
  }

  override fun getFlagsRaw(): Long {
    TODO("Not yet implemented")
  }

  override fun isEphemeral(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isSuppressedNotifications(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isVoiceMessage(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getStartedThread(): ThreadChannel? {
    TODO("Not yet implemented")
  }

  override fun getType(): MessageType {
    TODO("Not yet implemented")
  }

  override fun getInteraction(): Message.Interaction? {
    TODO("Not yet implemented")
  }

  override fun createThreadChannel(name: String): ThreadChannelAction {
    TODO("Not yet implemented")
  }

  override fun getChannel(): MessageChannelUnion = testChannel
  override fun getGuildChannel(): GuildMessageChannelUnion {
    TODO("Not yet implemented")
  }

  override fun getCategory(): Category? {
    TODO("Not yet implemented")
  }

  override fun hasGuild(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getGuildIdLong(): Long =
    guild.id.toLong()

  override fun getGuild(): Guild =
    testGuild!!

  override fun getAttachments(): @Unmodifiable List<Message.Attachment?> =
    attachments.toList()

  override fun getEmbeds(): @Unmodifiable List<MessageEmbed?> =
    emptyList()

  override fun getComponents(): @Unmodifiable List<LayoutComponent?> {
    TODO("Not yet implemented")
  }

  override fun getPoll(): MessagePoll? {
    TODO("Not yet implemented")
  }

  override fun endPoll(): AuditableRestAction<Message?> {
    TODO("Not yet implemented")
  }

  override fun getStickers(): @Unmodifiable List<StickerItem?> {
    TODO("Not yet implemented")
  }

  override fun getMessageSnapshots(): @Unmodifiable List<MessageSnapshot?> {
    TODO("Not yet implemented")
  }

  override fun isTTS(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getActivity(): MessageActivity? {
    TODO("Not yet implemented")
  }

  override fun editMessage(newContent: CharSequence): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun editMessage(data: MessageEditData): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun editMessageEmbeds(embeds: Collection<MessageEmbed?>): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun editMessageComponents(components: Collection<LayoutComponent?>): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun editMessageFormat(
    format: String,
    vararg args: Any?
  ): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun editMessageAttachments(attachments: Collection<AttachedFile?>): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun delete(): AuditableRestAction<Void?> =
    channel.deleteMessageById(id)

  override fun getContentRaw(): String = content
  override fun getContentStripped(): String {
    TODO("Not yet implemented")
  }

  override fun getInvites(): @Unmodifiable List<String?> {
    TODO("Not yet implemented")
  }

  override fun getNonce(): String? {
    TODO("Not yet implemented")
  }

  override fun isFromType(type: ChannelType): Boolean {
    TODO("Not yet implemented")
  }

  override fun isFromGuild(): Boolean =
    testGuild != null

  override fun getChannelType(): ChannelType {
    TODO("Not yet implemented")
  }

  override fun isWebhookMessage(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getApplicationIdLong(): Long {
    TODO("Not yet implemented")
  }

  override fun hasChannel(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getChannelIdLong(): Long =
    testChannel.idLong

  override fun getContentDisplay(): String = content
  override fun getMessageReference(): MessageReference? {
    TODO("Not yet implemented")
  }

  override fun getMentions(): Mentions {
    TODO("Not yet implemented")
  }

  override fun isEdited(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getTimeEdited(): OffsetDateTime? {
    TODO("Not yet implemented")
  }

  override fun getAuthor(): User = author

  override fun getMember(): Member? {
    if (isFromGuild && testGuild != null) {
      return testGuild.getMember(author)
    }
    return null
  }

  override fun getApproximatePosition(): Int {
    TODO("Not yet implemented")
  }

  override fun getJumpUrl(): String =
    "https://myowntestdiscord.com/channels${if (testGuild != null) "/$guildId" else ""}/${channelId}/${id}"

  override fun getId(): String = idLong.toString()
  override fun getIdLong(): Long = idLong

  override fun getTimeCreated(): OffsetDateTime = OffsetDateTime.now()
  override fun formatTo(
    formatter: Formatter?,
    flags: Int,
    width: Int,
    precision: Int
  ) {
    TODO("Not yet implemented")
  }


}