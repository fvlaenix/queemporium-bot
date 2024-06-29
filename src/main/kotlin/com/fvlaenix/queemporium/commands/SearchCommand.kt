package com.fvlaenix.queemporium.commands

import dev.inmo.saucenaoapi.SauceNaoAPI
import dev.inmo.saucenaoapi.models.SauceNaoAnswer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.InputStream
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.inputStream

private val PROPERTIES_PATH_STRING: String? = System.getenv("SEARCH_PROPERTIES_PATH")

val SEARCH_PROPERTIES_INPUT_STREAM: InputStream =
  PROPERTIES_PATH_STRING?.let { try { Path(it).inputStream() } catch (e: Exception) { throw Exception("Can't open file", e) } } ?:
  SearchCommand::class.java.getResourceAsStream("/search.properties") ?:
  throw IllegalStateException("Cannot find search properties in standard files")

data class SearchConfiguration(
  val apiKey: String
) {
  companion object {
    fun Properties.getSafeProperty(name: String): String = getProperty(name) ?: throw IllegalArgumentException("Property $name not found")
  }

  constructor(properties: Properties) : this(
    apiKey = properties.getSafeProperty("API_KEY")
  )

  constructor(inputStream: InputStream) : this(Properties().apply { load(inputStream) })
}

class SearchCommand : CoroutineListenerAdapter() {
  private val configuration: SearchConfiguration = SearchConfiguration(SEARCH_PROPERTIES_INPUT_STREAM)
  
  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw.startsWith("/shogun-sama test-search")

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    val message = event.message
    val sauceNaoAPI = SauceNaoAPI(configuration.apiKey)
    val attachmentsUrl = message.attachments.filter { it.isImage }.map { it.url }
    try {
      attachmentsUrl.forEach { url ->
        val parts = getMessageResult(sauceNaoAPI.request(url))
        parts.forEach { part ->
          message.reply(part).queue()
        }
      }
    } finally {
      sauceNaoAPI.close()
    }
  }
  
  companion object {
    fun getMessageResult(answer: SauceNaoAnswer): List<String> {
      val results = answer.results.filter { it.header.similarity >= 60 }
      if (results.isEmpty()) return emptyList()
      val texts = results.map { result ->
        val data = result.data
        val titles: List<String>? = listOfNotNull(data.title, data.titleJp, data.titleEng, *data.titleAlt.toTypedArray()).ifEmpty { null }
        val urls = data.extUrls
        var text = if (titles == null) "" else "Title: ${titles.joinToString(", ")}\n"
        text += "Source: ${urls.joinToString(", ") { "<$it>" }}\n"
        text
      }
      val returned = mutableListOf<String>()
      var accumulator = texts[0]
      for (i in 1 until texts.size) {
        if ((accumulator + "\n${texts[i]}").length > 1500) {
          returned += accumulator
          accumulator = ""
        }
        accumulator += "\n${texts[i]}"
      }
      if (accumulator.isNotEmpty()) returned += accumulator
      return returned.map { it.trim() }
    }
  }
}