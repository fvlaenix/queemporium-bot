package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.commands.SearchConfiguration
import dev.inmo.saucenaoapi.SauceNaoAPI
import dev.inmo.saucenaoapi.models.SauceNaoAnswer

class SearchServiceImpl(
  private val config: SearchConfiguration
) : SearchService {
  override suspend fun search(imageUrl: String): List<String> {
    val sauceNaoAPI = SauceNaoAPI(config.apiKey)
    return try {
      getMessageResult(sauceNaoAPI.request(imageUrl))
    } finally {
      sauceNaoAPI.close()
    }
  }

  private fun getMessageResult(answer: SauceNaoAnswer): List<String> {
    val results = answer.results.filter { it.header.similarity >= 60 }
    if (results.isEmpty()) return emptyList()
    val texts = results.map { result ->
      val data = result.data
      val titles: List<String>? =
        listOfNotNull(data.title, data.titleJp, data.titleEng, *data.titleAlt.toTypedArray()).ifEmpty { null }
      val urls = data.extUrls
      var text = if (titles == null) "" else "Title: ${titles.joinToString(", ")}\n"
      text += "Source: ${urls.joinToString(", ") { "<$it>" }}\nSimilarity: ${result.header.similarity}\n"
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