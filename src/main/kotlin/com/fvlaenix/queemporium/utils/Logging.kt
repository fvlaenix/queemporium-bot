package com.fvlaenix.queemporium.utils

import kotlinx.coroutines.slf4j.MDCContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.coroutines.CoroutineContext

object LogContextKeys {
  const val GUILD_ID = "guildId"
  const val CHANNEL_ID = "channelId"
  const val COMMAND = "command"
  const val SCENARIO = "scenario"
  const val STEP = "step"
  const val USER_ID = "userId"
}

object Logging {
  fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
  fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)
}

/**
 * Executes the block with the given logging context (MDC).
 * The context is restored after the block execution.
 */
inline fun <T> withLoggingContext(context: Map<String, String?>, block: () -> T): T {
  val previousContext = MDC.getCopyOfContextMap()
  try {
    context.forEach { (key, value) ->
      if (value != null) {
        MDC.put(key, value)
      }
    }
    return block()
  } finally {
    if (previousContext != null) {
      MDC.setContextMap(previousContext)
    } else {
      MDC.clear()
    }
  }
}

/**
 * Creates a CoroutineContext element that propagates the current MDC context
 * plus any additional key-values provided.
 */
fun mdcCoroutineContext(additionalContext: Map<String, String?> = emptyMap()): CoroutineContext {
  val currentMap = MDC.getCopyOfContextMap() ?: emptyMap()
  val newMap = currentMap + additionalContext.filterValues { it != null }.mapValues { it.value!! }
  return MDCContext(newMap)
}
