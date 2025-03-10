package com.fvlaenix.queemporium.grpc

import com.fvlaenix.alive.protobuf.isAliveRequest
import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.duplicate.protobuf.getCompressionSizeRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple test to verify the correctness of gRPC test infrastructure.
 */
class SimpleGrpcTest : BaseGrpcTest() {

  /**
   * Test verifies that server starts and responds to isAlive requests.
   */
  @Test
  fun `test server responds to isAlive request`(): Unit = runBlocking {
    // Verify that server is working
    val isAliveResponse = duplicateImagesClient.isAlive(isAliveRequest {})

    // Verify that request was registered in test service
    assertEquals(1, duplicateService.requests.size)
    assertTrue(duplicateService.requests[0] is com.fvlaenix.alive.protobuf.IsAliveRequest)

    // Verify response
    assertNotNull(isAliveResponse)
  }

  /**
   * Test verifies that server returns predefined response.
   */
  @Test
  fun `test server returns predefined compression size`() = runBlocking {
    // Configure test service to return specific response
    val expectedResponse = GetCompressionSizeResponse.newBuilder()
      .setX(1200)
      .build()

    duplicateService.compressionSizeResponse = expectedResponse

    // Send request
    val response = duplicateImagesClient.getImageCompressionSize(getCompressionSizeRequest {})

    // Verify that request was registered
    assertEquals(1, duplicateService.requests.size)

    // Verify that response matches expected
    assertEquals(1200, response.x)
  }

  /**
   * Test verifies server unavailability simulation.
   */
  @Test
  fun `test server simulates unavailable status`() = runBlocking {
    // Configure service to simulate unavailability
    simulateServerUnavailable()

    // Verify that request throws an exception
    val exception = assertThrows<io.grpc.StatusException> {
      duplicateImagesClient.isAlive(isAliveRequest {})
    }

    // Verify error status
    assertEquals(io.grpc.Status.Code.UNAVAILABLE, exception.status.code)
  }

  /**
   * Test verifies recovery after failure simulation.
   */
  @Test
  fun `test server recovers after failure simulation`(): Unit = runBlocking {
    // First simulate unavailability
    simulateServerUnavailable()

    // Verify that request throws an exception
    assertThrows<io.grpc.StatusException> {
      duplicateImagesClient.isAlive(isAliveRequest {})
    }

    // Restore normal behavior
    restoreServerNormalBehavior()

    // Verify that request now works
    val response = duplicateImagesClient.isAlive(isAliveRequest {})
    assertNotNull(response)
  }
}
