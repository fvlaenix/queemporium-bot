package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.ApplicationConfig
import com.fvlaenix.queemporium.service.SearchService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.InputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.inputStream

private val LOG = Logger.getLogger(SearchCommand::class.java.simpleName)

data class SearchConfiguration(
  val apiKey: String
) {
  companion object {
    private fun getResourceStream(applicationConfig: ApplicationConfig): InputStream? {
      val applicationConfigPropertyPath = applicationConfig.searchPropertiesPath
      if (applicationConfigPropertyPath != null) {
        LOG.log(Level.INFO, "Using search path from application config: $applicationConfigPropertyPath")
        try {
          return Path(applicationConfigPropertyPath).inputStream()
        } catch (e: Exception) {
          throw Exception("Can't open search config file", e)
        }
      }
      val defaultStream = SearchCommand::class.java.getResourceAsStream("/search.properties")
      if (defaultStream != null) {
        LOG.log(Level.INFO, "Using default search config file")
        return defaultStream
      }
      LOG.log(Level.WARNING, "No search config file found")
      return null
    }

    fun load(applicationConfig: ApplicationConfig): SearchConfiguration? {
      val inputStream = getResourceStream(applicationConfig) ?: return null
      val properties = Properties().apply { load(inputStream) }
      return SearchConfiguration(
        apiKey = properties.getSafeProperty("API_KEY")
      )
    }

    fun Properties.getSafeProperty(name: String): String =
      getProperty(name) ?: throw IllegalArgumentException("Property $name not found")
  }

  constructor(properties: Properties) : this(
    apiKey = properties.getSafeProperty("API_KEY")
  )

  constructor(inputStream: InputStream) : this(Properties().apply { load(inputStream) })
}

class SearchCommand(
  val searchService: SearchService
) : CoroutineListenerAdapter() {
  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw.startsWith("/shogun-sama search") || event.message.contentRaw.startsWith("/s s")

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    val message = event.message
    val attachmentsUrl = message.attachments.filter { it.isImage }.map { it.url }
    attachmentsUrl.forEach { url ->
      val parts = searchService.search(url)
      if (parts.isEmpty()) {
        message.reply("No sources found").queue()
      }
      parts.forEach { part ->
        message.reply(part).queue()
      }
    }
  }
}