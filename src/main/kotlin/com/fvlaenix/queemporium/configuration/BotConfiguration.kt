package com.fvlaenix.queemporium.configuration

import java.io.InputStream
import java.util.*

data class BotConfiguration(
  val token: String,
  val features: List<String>,
  val commandConfigs: Map<String, Map<String, String>>
) {
  companion object {
    fun Properties.getSafeProperty(name: String): String = getProperty(name) ?: throw IllegalArgumentException("Property $name not found")
  }
  
  constructor(properties: Properties) : this(
    token = properties.getSafeProperty("token"),
    features = properties.getSafeProperty("features").split(",").map { it.trim() },
    commandConfigs = properties.keys().toList()
      .filterIsInstance<String>()
      .filter { it.startsWith("command.configuration.") }
      .associate { throw IllegalArgumentException("Can't parse commands configuration now: $it") }
  )

  constructor(inputStream: InputStream) : this(Properties().apply { load(inputStream) })
}