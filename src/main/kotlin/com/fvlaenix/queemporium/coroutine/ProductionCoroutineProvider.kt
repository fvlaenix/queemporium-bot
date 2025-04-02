package com.fvlaenix.queemporium.coroutine

import kotlinx.coroutines.*
import kotlin.time.Duration

/**
 * Production implementation of BotCoroutineProvider.
 * Uses standard coroutine functionality.
 */
class ProductionCoroutineProvider : BotCoroutineProvider {
  @OptIn(DelicateCoroutinesApi::class)
  override val botPool = newFixedThreadPoolContext(4, "MainBotPool")

  override val mainScope = CoroutineScope(Dispatchers.Default)

  override suspend fun safeDelay(duration: Duration) {
    delay(duration.inWholeMilliseconds)
  }
}