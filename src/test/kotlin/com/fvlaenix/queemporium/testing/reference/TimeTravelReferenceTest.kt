package com.fvlaenix.queemporium.testing.reference

import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.testing.dsl.testBot
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

/**
 * Reference test demonstrating time-travel testing patterns with the new testBot DSL.
 *
 * Use this as a template for:
 * - Testing scheduled jobs
 * - Testing time-based behavior
 * - Deterministic timing tests
 */
class TimeTravelReferenceTest : BaseKoinTest() {

  @Test
  fun `reference - basic time advancement`() = testBot {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    setup {
      assertEquals(startTime, envWithTime.timeController!!.getCurrentTime())
    }

    scenario {
      advanceTime(4.hours)
    }

    scenario {
      val expectedTime = startTime.plusMillis(4.hours.inWholeMilliseconds)
      assertEquals(expectedTime, envWithTime.timeController!!.getCurrentTime())
    }
  }

  @Test
  fun `reference - testing with specific timestamps`() = testBot {
    val christmasEve = Instant.parse("2024-12-24T00:00:00Z")
    withVirtualTime(christmasEve)

    before {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    scenario {
      advanceTime(24.hours)
    }

    scenario {
      val christmas = Instant.parse("2024-12-25T00:00:00Z")
      assertEquals(christmas, envWithTime.timeController!!.getCurrentTime())
    }
  }

  @Test
  fun `reference - multiple time advancements`() = testBot {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    scenario {
      advanceTime(2.hours)
      advanceTime(3.hours)
      advanceTime(1.hours)
    }

    scenario {
      val expectedTime = startTime.plusMillis(6.hours.inWholeMilliseconds)
      assertEquals(expectedTime, envWithTime.timeController!!.getCurrentTime())
    }
  }

  @Test
  fun `reference - direct time controller access`() = testBot {
    val startTime = Instant.parse("2024-01-01T00:00:00Z")
    withVirtualTime(startTime)

    before {
      user("testUser")

      guild("testGuild") {
        channel("testChannel")
      }
    }

    scenario {
      val timeController = envWithTime.timeController!!
      timeController.advanceTime(5.hours)
      awaitAll()
    }

    scenario {
      val timeController = envWithTime.timeController!!
      val expectedTime = startTime.plusMillis(5.hours.inWholeMilliseconds)
      assertEquals(expectedTime, timeController.getCurrentTime())
    }
  }
}
