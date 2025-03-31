package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import java.util.function.BooleanSupplier

/**
 * Implementation of AuditableRestAction that immediately completes without actual network requests.
 * Used for testing JDA audit logging interactions without actual API calls.
 *
 * @param T The type of result this AuditableRestAction returns
 * @param jda The JDA instance
 */
class AuditableImmediatelyTestRestAction<T> internal constructor(jda: JDA) :
  BaseTestRestAction<T>(jda), AuditableRestAction<T> {

  private var reason: String? = null

  /**
   * Builder for creating AuditableImmediatelyTestRestAction instances.
   */
  class Builder<T>(private val jda: JDA) {
    private var result: T? = null
    private var error: Throwable? = null
    private var delay: Long = 0
    private var reason: String? = null
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

    fun withReason(reason: String) = apply {
      this.reason = reason
    }

    fun build() = AuditableImmediatelyTestRestAction<T>(jda).also {
      it.result = result
      it.error = error
      it.delay = delay
      it.reason = reason
      it.isResultSet = isResultSet
    }
  }

  companion object {
    fun <T> builder(jda: JDA) = Builder<T>(jda)
  }

  /**
   * Sets the reason for this audit action.
   *
   * @param reason The reason for this audit action
   * @return The same instance for chaining
   */
  override fun reason(reason: String?): AuditableRestAction<T> {
    this.reason = reason
    return this
  }

  /**
   * Sets a check that validates if the RestAction can be executed.
   * In this test implementation, the check is ignored.
   *
   * @param checks The BooleanSupplier to check if the RestAction can be executed
   * @return The same instance for chaining
   */
  override fun setCheck(checks: BooleanSupplier?): AuditableRestAction<T> = this
}