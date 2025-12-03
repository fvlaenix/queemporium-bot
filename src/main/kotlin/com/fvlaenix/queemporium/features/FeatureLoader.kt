package com.fvlaenix.queemporium.features

import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.DuplicateImageServiceConfig
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.service.SearchService
import com.fvlaenix.queemporium.utils.Logging
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module

class FeatureLoader(
  private val koin: Koin,
  private val registry: FeatureRegistry = FeatureRegistry
) {
  private val log = Logging.getLogger(FeatureLoader::class.java)

  fun load(botConfiguration: BotConfiguration): LoadedFeatures {
    val modulesToLoad = LinkedHashSet<Module>()
    val enabledKeys = mutableListOf<String>()

    botConfiguration.features.forEach { (key, toggle) ->
      val definition = registry.definitions[key]
      if (definition == null) {
        log.warn("Unknown feature key: $key")
        return@forEach
      }
      if (!toggle.enabled) return@forEach
      enabledKeys.add(key)
      definition.requiredSharedModules
        .filter { shouldLoadSharedModule(it) }
        .forEach { modulesToLoad.add(it) }
      modulesToLoad.addAll(definition.modules(toggle))
    }

    if (modulesToLoad.isNotEmpty()) {
      log.info("Loading modules for features: $enabledKeys")
      loadKoinModules(modulesToLoad.toList())
    } else {
      log.info("No feature modules to load")
    }
    return LoadedFeatures(enabledKeys)
  }

  private fun shouldLoadSharedModule(module: Module): Boolean =
    when (module) {
      SharedModules.coreModule -> koin.getOrNull<BotCoroutineProvider>() == null || koin.getOrNull<AnswerService>() == null
      SharedModules.databaseModule -> koin.getOrNull<DatabaseConfiguration>() == null
      SharedModules.duplicateImageModule -> koin.getOrNull<DuplicateImageService>() == null &&
          koin.getOrNull<DuplicateImageServiceConfig>() == null

      SharedModules.searchModule -> koin.getOrNull<SearchService>() == null
      else -> true
    }
}

data class LoadedFeatures(val keys: List<String>)