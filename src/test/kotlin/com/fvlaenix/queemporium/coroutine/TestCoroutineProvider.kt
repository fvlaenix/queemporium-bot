package com.fvlaenix.queemporium.coroutine

import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.time.Duration

private data class PendingDelay(
  val targetTime: Instant,
  val continuation: CancellableContinuation<Unit>
) : Comparable<PendingDelay> {
  override fun compareTo(other: PendingDelay): Int {
    return targetTime.compareTo(other.targetTime)
  }
}

class TestCoroutineProvider(
  private val virtualClock: VirtualClock? = null
) : BotCoroutineProvider {
  private val firstIterationCompletions = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
  private val loopIterationCounts = ConcurrentHashMap<String, AtomicInteger>()
  private val pendingDelays = PriorityBlockingQueue<PendingDelay>()

  @OptIn(DelicateCoroutinesApi::class)
  override val botPool = newFixedThreadPoolContext(2, "TestBotPool")

  override val mainScope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, ex ->
    if (ex !is CancellationException || ex.message?.contains("after first iteration") == true) {
      println("Error in test coroutine: ${ex.message}")
      ex.printStackTrace()
    }
  })

  init {
    virtualClock?.addListener { oldTime, newTime ->
      processDelaysUntil(newTime)
    }
  }

  override suspend fun safeDelay(duration: Duration) {
    if (virtualClock != null) {
      delayWithVirtualClock(duration)
    } else {
      delayWithInfiniteLoopHandling(duration)
    }
  }

  private suspend fun delayWithVirtualClock(duration: Duration) {
    val targetTime = virtualClock!!.getCurrentTime().plusMillis(duration.inWholeMilliseconds)

    suspendCancellableCoroutine { continuation ->
      val pendingDelay = PendingDelay(targetTime, continuation)
      continuation.invokeOnCancellation {
        pendingDelays.removeIf { it.continuation == continuation }
      }
      pendingDelays.add(pendingDelay)
    }
  }

  private suspend fun delayWithInfiniteLoopHandling(duration: Duration) {
    val coroutineName = coroutineContext[CoroutineName]?.name ?: "unknown"

    if (duration.inWholeHours >= 1) {
      val count = loopIterationCounts.computeIfAbsent(coroutineName) { AtomicInteger(0) }.incrementAndGet()

      if (count == 1) {
        firstIterationCompletions.computeIfAbsent(coroutineName) { CompletableDeferred() }.complete(Unit)
        delay(10)
      } else {
        throw CancellationException("Test environment: stopping infinite loop after first iteration")
      }
    } else {
      delay(duration.inWholeMilliseconds)
    }
  }

  private fun processDelaysUntil(newTime: Instant) {
    val delaysToResume = mutableListOf<PendingDelay>()

    while (true) {
      val nextDelay = pendingDelays.peek()
      if (nextDelay == null || nextDelay.targetTime.isAfter(newTime)) {
        break
      }
      pendingDelays.poll()
      delaysToResume.add(nextDelay)
    }

    delaysToResume.forEach { delay ->
      if (delay.continuation.isActive) {
        delay.continuation.resume(Unit)
      }
    }
  }

  suspend fun awaitFirstIterations() {
    firstIterationCompletions.values.forEach { it.await() }
  }

  suspend fun awaitRegularJobs() {
    while (true) {
      val children = mainScope.coroutineContext.job.children.toList()
      val activeChildren = children.filter { it.isActive }

      if (activeChildren.isEmpty()) {
        break
      }

      // If all active children are waiting in pendingDelays, we consider them "quiescent"
      // Note: This assumes 1:1 mapping of active jobs to pending delays for infinite loops
      // A better approach would be to track "busy" jobs, but this might suffice for now.
      if (activeChildren.size <= pendingDelays.size) {
        break
      }

      // Wait a bit and check again. We can't join() because it blocks until completion.
      delay(10)
    }
  }

  fun getPendingDelaysCount(): Int = pendingDelays.size

  fun getNextDelayTime(): Instant? = pendingDelays.peek()?.targetTime
}
