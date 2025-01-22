package com.fvlaenix.queemporium.configuration

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

data class DatabaseConfiguration(
  val url: String,
  val driver: String,
  val user: String,
  val password: String,
) {
  companion object {
    private val LOG = Logger.getLogger(DatabaseConfiguration::class.java.name)

    private fun Properties.getSafeProperty(name: String): String =
      getProperty(name) ?: throw IllegalArgumentException("Property $name not found")

    private fun getResourceStream(applicationConfig: ApplicationConfig): InputStream {
      val applicationConfigPropertyPath = applicationConfig.databasePropertiesPath
      if (applicationConfigPropertyPath != null) {
        LOG.log(Level.INFO, "Using database path from application config: $applicationConfigPropertyPath")
        try {
          return Path(applicationConfigPropertyPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open database config file", e)
        }
      }

      val defaultStream = DatabaseConfiguration::class.java.getResourceAsStream("/database.properties")
      if (defaultStream != null) {
        LOG.log(Level.INFO, "Using default database config file")
        return defaultStream
      }

      throw IllegalStateException("No database config file found")
    }

    fun load(applicationConfig: ApplicationConfig): DatabaseConfiguration {
      val inputStream = getResourceStream(applicationConfig)
      val properties = Properties().apply { load(inputStream) }
      return DatabaseConfiguration(
        url = properties.getSafeProperty("url"),
        driver = properties.getSafeProperty("driver"),
        user = properties.getSafeProperty("user"),
        password = properties.getSafeProperty("password")
      )
    }
  }

  fun toDatabase(): Database = Database.connect(
    url = url,
    driver = driver,
    user = user,
    password = password
  ).apply {
    if (!transaction(this) {
        try {
          !connection.isClosed
        } catch (e: Exception) {
          throw Exception("Test connection failed", e)
        }
      })
      throw Exception("Test connection failed: closed")
  }
}