package com.fvlaenix.queemporium.coroutine

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

/**
 * Test implementation of BotCoroutineProvider.
 * Provides controlled coroutine execution for testing.
 */
class TestCoroutineProvider : BotCoroutineProvider {
  // Track first iterations of infinite loops
  private val firstIterationCompletions = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
  private val loopIterationCounts = ConcurrentHashMap<String, AtomicInteger>()

  @OptIn(DelicateCoroutinesApi::class)
  override val botPool = newFixedThreadPoolContext(2, "TestBotPool")

  // Using a custom dispatcher to track job creation
  override val mainScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, ex ->
    if (ex !is CancellationException || ex.message?.contains("after first iteration") == true) {
      println("Error in test coroutine: ${ex.message}")
      ex.printStackTrace()
    }
  })

  /**
   * Controls delay behavior to handle infinite loops.
   * For long delays (>= 1 hour), likely part of infinite loops:
   *   - First iteration: completes normally with minimal delay
   *   - Subsequent iterations: cancels the coroutine
   * For short delays: behaves normally
   */
  override suspend fun safeDelay(duration: Duration) {
    // Get information about the current coroutine
    val coroutineName = coroutineContext[CoroutineName]?.name ?: "unknown"

    // For large delays (infinite loops typically use large delays)
    if (duration.inWholeHours >= 1) {
      val count = loopIterationCounts.computeIfAbsent(coroutineName) { AtomicInteger(0) }.incrementAndGet()

      if (count == 1) {
        // First iteration - mark completion and add minimal delay
        firstIterationCompletions.computeIfAbsent(coroutineName) { CompletableDeferred() }.complete(Unit)
        delay(10) // minimal delay for stability
      } else {
        // Subsequent iterations - cancel the coroutine
        throw CancellationException("Test environment: stopping infinite loop after first iteration")
      }
    } else {
      // For short delays - execute normally
      delay(duration.inWholeMilliseconds)
    }
  }

  /**
   * Wait for the first iterations of all infinite loops to complete.
   */
  suspend fun awaitFirstIterations() {
    firstIterationCompletions.values.forEach { it.await() }
  }

  /**
   * Wait for all tracked regular jobs to complete.
   */
  suspend fun awaitRegularJobs() {
    var isReady = false
    while (!isReady) {
      isReady = true
      mainScope.coroutineContext.job.children.forEach { isReady = false; it.join() }
    }
  }
}