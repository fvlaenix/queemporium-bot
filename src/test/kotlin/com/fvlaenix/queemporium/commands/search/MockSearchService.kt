package com.fvlaenix.queemporium.commands.search

import com.fvlaenix.queemporium.service.SearchService
import kotlinx.coroutines.delay

/**
 * Mock implementation of SearchService for testing purposes.
 * Allows predefined responses for specific image URLs and simulating errors.
 */
class MockSearchService : SearchService {
    // Map of image URLs to predefined search results
    private val responsesByUrl = mutableMapOf<String, List<String>>()
    
    // Default response used when no specific URL mapping is found
    var defaultResponse: List<String> = emptyList()
    
    // Control flags for simulation
    var shouldThrowException: Boolean = false
    var networkLatency: Long = 0 // Milliseconds to delay response
    
    // Exception to throw if shouldThrowException is true
    var exceptionToThrow: Exception = RuntimeException("Simulated search service failure")
    
    /**
     * Sets a predefined response for a specific image URL
     * 
     * @param imageUrl The image URL to match
     * @param response The search results to return
     */
    fun setResponseForUrl(imageUrl: String, response: List<String>) {
        responsesByUrl[imageUrl] = response
    }
    
    /**
     * Removes a response for a specific image URL
     * 
     * @param imageUrl The image URL to remove
     */
    fun removeResponseForUrl(imageUrl: String) {
        responsesByUrl.remove(imageUrl)
    }
    
    /**
     * Clears all predefined responses
     */
    fun clearResponses() {
        responsesByUrl.clear()
        defaultResponse = emptyList()
    }
    
    /**
     * Configure the mock to simulate a network error
     * 
     * @param shouldSimulate Whether to simulate a network error
     * @param latencyMs Optional latency to add before throwing the error (in milliseconds)
     * @param exception Optional custom exception to throw
     */
    fun simulateNetworkError(
        shouldSimulate: Boolean = true,
        latencyMs: Long = 0,
        exception: Exception = RuntimeException("Network error")
    ) {
        shouldThrowException = shouldSimulate
        networkLatency = latencyMs
        exceptionToThrow = exception
    }
    
    override suspend fun search(imageUrl: String): List<String> {
        // Simulate network latency if configured
        if (networkLatency > 0) {
            delay(networkLatency)
        }
        
        // Throw exception if configured to do so
        if (shouldThrowException) {
            throw exceptionToThrow
        }
        
        // Return the predefined response for this URL, or the default response if not found
        return responsesByUrl[imageUrl] ?: defaultResponse
    }
}