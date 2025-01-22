package com.fvlaenix.queemporium.configuration

import java.io.InputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

data class DuplicateImageServiceConfig(
  val hostname: String,
  val port: Int = 50055
) {
  companion object {
    private val LOG = Logger.getLogger(DuplicateImageServiceConfig::class.java.name)

    private fun getResourceStream(applicationConfig: ApplicationConfig): InputStream? {
      val duplicateImagePropertiesPath = applicationConfig.duplicateImagePropertiesPath
      if (duplicateImagePropertiesPath != null) {
        LOG.log(
          Level.INFO,
          "Using duplicate image service hostname from application config: $duplicateImagePropertiesPath"
        )
        try {
          return Path(duplicateImagePropertiesPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open database config file", e)
        }
      }

      val defaultStream = DuplicateImageServiceConfig::class.java.getResourceAsStream("/duplicate-image.properties")
      if (defaultStream != null) {
        LOG.log(Level.INFO, "Using default duplicate image service config file")
        return defaultStream
      }

      LOG.log(Level.WARNING, "No duplicate image service config found")
      return null
    }

    fun load(applicationConfig: ApplicationConfig): DuplicateImageServiceConfig {
      val inputStream =
        getResourceStream(applicationConfig) ?: return DuplicateImageServiceConfig(hostname = "localhost")
      val properties = Properties().apply { load(inputStream) }
      return DuplicateImageServiceConfig(
        hostname = properties.getProperty("hostname")
          ?: throw IllegalArgumentException("Property hostname not found")
      )
    }
  }
}