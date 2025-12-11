package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val LOG = Logging.getLogger(HallOfFameUpdateDebouncer::class.java)

class HallOfFameUpdateDebouncer(
  private val coroutineProvider: BotCoroutineProvider,
  private val clock: Clock,
  private val debounceDuration: Duration = 30.seconds,
  private val onUpdate: suspend (messageId: String, guildId: String, newCount: Int) -> Unit
) {
  private data class MessageKey(val messageId: String, val guildId: String)

  private val pendingJobs = ConcurrentHashMap<MessageKey, Job>()

  suspend fun emit(messageId: String, guildId: String, newCount: Int) {
    val key = MessageKey(messageId, guildId)

    pendingJobs[key]?.cancel()

    val job = coroutineProvider.mainScope.launch(CoroutineName("hof-debounce-${key.messageId}")) {
      LOG.debug("Debouncing Hall of Fame update for ${key.messageId} in guild ${key.guildId} (count=$newCount)")
      coroutineProvider.safeDelay(debounceDuration)

      pendingJobs.remove(key)

      runCatching {
        onUpdate(key.messageId, key.guildId, newCount)
      }.onFailure { error ->
        LOG.error("Failed to apply Hall of Fame update for ${key.messageId} in guild ${key.guildId}", error)
      }
    }

    pendingJobs[key] = job
  }
}
