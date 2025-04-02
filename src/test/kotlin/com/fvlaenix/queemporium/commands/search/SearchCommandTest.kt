package com.fvlaenix.queemporium.commands.search

import com.fvlaenix.queemporium.commands.SearchCommand
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Tests for the SearchCommand functionality
 */
class SearchCommandTest : BaseSearchCommandTest() {

  override fun getCommandsForTest(): Array<KClass<*>> {
    return arrayOf(SearchCommand::class)
  }

  @Test
  fun `test search command with single source`() {
    // Arrange
    val searchUrl = "https://example.com/test_image.jpg"
    val searchResults = listOf("Title: Test Image\nSource: <https://source-site.com/image>\nSimilarity: 95")
    mockSearchService.setResponseForUrl(searchUrl, searchResults)

    // Act
    sendSearchMessage()

    // Assert
    answerService.verify {
      messageCount(1)
      lastMessageContains("Title: Test Image")
      lastMessageContains("https://source-site.com/image")
      lastMessageContains("Similarity: 95")
    }
  }

  @Test
  fun `test search command with multiple sources`() {
    // Arrange
    val searchUrl = "https://example.com/test_image.jpg"
    val searchResults = listOf(
      "Title: Test Image 1\nSource: <https://source1.com/image>\nSimilarity: 95",
      "Title: Test Image 2\nSource: <https://source2.com/image>\nSimilarity: 85"
    )
    mockSearchService.setResponseForUrl(searchUrl, searchResults)

    // Act
    sendSearchMessage()

    // Assert - Depending on implementation, may send multiple messages or combine them
    answerService.verify {
      // Should have at least one message
      messageCount(searchResults.size)

      // First source in first message
      messageAt(0, "Title: Test Image 1")
      messageAt(0, "https://source1.com/image")

      // Second source in second message
      messageAt(1, "Title: Test Image 2")
      messageAt(1, "https://source2.com/image")
    }
  }

  @Test
  fun `test search command with no results`() {
    // Arrange
    val searchUrl = "https://example.com/test_image.jpg"
    val searchResults = emptyList<String>() // No results found
    mockSearchService.setResponseForUrl(searchUrl, searchResults)

    // Act
    sendSearchMessage()

    // Assert
    answerService.verify {
      messageCount(1)
      lastMessageContains("No sources found")
    }
  }

  @Test
  fun `test search command with alternative command syntax`() {
    // Arrange
    val searchUrl = "https://example.com/test_image.jpg"
    val searchResults = listOf("Title: Test Image\nSource: <https://source-site.com/image>\nSimilarity: 95")
    mockSearchService.setResponseForUrl(searchUrl, searchResults)

    // Act - Using the alternative syntax
    sendSearchMessage(commandText = "/s s")

    // Assert
    answerService.verify {
      messageCount(1)
      lastMessageContains("Title: Test Image")
    }
  }

  @Test
  fun `test search command with multiple attachments`() {
    // Arrange
    val searchUrl1 = "https://example.com/test_image1.jpg"
    val searchUrl2 = "https://example.com/test_image2.jpg"

    val results1 = listOf("Title: Test Image 1\nSource: <https://source1.com/image>\nSimilarity: 95")
    val results2 = listOf("Title: Test Image 2\nSource: <https://source2.com/image>\nSimilarity: 90")

    mockSearchService.setResponseForUrl(searchUrl1, results1)
    mockSearchService.setResponseForUrl(searchUrl2, results2)

    // Act
    sendSearchMessageWithMultipleImages(
      fileConfigs = listOf(
        ImageConfig("test_image1.jpg"),
        ImageConfig("test_image2.jpg")
      )
    )

    // Assert - Should have responses for both attachments
    answerService.verify {
      // Should have at least two messages (one per attachment)
      messageCount(2)

      // Check content of messages
      messagesContain("Title: Test Image 1")
      messagesContain("Title: Test Image 2")
    }
  }
}