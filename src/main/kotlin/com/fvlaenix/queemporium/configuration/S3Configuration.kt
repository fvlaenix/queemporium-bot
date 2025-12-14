package com.fvlaenix.queemporium.configuration

import com.fvlaenix.queemporium.utils.Logging
import java.io.InputStream
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.inputStream

data class S3Configuration(
  val accessKey: String,
  val secretKey: String,
  val region: String,
  val bucketName: String
) {
  companion object {
    private val LOG = Logging.getLogger(S3Configuration::class.java)

    private fun Properties.getSafeProperty(name: String): String =
      getProperty(name) ?: throw IllegalArgumentException("Property $name not found")

    private fun getResourceStream(applicationConfig: ApplicationConfig): InputStream {
      val applicationConfigPropertyPath = applicationConfig.s3PropertiesPath
      if (applicationConfigPropertyPath != null) {
        LOG.info("Using S3 path from application config: $applicationConfigPropertyPath")
        try {
          return Path(applicationConfigPropertyPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open S3 config file", e)
        }
      }

      val defaultStream = S3Configuration::class.java.getResourceAsStream("/s3.properties")
      if (defaultStream != null) {
        LOG.info("Using default S3 config file")
        return defaultStream
      }

      throw IllegalStateException("No S3 config file found")
    }

    fun load(applicationConfig: ApplicationConfig): S3Configuration {
      val inputStream = getResourceStream(applicationConfig)
      val properties = Properties().apply { load(inputStream) }
      return S3Configuration(
        accessKey = properties.getSafeProperty("accessKey"),
        secretKey = properties.getSafeProperty("secretKey"),
        region = properties.getSafeProperty("region"),
        bucketName = properties.getSafeProperty("bucketName")
      )
    }
  }
}
