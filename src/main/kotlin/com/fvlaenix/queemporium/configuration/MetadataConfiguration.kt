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
data class MetadataConfiguration(
  @XmlSerialName("command")
  val commands: List<Command>
) {
  companion object {
    private val LOG = Logger.getLogger(MetadataConfiguration::class.java.name)

    private fun getResourceStream(applicationConfig: ApplicationConfig): InputStream {
      val applicationConfigPropertyPath = applicationConfig.metadataPropertiesPath
      if (applicationConfigPropertyPath != null) {
        LOG.log(Level.INFO, "Using metadata path from application config: $applicationConfigPropertyPath")
        try {
          return Path(applicationConfigPropertyPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open metadata config file", e)
        }
      }

      val defaultStream = MetadataConfiguration::class.java.getResourceAsStream("/metadata.xml")
      if (defaultStream != null) {
        LOG.log(Level.INFO, "Using default metadata config file")
        return defaultStream
      }

      throw IllegalStateException("No metadata config file found")
    }

    fun load(applicationConfig: ApplicationConfig): MetadataConfiguration {
      LOG.log(Level.INFO, "Initializing metadata configuration")
      val inputStream = getResourceStream(applicationConfig)
      return try {
        XML.decodeFromString(inputStream.reader().readText())
      } catch (e: Exception) {
        throw Exception("Failed to parse metadata configuration XML", e)
      }
    }
  }

  @Serializable
  data class Command(val className: String)
}