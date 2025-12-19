package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.requests.restaction.MessageEditAction
import net.dv8tion.jda.api.utils.AttachedFile
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class TestMessageEditAction(private val message: Message) : MessageEditAction {

  override fun setReplace(replace: Boolean): MessageEditAction {
    return this
  }

  override fun isReplace(): Boolean {
    return false
  }

  override fun applyData(data: MessageEditData): MessageEditAction {
    return this
  }

  override fun setContent(content: String?): MessageEditAction {
    return this
  }

  override fun setEmbeds(embeds: Collection<MessageEmbed>): MessageEditAction {
    return this
  }

  override fun setComponents(components: Collection<LayoutComponent>): MessageEditAction {
    return this
  }

  override fun setFiles(files: Collection<FileUpload?>?): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun setAttachments(attachments: Collection<AttachedFile?>?): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun setSuppressEmbeds(suppress: Boolean): MessageEditAction {
    return this
  }

  override fun setAllowedMentions(allowedMentions: Collection<Message.MentionType?>?): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun mention(mentions: Collection<IMentionable?>): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun mentionUsers(userIds: Collection<String?>): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun mentionRoles(roleIds: Collection<String?>): MessageEditAction {
    TODO("Not yet implemented")
  }

  override fun mentionRepliedUser(mention: Boolean): MessageEditAction {
    return this
  }

  override fun isMentionRepliedUser(): Boolean {
    return false
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

  override fun getAttachments(): List<AttachedFile?> {
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

  override fun setCheck(checks: BooleanSupplier?): MessageEditAction {
    return this
  }

  override fun getJDA(): JDA = message.jda

  override fun queue(success: Consumer<in Message?>?, failure: Consumer<in Throwable>?) {
    success?.accept(message)
  }

  override fun submit(shouldQueue: Boolean): CompletableFuture<Message> {
    return CompletableFuture.completedFuture(message)
  }

  override fun complete(shouldQueue: Boolean): Message {
    return message
  }
}
