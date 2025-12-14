package com.fvlaenix.queemporium.features

import com.fvlaenix.queemporium.commands.SearchConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.S3Configuration
import com.fvlaenix.queemporium.di.coreServiceModule
import com.fvlaenix.queemporium.di.duplicateImageServiceModule
import com.fvlaenix.queemporium.service.S3FileService
import com.fvlaenix.queemporium.service.S3FileServiceImpl
import com.fvlaenix.queemporium.service.SearchService
import com.fvlaenix.queemporium.service.SearchServiceImpl
import org.koin.dsl.module

object SharedModules {
  val coreModule = module {
    includes(coreServiceModule)
    single<java.time.Clock> { java.time.Clock.systemUTC() }
  }

  val databaseModule = module {
    single { DatabaseConfiguration.load(get()) }
  }

  val duplicateImageModule = duplicateImageServiceModule

  val searchModule = module {
    single {
      SearchConfiguration.load(get())
        ?: throw IllegalStateException("Search configuration is required for search feature")
    }
    single<SearchService> { SearchServiceImpl(get()) }
  }

  val s3Module = module {
    single { S3Configuration.load(get()) }
    single<S3FileService> { S3FileServiceImpl(get()) }
  }
}
