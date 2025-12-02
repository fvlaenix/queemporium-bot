package com.fvlaenix.queemporium.testing.time

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration

class VirtualClock(initialTime: Instant = Instant.now()) {
  @Volatile
  private var currentTime: Instant = initialTime
  private val listeners = CopyOnWriteArrayList<TimeAdvanceListener>()

  fun interface TimeAdvanceListener {
    suspend fun onTimeAdvanced(oldTime: Instant, newTime: Instant)
  }

  fun getCurrentTime(): Instant = currentTime

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

    listeners.forEach { listener ->
      listener.onTimeAdvanced(oldTime, newTime)
    }
  }
}
