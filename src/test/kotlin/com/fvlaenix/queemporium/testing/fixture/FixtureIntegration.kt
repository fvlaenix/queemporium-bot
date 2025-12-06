package com.fvlaenix.queemporium.testing.fixture

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.coroutine.TestCoroutineProvider
import com.fvlaenix.queemporium.koin.BotConfigBuilder
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.testing.time.TimeController
import com.fvlaenix.queemporium.testing.time.VirtualClock
import com.fvlaenix.queemporium.testing.time.VirtualTimeController
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import org.koin.dsl.module

fun setupWithFixture(
  fixture: TestFixture,
  virtualClock: VirtualClock? = null,
  autoStart: Boolean = true,
  configureBuilder: (BotConfigBuilder) -> Unit = {}
): TestEnvironmentWithTime {
  // Capture fixture snapshot for tracing
  ScenarioTraceCollector.setFixtureSnapshot(fixture.toString())

  val testProvider = if (virtualClock != null) {
    TestCoroutineProvider(virtualClock)
  } else {
    TestCoroutineProvider()
  }

  setupBotKoinWithProvider(testProvider, virtualClock) {
    enableFeatures(*fixture.enabledFeatures.toTypedArray())
    configureBuilder(this)
  }

  val builder = fixture.buildEnvironment(virtualClock, autoStart)
  val environment = builder.build()
  val timeController = virtualClock?.let { VirtualTimeController(it) }

  return builder.toTestEnvironmentWithTime(timeController, testProvider, environment)
}

fun setupBotKoinWithProvider(
  testProvider: TestCoroutineProvider,
  virtualClock: VirtualClock? = null,
  configBlock: BotConfigBuilder.() -> Unit
): org.koin.core.Koin {
  val configBuilder = com.fvlaenix.queemporium.koin.BotConfigBuilder()
  configBuilder.configBlock()

  println("DEBUG: setupBotKoinWithProvider registering AnswerService: ${System.identityHashCode(configBuilder.answerService)}")

  val testConfigModule = module {
    single<BotCoroutineProvider> { testProvider }
    single<com.fvlaenix.queemporium.configuration.ApplicationConfig> { configBuilder.applicationConfig }
    single<com.fvlaenix.queemporium.configuration.DatabaseConfiguration> { configBuilder.databaseConfig }
    single<com.fvlaenix.queemporium.configuration.BotConfiguration> { configBuilder.botConfiguration }
    single<com.fvlaenix.queemporium.service.AnswerService> { configBuilder.answerService }
    if (virtualClock != null) {
      single<java.time.Clock> { virtualClock }
    } else {
      single<java.time.Clock> { java.time.Clock.systemUTC() }
    }
  }

  val koin = org.koin.core.context.GlobalContext.startKoin {
    allowOverride(true)
    modules(testConfigModule)
  }.koin

  com.fvlaenix.queemporium.features.FeatureLoader(koin, com.fvlaenix.queemporium.features.FeatureRegistry)
    .load(configBuilder.botConfiguration)

  return koin
}

data class TestEnvironmentWithTime(
  val environment: TestEnvironment,
  val timeController: TimeController?,
  val testProvider: TestCoroutineProvider,
  val userMap: Map<String, net.dv8tion.jda.api.entities.User> = emptyMap()
)

suspend fun TestEnvironmentWithTime.awaitAll() {
  if (timeController == null) {
    environment.awaitAll()
  } else {
    testProvider.awaitRegularJobs()
  }
}

fun setupWithFixtureAndModules(
  fixture: TestFixture,
  virtualClock: VirtualClock? = null,
  autoStart: Boolean = true,
  additionalModules: List<org.koin.core.module.Module> = emptyList(),
  configureBuilder: (BotConfigBuilder) -> Unit = {}
): TestEnvironmentWithTime {
  ScenarioTraceCollector.setFixtureSnapshot(fixture.toString())

  val testProvider = if (virtualClock != null) {
    TestCoroutineProvider(virtualClock)
  } else {
    TestCoroutineProvider()
  }

  val configBuilder = com.fvlaenix.queemporium.koin.BotConfigBuilder()
  configBuilder.enableFeatures(*fixture.enabledFeatures.toTypedArray())
  configureBuilder(configBuilder)

  println("DEBUG: setupWithFixtureAndModules registering AnswerService: ${System.identityHashCode(configBuilder.answerService)}")

  val testConfigModule = org.koin.dsl.module {
    single<BotCoroutineProvider> { testProvider }
    single<com.fvlaenix.queemporium.configuration.ApplicationConfig> { configBuilder.applicationConfig }
    single<com.fvlaenix.queemporium.configuration.DatabaseConfiguration> { configBuilder.databaseConfig }
    single<com.fvlaenix.queemporium.configuration.BotConfiguration> { configBuilder.botConfiguration }
    single<com.fvlaenix.queemporium.service.AnswerService> { configBuilder.answerService }
    if (virtualClock != null) {
      single<java.time.Clock> { virtualClock }
    } else {
      single<java.time.Clock> { java.time.Clock.systemUTC() }
    }
  }

  val koin = org.koin.core.context.GlobalContext.startKoin {
    allowOverride(true)
    modules(testConfigModule)
  }.koin

  if (additionalModules.isNotEmpty()) {
    koin.loadModules(additionalModules)
  }

  com.fvlaenix.queemporium.features.FeatureLoader(koin, com.fvlaenix.queemporium.features.FeatureRegistry)
    .load(configBuilder.botConfiguration)

  val builder = fixture.buildEnvironment(virtualClock, autoStart)
  val environment = builder.build()
  val timeController = virtualClock?.let { VirtualTimeController(it) }

  return builder.toTestEnvironmentWithTime(timeController, testProvider, environment)
}
