package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.coroutine.TestCoroutineProvider
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class HallOfFameUpdateDebouncerTest {

  @Test
  fun `emits latest count once after debounce window`() = runBlocking {
    val clock = VirtualClock()
    val coroutineProvider = TestCoroutineProvider(clock)
    val updates = mutableListOf<Int>()

    val debouncer = HallOfFameUpdateDebouncer(
      coroutineProvider = coroutineProvider,
      clock = clock
    ) { _, _, newCount ->
      updates.add(newCount)
    }

    debouncer.emit(messageId = "message", guildId = "guild", newCount = 5)
    clock.advanceTime(20.seconds)
    coroutineProvider.awaitRegularJobs()
    assertEquals(emptyList<Int>(), updates)

    debouncer.emit(messageId = "message", guildId = "guild", newCount = 7)
    clock.advanceTime(10.seconds)
    coroutineProvider.awaitRegularJobs()
    assertEquals(emptyList<Int>(), updates)

    clock.advanceTime(30.seconds)
    coroutineProvider.awaitRegularJobs()
    assertEquals(listOf(7), updates)

    debouncer.emit(messageId = "message", guildId = "guild", newCount = 9)
    debouncer.emit(messageId = "message", guildId = "guild", newCount = 11)
    clock.advanceTime(5.seconds)
    coroutineProvider.awaitRegularJobs()
    assertEquals(listOf(7), updates)

    clock.advanceTime(30.seconds)
    coroutineProvider.awaitRegularJobs()
    assertEquals(listOf(7, 11), updates)
  }
}
