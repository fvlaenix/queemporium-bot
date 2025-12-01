package com.fvlaenix.queemporium.configuration

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

@Serializable
data class BotConfiguration(
  val token: String,
  val features: Map<String, FeatureToggle> = emptyMap()
) {
  companion object {
    private val LOG = Logger.getLogger(BotConfiguration::class.java.name)

    fun load(applicationConfig: ApplicationConfig): BotConfiguration {
      val botPropertiesPath = applicationConfig.botPropertiesPath
        ?: throw IllegalStateException("bot.properties.path is not set")

      LOG.log(Level.INFO, "Using bot configuration path from application config: $botPropertiesPath")
      val inputStream = try {
        Path(botPropertiesPath).inputStream()
      } catch (e: Exception) {
        throw Exception("Can't open bot config file", e)
      }

      return try {
        Yaml.default.decodeFromStream(serializer(), inputStream)
      } catch (e: Exception) {
        throw Exception("Failed to parse bot configuration YAML", e)
      }
    }
  }
}

@Serializable
data class FeatureToggle(
  val enabled: Boolean = false,
  @SerialName("params")
  val params: JsonObject = JsonObject(emptyMap())
)
