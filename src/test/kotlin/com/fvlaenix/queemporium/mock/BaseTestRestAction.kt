package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * Base abstract class for implementing test Rest Actions.
 * Contains common functionality for both regular and auditable rest actions.
 *
 * @param T The type of result this RestAction returns
 * @param JDA The JDA instance
 */
abstract class BaseTestRestAction<T>(protected val jda: JDA) {
    protected var result: T? = null
    protected var error: Throwable? = null
    protected var delay: Long = 0
    protected var isResultSet: Boolean = false

    /**
     * Gets the JDA instance used for this RestAction.
     *
     * @return JDA instance
     */
    fun getJDA(): JDA = jda

    /**
     * Queues the request, and provides callbacks for success and failure.
     *
     * @param success Consumer to handle success
     * @param failure Consumer to handle failure
     */
    fun queue(success: Consumer<in T?>?, failure: Consumer<in Throwable>?) {
        submit(true).whenComplete { result, error ->
            when {
                error != null -> failure?.accept(error)
                isResultSet -> success?.accept(result)
            }
        }
    }

    /**
     * Blocks the current thread and waits for the request to complete.
     *
     * @param shouldQueue Whether the request should be queued in JDA
     * @return The response value
     */
    fun complete(shouldQueue: Boolean): T? {
        return submit(shouldQueue).get()
    }

    /**
     * Submits the request and returns a Future representing the pending result.
     *
     * @param shouldQueue Whether the request should be queued in JDA
     * @return CompletableFuture containing the response value
     */
    fun submit(shouldQueue: Boolean): CompletableFuture<T?> {
        val future = CompletableFuture<T?>()

        if (delay > 0) {
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute {
                completeWithResult(future)
            }
        } else {
            completeWithResult(future)
        }

        return future
    }

    /**
     * Completes the future with either the result or error.
     *
     * @param future The future to complete
     */
    protected fun completeWithResult(future: CompletableFuture<T?>) {
        when {
            error != null -> future.completeExceptionally(error)
            isResultSet -> future.complete(result)
            else -> future.completeExceptionally(IllegalStateException("Neither result nor error was set"))
        }
    }
}