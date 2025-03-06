package com.fvlaenix.queemporium.commands.search

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the MockSearchService implementation
 */
class MockSearchServiceTest {
    private lateinit var mockService: MockSearchService

    @BeforeEach
    fun setup() {
        mockService = MockSearchService()
    }

    @Test
    fun `test search with URL-specific response`() = runBlocking {
        // Arrange
        val imageUrl = "https://example.com/test-image.jpg"
        val expectedResponse = listOf("Source: Website A", "Source: Website B")
        mockService.setResponseForUrl(imageUrl, expectedResponse)

        // Act
        val result = mockService.search(imageUrl)

        // Assert
        assertEquals(expectedResponse, result, "The search result should match the predefined response")
    }

    @Test
    fun `test search with default response`() = runBlocking {
        // Arrange
        val imageUrl = "https://example.com/unknown-image.jpg"
        val expectedResponse = listOf("Default source: Website X")
        mockService.defaultResponse = expectedResponse

        // Act
        val result = mockService.search(imageUrl)

        // Assert
        assertEquals(expectedResponse, result, "The search result should match the default response")
    }

    @Test
    fun `test search with no matching URL and empty default response`() = runBlocking {
        // Arrange
        val imageUrl = "https://example.com/unknown-image.jpg"
        // Default response is empty by default

        // Act
        val result = mockService.search(imageUrl)

        // Assert
        assertTrue(result.isEmpty(), "The search result should be empty")
    }

    @Test
    fun `test clearResponses clears all configured responses`() = runBlocking {
        // Arrange
        val imageUrl = "https://example.com/test-image.jpg"
        mockService.setResponseForUrl(imageUrl, listOf("Source: Website A"))
        mockService.defaultResponse = listOf("Default source")

        // Act
        mockService.clearResponses()
        val result = mockService.search(imageUrl)

        // Assert
        assertTrue(result.isEmpty(), "After clearing, the search result should be empty")
    }
    
    @Test
    fun `test removal of specific URL response`() = runBlocking {
        // Arrange
        val imageUrl1 = "https://example.com/test-image1.jpg"
        val imageUrl2 = "https://example.com/test-image2.jpg"
        
        mockService.setResponseForUrl(imageUrl1, listOf("Source: Website A"))
        mockService.setResponseForUrl(imageUrl2, listOf("Source: Website B"))
        
        // Act
        mockService.removeResponseForUrl(imageUrl1)
        
        // Assert
        assertTrue(mockService.search(imageUrl1).isEmpty(), "Response for removed URL should be empty")
        assertEquals(listOf("Source: Website B"), mockService.search(imageUrl2), 
            "Response for non-removed URL should still be available")
    }
    
    @Test
    fun `test exception throwing`() = runBlocking {
        // Arrange
        val imageUrl = "https://example.com/test-image.jpg"
        val customException = IllegalArgumentException("Custom test exception")
        
        mockService.shouldThrowException = true
        mockService.exceptionToThrow = customException
        
        // Act & Assert
        val exception = assertThrows<IllegalArgumentException> {
            mockService.search(imageUrl)
        }
        
        assertEquals("Custom test exception", exception.message, 
            "Exception should have the custom message")
    }
    
    @Test
    fun `test network error simulation`() = runBlocking {
        // Arrange
        val imageUrl = "https://example.com/test-image.jpg"
        
        mockService.simulateNetworkError(
            shouldSimulate = true,
            exception = java.io.IOException("Network timeout")
        )
        
        // Act & Assert
        val exception = assertThrows<java.io.IOException> {
            mockService.search(imageUrl)
        }
        
        assertEquals("Network timeout", exception.message,
            "Exception should have the custom network error message")
    }
}