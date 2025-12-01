package com.fvlaenix.queemporium.features

import com.fvlaenix.queemporium.configuration.FeatureToggle
import org.koin.core.module.Module

data class FeatureDefinition(
  val key: String,
  val requiredSharedModules: List<Module> = emptyList(),
  val modules: FeatureModuleBuilder
)

typealias FeatureModuleBuilder = (FeatureToggle) -> List<Module>
