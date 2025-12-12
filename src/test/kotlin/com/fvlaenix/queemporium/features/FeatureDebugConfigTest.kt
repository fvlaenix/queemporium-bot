package com.fvlaenix.queemporium.features

import com.fvlaenix.queemporium.commands.halloffame.HallOfFameCommand
import com.fvlaenix.queemporium.configuration.FeatureToggle
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.koin.createInMemoryDatabaseConfig
import com.fvlaenix.queemporium.service.AnswerService
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import java.time.Clock
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureDebugConfigTest {

  @AfterEach
  fun tearDown() {
    GlobalContext.stopKoin()
  }

  @Test
  fun `hall of fame feature reads debug flag from params`() {
    val definition = FeatureRegistry.definitions[FeatureKeys.HALL_OF_FAME]!!

    val params = JsonObject(mapOf("debug" to JsonPrimitive(true)))
    val toggle = FeatureToggle(enabled = true, params = params)

    val modules = definition.modules(toggle)

    // Setup Koin with in-memory DB
    val koinApp = GlobalContext.startKoin {
      modules(module {
        single { createInMemoryDatabaseConfig() }
        single { mockk<AnswerService>(relaxed = true) }
        single { mockk<BotCoroutineProvider>(relaxed = true) }
        single { Clock.systemUTC() }
      })
      modules(modules)
    }

    val command = koinApp.koin.get<HallOfFameCommand>()

    // Use reflection to check the debug flag
    val debugField = HallOfFameCommand::class.java.getDeclaredField("debug")
    debugField.isAccessible = true
    val isDebug = debugField.getBoolean(command)

    assertTrue(isDebug, "Debug flag should be true when configured in params")
  }

  @Test
  fun `hall of fame feature defaults debug flag to false`() {
    val definition = FeatureRegistry.definitions[FeatureKeys.HALL_OF_FAME]!!

    val toggle = FeatureToggle(enabled = true) // empty params

    val modules = definition.modules(toggle)

    val koinApp = GlobalContext.startKoin {
      modules(module {
        single { createInMemoryDatabaseConfig() }
        single { mockk<AnswerService>(relaxed = true) }
        single { mockk<BotCoroutineProvider>(relaxed = true) }
        single { Clock.systemUTC() }
      })
      modules(modules)
    }

    val command = koinApp.koin.get<HallOfFameCommand>()

    val debugField = HallOfFameCommand::class.java.getDeclaredField("debug")
    debugField.isAccessible = true
    val isDebug = debugField.getBoolean(command)

    assertFalse(isDebug, "Debug flag should be false when not configured")
  }
}
