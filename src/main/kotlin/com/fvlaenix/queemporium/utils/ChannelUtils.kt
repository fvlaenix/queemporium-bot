package com.fvlaenix.queemporium.utils

import io.grpc.ManagedChannel
import io.grpc.kotlin.AbstractCoroutineStub
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

private val LOG = Logger.getLogger(ChannelUtils::class.java.name)

object ChannelUtils {
  const val STANDARD_IMAGE_CHANNEL_SIZE = 50 * 1024 * 1024 // 50 mb
  
  suspend fun <T, COROUTINE_STUB: AbstractCoroutineStub<*>> runWithClose(channel: ManagedChannel, service: COROUTINE_STUB, body: suspend (COROUTINE_STUB) -> T): T {
    return try {
      body(service)
    } finally {
      channel.shutdown()
      if (!channel.awaitTermination(10, TimeUnit.SECONDS)) {
        LOG.log(Level.SEVERE, "Can't close connection in 10 seconds")
      }
    }
  }

  fun checkServerAliveness(serviceName: String, body: suspend () -> Unit): Boolean {
    try {
      runBlocking {
        body()
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Can't open connection to $serviceName", e)
      return false
    }
    return true
  }
}