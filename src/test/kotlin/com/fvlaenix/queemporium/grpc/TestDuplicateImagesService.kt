package com.fvlaenix.queemporium.grpc

import com.fvlaenix.alive.protobuf.IsAliveRequest
import com.fvlaenix.alive.protobuf.IsAliveResponse
import com.fvlaenix.duplicate.protobuf.*
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.delay
import java.util.Collections
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Test mock of DuplicateImagesService for integration testing.
 * Allows configuring responses and failure scenarios for various service methods.
 */
class TestDuplicateImagesService : DuplicateImagesServiceGrpcKt.DuplicateImagesServiceCoroutineImplBase() {
    private val logger = Logger.getLogger(TestDuplicateImagesService::class.java.name)

    // Configurable responses
    var isAliveResponse: IsAliveResponse? = null
    var addImageResponse: AddImageResponse? = null
    var checkImageResponse: CheckImageResponse? = null
    var existsImageResponse: ExistsImageResponse? = null
    var deleteImageResponse: DeleteImageResponse? = null
    var compressionSizeResponse: GetCompressionSizeResponse? = null

    // Request tracking
    val requests = Collections.synchronizedList(mutableListOf<Any>())

    // Failure settings
    var simulateUnavailable = false
    var simulateTimeout = false
    var timeoutDuration = 10000L // ms

    /**
     * Resets all mock settings to initial state.
     */
    fun reset() {
        isAliveResponse = null
        addImageResponse = null
        checkImageResponse = null
        existsImageResponse = null
        deleteImageResponse = null
        compressionSizeResponse = null
        requests.clear()
        simulateUnavailable = false
        simulateTimeout = false
        timeoutDuration = 10000L
    }

    /**
     * Service health check.
     */
    override suspend fun isAlive(request: IsAliveRequest): IsAliveResponse {
        logger.log(Level.INFO, "Received isAlive request")
        requests.add(request)

        handleFailureScenarios()

        return isAliveResponse ?: IsAliveResponse.getDefaultInstance()
    }

    /**
     * Add image with duplicate check.
     */
    override suspend fun addImageWithCheck(request: AddImageRequest): AddImageResponse {
        logger.log(Level.INFO, "Received addImageWithCheck request for messageId: ${request.messageId}")
        requests.add(request)

        handleFailureScenarios()

        return addImageResponse ?: createDefaultAddImageResponse()
    }

    /**
     * Check if image exists.
     */
    override suspend fun existsImage(request: ExistsImageRequest): ExistsImageResponse {
        logger.log(Level.INFO, "Received existsImage request for messageId: ${request.messageId}")
        requests.add(request)

        handleFailureScenarios()

        return existsImageResponse ?: createDefaultExistsImageResponse()
    }

    /**
     * Check image for duplicates.
     */
    override suspend fun checkImage(request: CheckImageRequest): CheckImageResponse {
        logger.log(Level.INFO, "Received checkImage request for group: ${request.group}")
        requests.add(request)

        handleFailureScenarios()

        return checkImageResponse ?: createDefaultCheckImageResponse()
    }

    /**
     * Delete image.
     */
    override suspend fun deleteImage(request: DeleteImageRequest): DeleteImageResponse {
        logger.log(Level.INFO, "Received deleteImage request for messageId: ${request.messageId}")
        requests.add(request)

        handleFailureScenarios()

        return deleteImageResponse ?: createDefaultDeleteImageResponse()
    }

    /**
     * Get image compression size.
     */
    override suspend fun getImageCompressionSize(request: GetCompressionSizeRequest): GetCompressionSizeResponse {
        logger.log(Level.INFO, "Received getImageCompressionSize request")
        requests.add(request)

        handleFailureScenarios()

        return compressionSizeResponse ?: createDefaultCompressionSizeResponse()
    }

    /**
     * Handles failure scenarios if they are configured.
     */
    private suspend fun handleFailureScenarios() {
        if (simulateUnavailable) {
            logger.log(Level.INFO, "Simulating server unavailability")
            throw StatusException(Status.UNAVAILABLE.withDescription("Service Unavailable"))
        }

        if (simulateTimeout) {
            logger.log(Level.INFO, "Simulating timeout (${timeoutDuration}ms)")
            delay(timeoutDuration)
            throw StatusException(Status.DEADLINE_EXCEEDED.withDescription("Request timed out"))
        }
    }

    // Default methods for creating responses

    private fun createDefaultAddImageResponse(): AddImageResponse {
        return AddImageResponse.newBuilder()
            .setResponseOk(
                AddImageResponseOk.newBuilder()
                    .setIsAdded(true)
                    .setImageInfo(CheckImageResponseImagesInfo.getDefaultInstance())
                    .build()
            )
            .build()
    }

    private fun createDefaultExistsImageResponse(): ExistsImageResponse {
        return ExistsImageResponse.newBuilder()
            .setIsExists(false)
            .build()
    }

    private fun createDefaultCheckImageResponse(): CheckImageResponse {
        return CheckImageResponse.newBuilder()
            .setImageInfo(CheckImageResponseImagesInfo.getDefaultInstance())
            .build()
    }

    private fun createDefaultDeleteImageResponse(): DeleteImageResponse {
        return DeleteImageResponse.newBuilder()
            .setIsDeleted(true)
            .build()
    }

    private fun createDefaultCompressionSizeResponse(): GetCompressionSizeResponse {
        return GetCompressionSizeResponse.newBuilder()
            .setX(800)
            .build()
    }
}
