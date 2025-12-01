package com.fvlaenix.queemporium.di

import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DuplicateImageServiceConfig
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.coroutine.ProductionCoroutineProvider
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.service.AnswerServiceImpl
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.service.DuplicateImageServiceImpl
import org.koin.dsl.module
import java.util.logging.Logger

private val LOG = Logger.getLogger("KoinModule")

val applicationConfigModule = module {
  single { ApplicationConfig.load() }
}

val botConfigModule = module {
  single { BotConfiguration.load(get()) }
}

val coreServiceModule = module {
  single<BotCoroutineProvider> { ProductionCoroutineProvider() }
  single<AnswerService> { AnswerServiceImpl() }
}

val duplicateImageServiceModule = module {
  single { DuplicateImageServiceConfig.load(get()) }
  single<DuplicateImageService> { DuplicateImageServiceImpl(get()) }
}
