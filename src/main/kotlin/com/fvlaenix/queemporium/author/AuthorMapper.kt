package com.fvlaenix.queemporium.author

import com.fvlaenix.queemporium.database.CorrectAuthorMappingData

abstract class AuthorMapper {
  abstract fun authorMapping(): Map<List<String>, List<String>>

  fun findMapping(text: String): CorrectAuthorMappingData? {
    val authorMap = authorMapping()

    // Tokenize: replace punctuation with spaces, then split into words
    // Note that we keep hyphens since they might be part of author names
    val tokenizedText = text.replace(Regex("[,.!?;:|]"), " ")
    val words = tokenizedText.split(Regex("\\s+")).filter { it.isNotEmpty() }

    return authorMap.map { (incorrectList, correctList) ->
      // Check for incorrect author names (exact word match, case-insensitive)
      val incorrectUsages = incorrectList.filter { incorrectName ->
        words.any { it.equals(incorrectName, ignoreCase = true) }
      }

      // Check if any correct name is present (exact word match, case-insensitive)
      val isCorrectPresent = correctList.any { correctName ->
        words.any { it.equals(correctName, ignoreCase = true) }
      }

      if (incorrectUsages.isNotEmpty() && !isCorrectPresent) {
        CorrectAuthorMappingData(incorrectUsages.joinToString(" / "), correctList.joinToString(" / "))
      } else {
        null
      }
    }.firstNotNullOfOrNull { it }
  }
}