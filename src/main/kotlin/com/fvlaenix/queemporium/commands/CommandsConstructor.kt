package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import net.dv8tion.jda.api.hooks.ListenerAdapter

object CommandsConstructor {
  private val PACKAGES_CANDIDATES: List<String> = listOf(
    "com.fvlaenix.queemporium.commands.duplicate.",
    "com.fvlaenix.queemporium.commands.",
  )
  
  private val STANDARD_FEATURES: List<String> = listOf(
    "OnlinePictureCompare",
    "DependentDeleterCommand",
    "ExcludeChannelCommand",
    "LoggerMessageCommand",
    "SetDuplicateChannelCommand"
  )
  
  fun convert(botConfiguration: BotConfiguration, databaseConfiguration: DatabaseConfiguration): List<ListenerAdapter> {
    val commands = mutableListOf<ListenerAdapter>()
    val features = STANDARD_FEATURES + botConfiguration.features
    features.forEach { featureCommand ->
      val (feature, isAdd) = if (featureCommand.startsWith("no-")) {
        featureCommand.removePrefix("no-") to false
      } else {
        featureCommand to true
      }
      val clazz = PACKAGES_CANDIDATES.firstNotNullOfOrNull { packageCandidate ->
        try {
          Class.forName(packageCandidate + feature).kotlin
        } catch (e: ClassNotFoundException) { null }
      } ?: throw Exception("Can't find class $feature. Please make sure you wrote it correctly")
      check(clazz.java.interfaces.contains(ListenerAdapter::class.java)) { "Class ${clazz.java.name} should inherit interface ${ListenerAdapter::class.qualifiedName}" }
      if (!isAdd) {
        commands.removeAll { clazz.isInstance(it) }
        return@forEach
      }
      check(commands.none { clazz.isInstance(it) }) { "Class $clazz was added twice" }
      val constructors = clazz.constructors
      check(constructors.size == 1) { "Class $feature should have only one constructor" }
      val constructor = constructors.single()
      val parameters: List<Any> = constructor.parameters.map { kParameter -> 
        when (kParameter.type) {
          DatabaseConfiguration::class -> databaseConfiguration
          else -> throw IllegalArgumentException("Unsupported parameter type: ${kParameter.type}")
        }
      }
      
      val newCommand = constructor.call(parameters) as ListenerAdapter
      commands.add(newCommand)
    }
    return commands
  }
}