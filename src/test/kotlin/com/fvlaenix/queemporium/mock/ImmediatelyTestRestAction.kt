package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.RestAction
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier
import java.util.function.Consumer

open class ImmediatelyTestRestAction<T> internal constructor(private val jda: JDA) : RestAction<T> {
  private var result: T? = null
  private var error: Throwable? = null
  private var delay: Long = 0

  class Builder<T>(private val jda: JDA) {
    private var result: T? = null
    private var error: Throwable? = null
    private var delay: Long = 0

    fun withResult(result: T) = apply {
      this.result = result
      this.error = null
    }

    fun withError(error: Throwable) = apply {
      this.error = error
      this.result = null
    }

    fun withDelay(delayMs: Long) = apply {
      this.delay = delayMs
    }

    fun build() = ImmediatelyTestRestAction<T>(jda).also {
      it.result = result
      it.error = error
      it.delay = delay
    }
  }

  companion object {
    fun <T> builder(jda: JDA) = Builder<T>(jda)
  }

  override fun getJDA(): JDA = jda

  override fun setCheck(checks: BooleanSupplier?): RestAction<T> = this

  override fun queue(success: Consumer<in T>?, failure: Consumer<in Throwable>?) {
    submit(true).whenComplete { result, error ->
      when {
        error != null -> failure?.accept(error)
        else -> success?.accept(result)
      }
    }
  }

  override fun complete(shouldQueue: Boolean): T {
    return submit(shouldQueue).get()
  }

  override fun submit(shouldQueue: Boolean): CompletableFuture<T> {
    val future = CompletableFuture<T>()

    if (delay > 0) {
      CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute {
        completeWithResult(future)
      }
    } else {
      completeWithResult(future)
    }

    return future
  }

  private fun completeWithResult(future: CompletableFuture<T>) {
    when {
      error != null -> future.completeExceptionally(error)
      result != null -> future.complete(result)
      else -> future.completeExceptionally(IllegalStateException("Neither result nor error was set"))
    }
  }
}