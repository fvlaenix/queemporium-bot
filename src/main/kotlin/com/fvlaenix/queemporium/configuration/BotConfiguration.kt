package com.fvlaenix.queemporium.configuration

import com.fvlaenix.queemporium.utils.XmlUtils.XML
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

@Serializable
data class BotConfiguration(
  @XmlSerialName("token")
  val token: Token,
  @XmlSerialName("feature")
  val features: List<Feature> = emptyList(),
) {
  companion object {
    private val LOG = Logger.getLogger(BotConfiguration::class.java.name)

    private fun getResourceStream(applicationConfig: ApplicationConfig): InputStream {
      val applicationConfigPropertyPath = applicationConfig.botPropertiesPath
      if (applicationConfigPropertyPath != null) {
        LOG.log(Level.INFO, "Using bot configuration path from application config: $applicationConfigPropertyPath")
        try {
          return Path(applicationConfigPropertyPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open bot config file", e)
        }
      }

      val defaultStream = BotConfiguration::class.java.getResourceAsStream("/bot-properties.xml")
      if (defaultStream != null) {
        LOG.log(Level.INFO, "Using default bot config file")
        return defaultStream
      }

      throw IllegalStateException("No bot config file found")
    }

    fun load(applicationConfig: ApplicationConfig): BotConfiguration {
      LOG.log(Level.INFO, "Initializing bot configuration")
      val inputStream = getResourceStream(applicationConfig)
      return try {
        XML.decodeFromString(inputStream.reader().readText())
      } catch (e: Exception) {
        throw Exception("Failed to parse bot configuration XML", e)
      }
    }
  }

  @Serializable
  data class Feature(
    val className: String,
    @XmlSerialName("enable")
    val enable: Boolean = true,
    @XmlSerialName("parameter")
    val parameter: List<Parameter> = emptyList()
  ) {
    @Serializable
    data class Parameter(
      val name: String,
      val value: String
    )
  }

  @Serializable
  data class Token(
    val raw: String
  )
}