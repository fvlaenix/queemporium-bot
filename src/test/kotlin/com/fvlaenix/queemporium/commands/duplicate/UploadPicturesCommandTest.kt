package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.features.FeatureKeys
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UploadPicturesCommandTest : BaseDuplicateCommandTest() {

  override fun getFeaturesForTest(): Array<String> {
    return arrayOf(FeatureKeys.UPLOAD_PICTURES)
  }

  override var autoStartEnvironment: Boolean = false

  @Test
  fun `test upload command adds images to database`() {
    // Create a message with an image to upload
    sendMessageWithImage(
      messageText = "Message with image to upload",
      fileName = "image_to_upload.jpg"
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Verify that the image was added (through the mock service)
    assertEquals(1, mockDuplicateService.countAddImageRequests(), "Should have at least one request to add the image")

    // Verify no duplicate notifications were sent
    answerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test upload command ignores excluded channels`() {
    // Add generalChannel to excluded list
    guildInfoConnector.addExcludingChannel(testGuild.id, generalChannel.id)

    // Create a message with an image in the excluded channel
    sendMessageWithImage(
      messageText = "Message in excluded channel",
      fileName = "image_in_excluded.jpg"
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Verify no requests were made to add the image
    assertEquals(0, mockDuplicateService.countAddImageRequests(), "Should not have any requests for excluded channel")
  }

  @Test
  fun `test upload command handles multiple images`() {
    // Create a message with multiple images
    sendMessageWithMultipleImages(
      messageText = "Message with multiple images",
      fileConfigs = listOf(
        ImageConfig("upload1.jpg"),
        ImageConfig("upload2.jpg"),
        ImageConfig("upload3.jpg")
      )
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    assertEquals(1, mockDuplicateService.countAddImageRequests(), "Should have requests to add images")
  }

  @Test
  fun `test upload command handles service errors gracefully`() {
    // Create a message with an image
    sendMessageWithImage(
      messageText = "Message that will cause error",
      fileName = "error_image.jpg"
    )

    // Configure the service to return an error
    mockDuplicateService.setResponseForFile("error_image.jpg", null)

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Verify errors are handled gracefully and don't cause crashes
    // There should be no error messages in the channel
    answerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test upload command properly processes spoilered images`() {
    // Create a message with a spoilered image
    sendMessageWithImage(
      messageText = "Message with spoilered image",
      fileName = "spoiler_image.jpg",
      isSpoiler = true
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Verify image was processed
    assertEquals(1, mockDuplicateService.countAddImageRequests(), "Should have a request to add the spoilered image")

    // Verify no duplicate notifications
    answerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test upload command handles images with different dimensions`() {
    // Create messages with images of different dimensions
    sendMessageWithImage(
      messageText = "Message with small image",
      fileName = "small_image.jpg",
      width = 50,
      height = 50
    )

    sendMessageWithImage(
      messageText = "Message with large image",
      fileName = "large_image.jpg",
      width = 1200,
      height = 800
    )

    // Start the environment
    startEnvironment()

    // Wait for completion
    env.awaitAll()

    // Verify images were processed
    assertEquals(
      2,
      mockDuplicateService.countAddImageRequests(),
      "Should have requests to add images of different sizes"
    )

    // Verify no duplicate notifications
    answerService.verify {
      messageCount(0)
    }
  }
}