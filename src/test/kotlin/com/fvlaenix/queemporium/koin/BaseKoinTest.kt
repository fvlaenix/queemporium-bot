package com.fvlaenix.queemporium.koin

import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.coroutine.TestCoroutineProvider
import com.fvlaenix.queemporium.features.FeatureLoader
import com.fvlaenix.queemporium.features.FeatureRegistry
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.testing.trace.ScenarioTestWatcher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.Koin
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module

@ExtendWith(ScenarioTestWatcher::class)
abstract class BaseKoinTest {
  fun setupBotKoin(configBlock: BotConfigBuilder.() -> Unit): Koin {

    val configBuilder = BotConfigBuilder()
    configBuilder.configBlock()

    val testConfigModule = module {
      single<BotCoroutineProvider> { TestCoroutineProvider() }
      single<ApplicationConfig> { configBuilder.applicationConfig }
      single<DatabaseConfiguration> { configBuilder.databaseConfig }
      single<BotConfiguration> { configBuilder.botConfiguration }
      single<AnswerService> { configBuilder.answerService }
      single<java.time.Clock> { java.time.Clock.systemUTC() }
    }

    val koin = startKoin {
      allowOverride(true)
      modules(testConfigModule)
    }.koin

    FeatureLoader(koin, FeatureRegistry).load(configBuilder.botConfiguration)

    return koin
  }

  @AfterEach
  fun tearDownKoin() {
    stopKoin()
  }
}
