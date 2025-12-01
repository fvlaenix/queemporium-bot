package com.fvlaenix.queemporium.configuration

import java.io.InputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

private val LOG = Logger.getLogger(ApplicationConfig::class.java.simpleName)

data class ApplicationConfig(
  val databasePropertiesPath: String?,
  val botPropertiesPath: String?,
  val searchPropertiesPath: String?,
  val duplicateImagePropertiesPath: String?
) {
  companion object {
    private const val CONFIG_PATH_ENV = "APP_CONFIG_PATH"

    private fun getResourceStream(): InputStream {
      val configPath = System.getenv(CONFIG_PATH_ENV)
      if (configPath != null) {
        LOG.log(Level.INFO, "Using config path from env: $configPath")
        try {
          return Path(configPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open master config file", e)
        }
      }
      val standardResourceStream = ApplicationConfig::class.java.getResourceAsStream("/application.properties")
      if (standardResourceStream != null) {
        LOG.log(Level.INFO, "Using standard config file")
        return standardResourceStream
      }
      LOG.log(Level.INFO, "Using default config file")
      return "".byteInputStream()
    }

    fun load(): ApplicationConfig {
      val properties = Properties().apply { load(getResourceStream()) }

      return ApplicationConfig(
        databasePropertiesPath = properties.getProperty("database.properties.path"),
        botPropertiesPath = properties.getProperty("bot.properties.path"),
        searchPropertiesPath = properties.getProperty("search.properties.path"),
        duplicateImagePropertiesPath = properties.getProperty("duplicate.image.properties.path")
      )
    }
  }
}
