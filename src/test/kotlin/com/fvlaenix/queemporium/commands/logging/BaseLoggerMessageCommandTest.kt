package com.fvlaenix.queemporium.commands.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.testing.dsl.BotTestFixture
import com.fvlaenix.queemporium.testing.dsl.BotTestScenarioContext
import com.fvlaenix.queemporium.testing.dsl.testBotFixture
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseLoggerMessageCommandTest : BaseKoinTest() {
  protected lateinit var fixture: BotTestFixture

  protected open var autoStartEnvironment: Boolean = true

  @BeforeEach
  fun baseSetUp() = runBlocking {
    fixture = testBotFixture {
      before {
        enableFeatures(*getFeaturesForTest())

        user("Test User")

        guild("Test Guild") {
          channel("general")
        }
      }
    }

    fixture.autoStart = autoStartEnvironment
    fixture.initialize(this@BaseLoggerMessageCommandTest)

    additionalSetUp()
  }

  @AfterEach
  fun tearDown() {
    fixture.cleanup()
  }

  protected open fun additionalSetUp() {
  }

  protected abstract fun getFeaturesForTest(): Array<String>

  protected fun clearLogs() = runBlocking {
    fixture.runScenario {
      logger.clearLogs()
    }
  }

  protected fun getLogsContaining(text: String): List<ILoggingEvent> = runBlocking {
    var result: List<ILoggingEvent> = emptyList()
    fixture.runScenario {
      result = logger.getLogsContaining(text)
    }
    result
  }

  protected fun <T> runWithScenario(block: suspend BotTestScenarioContext.() -> T): T = runBlocking {
    var result: T? = null
    fixture.runScenario {
      result = block()
    }
    @Suppress("UNCHECKED_CAST")
    result as T
  }

  protected val env: TestEnvironment
    get() = runWithScenario { envWithTime.environment }

  protected val jda: JDA
    get() = env.jda
}
