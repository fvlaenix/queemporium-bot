package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.*
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction
import net.dv8tion.jda.api.utils.Procedure
import net.dv8tion.jda.api.utils.data.DataObject
import org.jetbrains.annotations.Unmodifiable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class TestEmoji(
  private val name: String,
  private val id: String? = null,
  private val isCustom: Boolean = false
) : EmojiUnion {
  override fun getName(): String = name
  override fun getFormatted(): String = if (isCustom) "<:$name:$id>" else name
  override fun getAsReactionCode(): String = if (isCustom) "$name:$id" else name
  override fun getType(): Emoji.Type = if (isCustom) Emoji.Type.CUSTOM else Emoji.Type.UNICODE
  override fun toData(): DataObject = TODO("Not yet implemented")
  override fun asUnicode(): UnicodeEmoji = TODO("Not yet implemented")
  override fun asCustom(): CustomEmoji = TODO("Not yet implemented")
  override fun asRich(): RichCustomEmoji = TODO("Not yet implemented")
  override fun asApplication(): ApplicationEmoji = TODO("Not yet implemented")
}

class TestMessageReaction(
  private val testJda: JDA,
  private val testMessage: Message,
  private val emoji: TestEmoji,
  private val users: MutableList<User> = mutableListOf()
) : MessageReaction(
  testJda,
  testMessage.channel,
  emoji,
  testMessage.channel.idLong,
  testMessage.idLong,
  arrayOf<Boolean>(false, false).toBooleanArray(),
  arrayOf<Int>(users.size, users.size, 0).toIntArray()
) {
  override fun getEmoji(): EmojiUnion = emoji
  override fun getCount(): Int = users.size
  override fun isSelf(): Boolean = users.any { it.id == testJda.selfUser.id }
  override fun getJDA(): JDA = testJda
  override fun getChannel(): MessageChannelUnion = testMessage.channel
  override fun getGuild(): Guild = testMessage.guild

  override fun retrieveUsers(): ReactionPaginationAction =
    ReactionPaginationActionImpl(users)

  fun addUser(user: User) {
    if (!users.contains(user)) {
      users.add(user)
    }
  }

  fun removeUser(user: User) {
    users.remove(user)
  }

  class ReactionPaginationActionImpl(val users: List<User>) : ReactionPaginationAction {
    override fun getReaction(): MessageReaction {
      TODO("Not yet implemented")
    }

    override fun skipTo(id: Long): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun getLastKey(): Long {
      TODO("Not yet implemented")
    }

    override fun setCheck(checks: BooleanSupplier?): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun timeout(
      timeout: Long,
      unit: TimeUnit
    ): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun deadline(timestamp: Long): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun getOrder(): PaginationAction.PaginationOrder {
      TODO("Not yet implemented")
    }

    override fun order(order: PaginationAction.PaginationOrder): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun cacheSize(): Int {
      TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
      TODO("Not yet implemented")
    }

    override fun getCached(): @Unmodifiable List<User?> {
      TODO("Not yet implemented")
    }

    override fun getLast(): User {
      TODO("Not yet implemented")
    }

    override fun getFirst(): User {
      TODO("Not yet implemented")
    }

    override fun limit(limit: Int): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun cache(enableCache: Boolean): ReactionPaginationAction {
      TODO("Not yet implemented")
    }

    override fun isCacheEnabled(): Boolean {
      TODO("Not yet implemented")
    }

    override fun getMaxLimit(): Int {
      TODO("Not yet implemented")
    }

    override fun getMinLimit(): Int {
      TODO("Not yet implemented")
    }

    override fun getLimit(): Int {
      TODO("Not yet implemented")
    }

    override fun takeAsync(amount: Int): CompletableFuture<List<User?>?> {
      TODO("Not yet implemented")
    }

    override fun takeRemainingAsync(amount: Int): CompletableFuture<List<User?>?> {
      TODO("Not yet implemented")
    }

    override fun forEachAsync(
      action: Procedure<in User>,
      failure: Consumer<in Throwable>
    ): CompletableFuture<*> {
      TODO("Not yet implemented")
    }

    override fun forEachRemainingAsync(
      action: Procedure<in User>,
      failure: Consumer<in Throwable>
    ): CompletableFuture<*> {
      TODO("Not yet implemented")
    }

    override fun forEachRemaining(action: Procedure<in User>) {
      TODO("Not yet implemented")
    }

    override fun iterator(): PaginationAction.PaginationIterator<User?> {
      TODO("Not yet implemented")
    }

    override fun getJDA(): JDA {
      TODO("Not yet implemented")
    }

    override fun queue(
      success: Consumer<in @Unmodifiable MutableList<User>>?,
      failure: Consumer<in Throwable>?
    ) {
      TODO("Not yet implemented")
    }

    override fun complete(shouldQueue: Boolean): @Unmodifiable List<User?>? =
      users.toList()

    override fun submit(shouldQueue: Boolean): CompletableFuture<@Unmodifiable List<User?>?> {
      TODO("Not yet implemented")
    }
  }
}