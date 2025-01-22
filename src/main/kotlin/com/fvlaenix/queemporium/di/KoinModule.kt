package com.fvlaenix.queemporium.di

import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.MetadataConfiguration
import com.fvlaenix.queemporium.service.CommandsServiceImpl
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

fun Koin.loadCoreModule() {
  declare(CommandsServiceImpl(this, get<MetadataConfiguration>()))
  declare(DiscordBot(get(), get<CommandsServiceImpl>()))
}