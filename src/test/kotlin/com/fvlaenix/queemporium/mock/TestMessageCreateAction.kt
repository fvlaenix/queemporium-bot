package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.MessageReference
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessagePollData
import java.util.EnumSet
import java.util.concurrent.CompletableFuture
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class TestMessageCreateAction(val answer: ImmediatelyTestRestAction<Message?>) : MessageCreateAction {

  private var referencedMessageId: String? = null
  private var failOnInvalidReply: Boolean = true

  override fun setNonce(nonce: String?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setMessageReference(
    type: MessageReference.MessageReferenceType,
    guildId: String?,
    channelId: String,
    messageId: String
  ): MessageCreateAction {
    this.referencedMessageId = messageId
    return this
  }

  override fun setMessageReference(messageId: String?): MessageCreateAction {
    this.referencedMessageId = messageId
    return this
  }

  override fun failOnInvalidReply(fail: Boolean): MessageCreateAction {
    this.failOnInvalidReply = fail
    return this
  }

  override fun setStickers(stickers: Collection<StickerSnowflake?>?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun addContent(content: String): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun addEmbeds(embeds: Collection<MessageEmbed?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun addComponents(components: Collection<LayoutComponent?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun addFiles(files: Collection<FileUpload?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun getAttachments(): List<FileUpload?> {
    TODO("Not yet implemented")
  }

  override fun getPoll(): MessagePollData? {
    TODO("Not yet implemented")
  }

  override fun setPoll(poll: MessagePollData?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setTTS(tts: Boolean): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setSuppressedNotifications(suppressed: Boolean): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setVoiceMessage(voiceMessage: Boolean): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setContent(content: String?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setEmbeds(embeds: Collection<MessageEmbed?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setComponents(components: Collection<LayoutComponent?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setSuppressEmbeds(suppress: Boolean): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setFiles(files: Collection<FileUpload?>?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun mentionRepliedUser(mention: Boolean): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun setAllowedMentions(allowedMentions: Collection<Message.MentionType?>?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun mention(mentions: Collection<IMentionable?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun mentionUsers(userIds: Collection<String?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun mentionRoles(roleIds: Collection<String?>): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun getContent(): String {
    TODO("Not yet implemented")
  }

  override fun getEmbeds(): List<MessageEmbed?> {
    TODO("Not yet implemented")
  }

  override fun getComponents(): List<LayoutComponent?> {
    TODO("Not yet implemented")
  }

  override fun isSuppressEmbeds(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getMentionedUsers(): Set<String?> {
    TODO("Not yet implemented")
  }

  override fun getMentionedRoles(): Set<String?> {
    TODO("Not yet implemented")
  }

  override fun getAllowedMentions(): EnumSet<Message.MentionType?> {
    TODO("Not yet implemented")
  }

  override fun isMentionRepliedUser(): Boolean {
    TODO("Not yet implemented")
  }

  override fun setCheck(checks: BooleanSupplier?): MessageCreateAction {
    TODO("Not yet implemented")
  }

  override fun getJDA(): JDA = answer.jda

  override fun queue(
    success: Consumer<in Message?>?,
    failure: Consumer<in Throwable>?
  ) {
    answer.queue(success, failure)
  }

  override fun complete(shouldQueue: Boolean): Message? {
    return answer.complete(shouldQueue)
  }

  override fun submit(shouldQueue: Boolean): CompletableFuture<Message?> {
    return answer.submit(shouldQueue)
  }

  fun getReferencedMessageId(): String? {
    return referencedMessageId
  }

  fun hasMessageReference(): Boolean {
    return referencedMessageId != null
  }

  fun isFailingOnInvalidReply(): Boolean {
    return failOnInvalidReply
  }
}