package com.fvlaenix.queemporium.coroutine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ProductionCoroutineProviderTest {
  @Test
  fun failedJobDoesNotStopLaterJobs() = runBlocking {
    val provider = ProductionCoroutineProvider()
    try {
      val failedJob = provider.mainScope.launch(CoroutineExceptionHandler { _, _ -> }) {
        coroutineScope {
          launch {
            throw IllegalStateException("simulated message failure")
          }
        }
      }
      withTimeout(1_000) { failedJob.join() }

      assertTrue(provider.mainScope.coroutineContext.job.isActive)

      val nextMessageProcessed = CompletableDeferred<Unit>()
      val nextJob = provider.mainScope.launch {
        nextMessageProcessed.complete(Unit)
      }
      withTimeout(1_000) { nextMessageProcessed.await() }
      nextJob.join()
    } finally {
      provider.mainScope.cancel()
      provider.botPool.close()
    }
  }
}
