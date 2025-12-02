package com.fvlaenix.queemporium.testing.time

import java.time.Instant
import kotlin.time.Duration

interface TimeController {
  fun getCurrentTime(): Instant
  suspend fun advanceTime(duration: Duration)
  suspend fun setTime(instant: Instant)
}

class VirtualTimeController(private val clock: VirtualClock) : TimeController {
  override fun getCurrentTime(): Instant = clock.getCurrentTime()

  override suspend fun advanceTime(duration: Duration) {
    clock.advanceTime(duration)
  }

  override suspend fun setTime(instant: Instant) {
    clock.setTime(instant)
  }
}
