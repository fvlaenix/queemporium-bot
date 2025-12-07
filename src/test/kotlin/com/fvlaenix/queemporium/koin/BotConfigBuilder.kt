package com.fvlaenix.queemporium.koin

import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.FeatureToggle
import com.fvlaenix.queemporium.service.AnswerService
import io.mockk.mockk

class BotConfigBuilder {
  var applicationConfig: ApplicationConfig = mockk(relaxed = true)
  var databaseConfig: DatabaseConfiguration = createInMemoryDatabaseConfig()
  var botConfiguration: BotConfiguration = BotConfiguration(token = "test-token", features = emptyMap())
  var answerService: AnswerService = mockk()

  fun enableFeatures(vararg featureKeys: String) {
    botConfiguration = botConfiguration.copy(
      features = featureKeys.associateWith { FeatureToggle(enabled = true) }
    )
  }
}
