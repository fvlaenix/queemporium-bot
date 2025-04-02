package com.fvlaenix.queemporium.di

import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.commands.SearchConfiguration
import com.fvlaenix.queemporium.configuration.*
import com.fvlaenix.queemporium.service.*
import org.koin.core.Koin
import org.koin.dsl.module
import java.util.logging.Level
import java.util.logging.Logger

private val LOG = Logger.getLogger("KoinModule")

val applicationConfigModule = module {
  single { ApplicationConfig.load() }
}

val configurationModule = module {
  LOG.log(Level.INFO, "Initializing configuration module")
  single {
    LOG.log(Level.INFO, "Initializing database configuration")
    DatabaseConfiguration.load(get())
  }
  single {
    LOG.log(Level.INFO, "Initializing metadata configuration")
    MetadataConfiguration.load(get())
  }
  single {
    LOG.log(Level.INFO, "Initializing bot configuration")
    BotConfiguration.load(get())
  }
}

val productionServiceModule = module {
  single<AnswerService> { AnswerServiceImpl() }
  single { DuplicateImageServiceConfig.load(get()) }
  single<DuplicateImageService> { DuplicateImageServiceImpl(get()) }
  single { SearchConfiguration.load(get()) }
  single<SearchService> { SearchServiceImpl(get()) }
}

fun Koin.loadCoreModule() {
  declare(CommandsServiceImpl(this, get<MetadataConfiguration>()))
  declare(DiscordBot(get(), get<CommandsServiceImpl>()))
}