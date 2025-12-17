package com.fvlaenix.queemporium.testing.log

import com.fvlaenix.queemporium.koin.BaseKoinTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LogLevelEnforcementTest : BaseKoinTest() {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Test
  fun `test passes when no WARN or ERROR logs are produced`() {
    logger.info("This is an info log")
    logger.debug("This is a debug log")
  }

  @Test
  fun `test passes when expected ERROR log is produced`() {
    expectLogs {
      error("com.fvlaenix.queemporium.testing.log.LogLevelEnforcementTest", count = 1)
    }

    logger.error("Expected error log")
  }

  @Test
  fun `test passes when expected WARN log is produced`() {
    expectLogs {
      warn("com.fvlaenix.queemporium.testing.log.LogLevelEnforcementTest", count = 1)
    }

    logger.warn("Expected warn log")
  }

  @Test
  fun `test passes when multiple expected logs are produced`() {
    expectLogs {
      error("com.fvlaenix.queemporium.testing.log.LogLevelEnforcementTest", count = 2)
      warn("com.fvlaenix.queemporium.testing.log.LogLevelEnforcementTest", count = 1)
    }

    logger.error("First error")
    logger.error("Second error")
    logger.warn("A warning")
  }

  @Test
  fun `test passes when expected log with message contains is produced`() {
    expectLogs {
      error(
        "com.fvlaenix.queemporium.testing.log.LogLevelEnforcementTest",
        count = 1,
        messageContains = "specific message"
      )
    }

    logger.error("This is a specific message that should match")
  }
}
