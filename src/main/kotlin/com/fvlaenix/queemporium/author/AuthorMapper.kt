package com.fvlaenix.queemporium.author

import com.fvlaenix.queemporium.database.CorrectAuthorMappingData

abstract class AuthorMapper {
  abstract fun authorMapping(): Map<List<String>, List<String>>

  fun findMapping(text: String): CorrectAuthorMappingData? {
    val authorMap = authorMapping()
    val words = text.split("\\s+".toRegex())
    fun List<String>.containsIgnoreCase(name: String): Boolean = words.any { it.equals(name, true) }

    return authorMap.map { (incorrectList, correctList) ->
      val incorrectUsages = incorrectList.filter { incorrectName -> words.containsIgnoreCase(incorrectName) }
      val isCorrectPresent = correctList.any { correctName -> words.containsIgnoreCase(correctName) }
      if (incorrectUsages.isNotEmpty() && !isCorrectPresent) {
        CorrectAuthorMappingData(incorrectUsages.joinToString(" / "), correctList.joinToString(" / "))
      } else {
        null
      }
    }.firstNotNullOfOrNull { it }
  }
}