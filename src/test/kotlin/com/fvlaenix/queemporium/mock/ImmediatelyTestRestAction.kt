package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.RestAction
import java.util.function.BooleanSupplier

/**
 * Implementation of RestAction that immediately completes without actual network requests.
 * Used for testing JDA interactions without actual API calls.
 *
 * @param T The type of result this RestAction returns
 * @param jda The JDA instance
 */
open class ImmediatelyTestRestAction<T> internal constructor(jda: JDA) :
  BaseTestRestAction<T>(jda), RestAction<T> {

  /**
   * Builder for creating ImmediatelyTestRestAction instances.
   */
  class Builder<T>(private val jda: JDA) {
    private var result: T? = null
    private var error: Throwable? = null
    private var delay: Long = 0
    private var isResultSet: Boolean = false

    fun withResult(result: T?) = apply {
      this.result = result
      this.error = null
      this.isResultSet = true
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
      it.isResultSet = isResultSet
    }
  }

  companion object {
    fun <T> builder(jda: JDA) = Builder<T>(jda)
  }

  /**
   * Sets a check that validates if the RestAction can be executed.
   * In this test implementation, the check is ignored.
   *
   * @param checks The BooleanSupplier to check if the RestAction can be executed
   * @return The same instance for chaining
   */
  override fun setCheck(checks: BooleanSupplier?): RestAction<T> = this
}