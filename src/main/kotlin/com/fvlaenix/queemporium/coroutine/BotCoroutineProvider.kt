package com.fvlaenix.queemporium.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlin.time.Duration

/**
 * Provider interface for bot coroutine resources.
 * Allows different implementations for production and testing.
 */
interface BotCoroutineProvider {
  /** Thread pool for bot processing */
  val botPool: ExecutorCoroutineDispatcher

  /** Main coroutine scope for launching tasks */
  val mainScope: CoroutineScope

  /**
   * Controlled delay function for bot operations.
   * In production, behaves like normal delay.
   * In tests, can control infinite loops.
   */
  suspend fun safeDelay(duration: Duration)
}