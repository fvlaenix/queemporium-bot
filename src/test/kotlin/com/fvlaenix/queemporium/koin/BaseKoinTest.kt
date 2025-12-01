package com.fvlaenix.queemporium.koin

import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.coroutine.TestCoroutineProvider
import com.fvlaenix.queemporium.features.FeatureLoader
import com.fvlaenix.queemporium.features.FeatureRegistry
import com.fvlaenix.queemporium.service.AnswerService
import org.junit.jupiter.api.AfterEach
import org.koin.core.Koin
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.dsl.module

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
