package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val LOG = Logging.getLogger(HallOfFameUpdateDebouncer::class.java)

// TODO make it Flow.debounce
class HallOfFameUpdateDebouncer(
  private val coroutineProvider: BotCoroutineProvider,
  private val clock: Clock,
  private val debounceDuration: Duration = 30.seconds,
  private val cleanupDuration: Duration = 5.minutes,
  private val onUpdate: suspend (messageId: String, guildId: String, newCount: Int) -> Unit
) {
  private data class MessageKey(val messageId: String, val guildId: String)

  private data class Holder(
    val job: Job,
    val cleanupJob: Job
  )

  private val holders = ConcurrentHashMap<MessageKey, Holder>()

  suspend fun emit(messageId: String, guildId: String, newCount: Int) {
    val key = MessageKey(messageId, guildId)
    val now = clock.instant()

    holders.compute(key) { _, existing ->
      existing?.job?.cancel()
      existing?.cleanupJob?.cancel()

      val updateJob = coroutineProvider.mainScope.launch(CoroutineName("hof-debounce-${key.messageId}")) {
        LOG.debug("Debouncing Hall of Fame update for ${key.messageId} in guild ${key.guildId} (count=$newCount)")
        coroutineProvider.safeDelay(debounceDuration)
        runCatching {
          onUpdate(key.messageId, key.guildId, newCount)
        }.onFailure { error ->
          LOG.error("Failed to apply Hall of Fame update for ${key.messageId} in guild ${key.guildId}", error)
        }
      }

      lateinit var cleanupJob: Job
      cleanupJob = coroutineProvider.mainScope.launch(CoroutineName("hof-debounce-cleanup-${key.messageId}")) {
        coroutineProvider.safeDelay(cleanupDuration)
        LOG.debug("Cleaning up Hall of Fame debouncer for ${key.messageId} after idle since $now")
        removeHolder(key, cleanupJob)
      }

      updateJob.invokeOnCompletion { removeHolder(key, updateJob) }
      cleanupJob.invokeOnCompletion { removeHolder(key, cleanupJob) }

      Holder(updateJob, cleanupJob)
    }
  }

  private fun removeHolder(key: MessageKey, job: Job) {
    val holder = holders[key] ?: return
    if (holder.job != job && holder.cleanupJob != job) return
    if (!holders.remove(key, holder)) return

    holder.job.cancel()
    holder.cleanupJob.cancel()
  }
}
