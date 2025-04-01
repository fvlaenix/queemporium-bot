package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction
import net.dv8tion.jda.api.utils.Procedure
import org.jetbrains.annotations.Unmodifiable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class TestMessagePaginationAction(
  val jda: JDA,
  val channel: TestTextChannel
) : MessagePaginationAction {
  override fun getChannel(): MessageChannelUnion = channel

  override fun skipTo(id: Long): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun getLastKey(): Long {
    TODO("Not yet implemented")
  }

  override fun setCheck(checks: BooleanSupplier?): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun timeout(
    timeout: Long,
    unit: TimeUnit
  ): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun deadline(timestamp: Long): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun getOrder(): PaginationAction.PaginationOrder {
    TODO("Not yet implemented")
  }

  override fun order(order: PaginationAction.PaginationOrder): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun cacheSize(): Int {
    TODO("Not yet implemented")
  }

  override fun isEmpty(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getCached(): @Unmodifiable List<Message?> {
    TODO("Not yet implemented")
  }

  override fun getLast(): Message {
    TODO("Not yet implemented")
  }

  override fun getFirst(): Message {
    TODO("Not yet implemented")
  }

  override fun limit(limit: Int): MessagePaginationAction {
    TODO("Not yet implemented")
  }

  override fun cache(enableCache: Boolean): MessagePaginationAction {
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

  override fun takeAsync(amount: Int): CompletableFuture<List<Message?>?> {
    return CompletableFuture<List<Message?>?>().apply { complete(channel.messages) }
  }

  override fun takeRemainingAsync(amount: Int): CompletableFuture<List<Message?>?> {
    TODO("Not yet implemented")
  }

  override fun forEachAsync(
    action: Procedure<in Message>,
    failure: Consumer<in Throwable>
  ): CompletableFuture<*> {
    TODO("Not yet implemented")
  }

  override fun forEachRemainingAsync(
    action: Procedure<in Message>,
    failure: Consumer<in Throwable>
  ): CompletableFuture<*> {
    TODO("Not yet implemented")
  }

  override fun forEachRemaining(action: Procedure<in Message>) {
    TODO("Not yet implemented")
  }

  override fun iterator(): PaginationAction.PaginationIterator<Message?> {
    val messages = channel.messages.toMutableList()
    return PaginationAction.PaginationIterator(channel.messages.toList()) { val copy = messages.toList(); messages.clear(); copy }
  }

  override fun getJDA(): JDA {
    TODO("Not yet implemented")
  }

  override fun queue(
    success: Consumer<in @Unmodifiable MutableList<Message>>?,
    failure: Consumer<in Throwable>?
  ) {
    TODO("Not yet implemented")
  }

  override fun complete(shouldQueue: Boolean): @Unmodifiable List<Message?>? {
    TODO("Not yet implemented")
  }

  override fun submit(shouldQueue: Boolean): CompletableFuture<@Unmodifiable List<Message?>?> {
    TODO("Not yet implemented")
  }
}