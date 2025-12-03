package com.fvlaenix.queemporium.testing.time

import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import com.fvlaenix.queemporium.testing.trace.TimeAdvanceEvent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

class VirtualClock(
  initialTime: Instant = Instant.now(),
  private val zone: ZoneId = ZoneId.of("UTC")
) : Clock() {
  @Volatile
  private var currentTime: Instant = initialTime
  private val listeners = CopyOnWriteArrayList<TimeAdvanceListener>()

  fun interface TimeAdvanceListener {
    suspend fun onTimeAdvanced(oldTime: Instant, newTime: Instant)
  }

  fun getCurrentTime(): Instant = currentTime

  override fun getZone(): ZoneId = zone

  override fun withZone(zone: ZoneId): Clock = VirtualClock(currentTime, zone).also { newClock ->
    // Note: newClock will drift independently if we don't share state.
    // For typical test usage, we reuse the same VirtualClock instance.
    // If precise zone support is needed with shared time, we'd need a shared state holder.
  }

  override fun instant(): Instant = currentTime

  fun addListener(listener: TimeAdvanceListener) {
    listeners.add(listener)
  }

  fun removeListener(listener: TimeAdvanceListener) {
    listeners.remove(listener)
  }

  suspend fun advanceTime(duration: Duration) {
    val oldTime = currentTime
    val newTime = oldTime.plusMillis(duration.inWholeMilliseconds)
    currentTime = newTime

    ScenarioTraceCollector.addEvent(TimeAdvanceEvent(Instant.now(), duration, newTime))

    listeners.forEach { listener ->
      listener.onTimeAdvanced(oldTime, newTime)
    }
  }

  suspend fun setTime(newTime: Instant) {
    val oldTime = currentTime
    if (newTime.isBefore(oldTime)) {
      throw IllegalArgumentException("Cannot move time backward (from $oldTime to $newTime)")
    }
    currentTime = newTime

    // Treat setTime as an advance of 0 duration or calc diff?
    // Better to capture it.
    val diff = java.time.Duration.between(oldTime, newTime)
    ScenarioTraceCollector.addEvent(
      TimeAdvanceEvent(
        Instant.now(),
        kotlin.time.Duration.parse("${diff.toMillis()}ms"),
        newTime
      )
    )

    listeners.forEach { listener ->
      listener.onTimeAdvanced(oldTime, newTime)
    }
  }
}
