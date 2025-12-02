package com.fvlaenix.queemporium.testing.reference

import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.fixture.fixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.scenario.runScenario
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

/**
 * Reference test demonstrating time-travel testing patterns.
 *
 * Use this as a template for:
 * - Testing scheduled jobs
 * - Testing time-based behavior
 * - Deterministic timing tests
 */
class TimeTravelReferenceTest : BaseKoinTest() {

  @Test
  fun `reference - basic time advancement`() = runBlocking {
    // 1. Setup: Create virtual clock with fixed start time
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    // 2. Define fixture
    val testFixture = fixture {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    // 3. Initialize with virtual clock
    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    // 4. Verify initial time
    assertEquals(startTime, envWithTime.timeController!!.getCurrentTime())

    // 5. Advance time in scenario
    envWithTime.runScenario(answerService) {
      // Advance time by 4 hours
      advanceTime(4.hours)
    }

    // 6. Verify time advanced
    val expectedTime = startTime.plusMillis(4.hours.inWholeMilliseconds)
    assertEquals(expectedTime, envWithTime.timeController!!.getCurrentTime())
  }

  @Test
  fun `reference - testing with specific timestamps`() = runBlocking {
    // Use a meaningful start time
    val christmasEve = Instant.parse("2024-12-24T00:00:00Z")
    val virtualClock = VirtualClock(christmasEve)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // Advance to Christmas Day
      advanceTime(24.hours)
    }

    // Verify we're on Christmas
    val christmas = Instant.parse("2024-12-25T00:00:00Z")
    assertEquals(christmas, envWithTime.timeController!!.getCurrentTime())
  }

  @Test
  fun `reference - multiple time advancements`() = runBlocking {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    envWithTime.runScenario(answerService) {
      // First advancement
      advanceTime(2.hours)

      // Do something...

      // Second advancement
      advanceTime(3.hours)

      // Do something else...

      // Third advancement
      advanceTime(1.hours)
    }

    // Total: 2 + 3 + 1 = 6 hours advanced
    val expectedTime = startTime.plusMillis(6.hours.inWholeMilliseconds)
    assertEquals(expectedTime, envWithTime.timeController!!.getCurrentTime())
  }

  @Test
  fun `reference - time advancement without scenario DSL`() = runBlocking {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    val virtualClock = VirtualClock(startTime)
    val answerService = MockAnswerService()

    val testFixture = fixture {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    val envWithTime = setupWithFixture(testFixture, virtualClock) { builder ->
      builder.answerService = answerService
    }

    // You can also advance time directly via timeController
    val timeController = envWithTime.timeController!!

    timeController.advanceTime(5.hours)
    envWithTime.awaitAll()

    val expectedTime = startTime.plusMillis(5.hours.inWholeMilliseconds)
    assertEquals(expectedTime, timeController.getCurrentTime())
  }
}
