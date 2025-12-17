package com.fvlaenix.queemporium.commands.search

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.testing.log.expectLogs
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test

/**
 * Tests focused specifically on error handling and edge cases
 * in the SearchCommand implementation
 */
class SearchErrorHandlingTest : BaseSearchCommandTest() {

  override fun getFeaturesForTest(): Array<String> {
    return arrayOf(FeatureKeys.SEARCH)
  }

  override fun additionalSetUp() {
    // Configure default behavior
    mockSearchService.defaultResponse = emptyList()
  }

  @Test
  fun `test search command handles network errors gracefully`() {
    expectLogs {
      error("com.fvlaenix.queemporium.commands.SearchCommand", count = 1)
    }

    // Arrange - Configure mock to simulate a network error
    mockSearchService.simulateNetworkError(
      shouldSimulate = true,
      exception = java.io.IOException("Network timeout")
    )

    // Act
    sendSearchMessage()

    // Assert - The command should handle the error gracefully
    answerService.verify {
      messageCount(1)
      lastMessageContains("No sources found") // Or appropriate error message
    }
  }

  @Test
  fun `test search command handles high latency`() {
    // Arrange - Configure mock to have high latency but eventually succeed
    val searchResults = listOf("Title: Test Image\nSource: <https://source-site.com/image>\nSimilarity: 95")
    mockSearchService.networkLatency = 2000 // 2 seconds delay
    mockSearchService.defaultResponse = searchResults

    // Act
    sendSearchMessage()

    // Assert - Should still get results despite latency
    answerService.verify {
      messageCount(1)
      lastMessageContains("Title: Test Image")
    }
  }

  @Test
  fun `test search command handles unexpected exceptions`() {
    expectLogs {
      error("com.fvlaenix.queemporium.commands.SearchCommand", count = 1)
    }

    // Arrange - Configure mock to throw an unexpected exception
    mockSearchService.shouldThrowException = true
    mockSearchService.exceptionToThrow = NullPointerException("Unexpected error")

    // Act
    sendSearchMessage()

    // Assert
    answerService.verify {
      messageCount(1)
      // The message might differ depending on how error handling is implemented
      // but there should be a response to the user
    }
  }

  @Test
  fun `test search command with invalid attachment URL`() {
    // Arrange - Imagine a scenario where URL processing fails
    mockSearchService.setResponseForUrl("https://example.com/malformed_image.jpg", emptyList())

    // Act
    sendSearchMessage(fileName = "malformed_image.jpg")

    // Assert
    answerService.verify {
      messageCount(1)
      lastMessageContains("No sources found") // Or appropriate error message
    }
  }

  @Test
  fun `test search command with extremely large response`() {
    // Arrange - Create a very large response
    val veryLongDescription = "A".repeat(1000)
    val largeSearchResult = listOf(
      "Title: Very Long Title\nDescription: $veryLongDescription\nSource: <https://source-site.com/image>\nSimilarity: 95"
    )
    mockSearchService.defaultResponse = largeSearchResult

    // Act
    sendSearchMessage()

    // Assert - Should handle large responses appropriately
    answerService.verify {
      messageCount(1)
      lastMessageContains("Very Long Title")
    }
  }

  @Test
  fun `test search command with multiple large responses`() {
    // Arrange - Create multiple large responses
    val veryLongDescription1 = "A".repeat(500)
    val veryLongDescription2 = "B".repeat(500)
    val largeSearchResults = listOf(
      "Title: Long Title 1\nDescription: $veryLongDescription1\nSource: <https://source1.com/image>\nSimilarity: 95",
      "Title: Long Title 2\nDescription: $veryLongDescription2\nSource: <https://source2.com/image>\nSimilarity: 90"
    )
    mockSearchService.defaultResponse = largeSearchResults

    // Act
    sendSearchMessage()

    // Assert - Should handle multiple large responses properly
    answerService.verify {
      // The message count will depend on if the SearchCommand implementation
      // sends each result separately or combines them
      messageCount(2)
      messagesContain("Long Title 1")
      messagesContain("Long Title 2")
    }
  }

  @Test
  fun `test search command behavior when service returns malformed data`() {
    // Arrange - Configure mock to return malformed data
    val malformedResults = listOf("This is not in the expected format")
    mockSearchService.defaultResponse = malformedResults

    // Act
    sendSearchMessage()

    // Assert - Should handle malformed data gracefully
    answerService.verify {
      messageCount(1)
      // The implementation should either pass through the malformed data
      // or handle it appropriately
    }
  }
}