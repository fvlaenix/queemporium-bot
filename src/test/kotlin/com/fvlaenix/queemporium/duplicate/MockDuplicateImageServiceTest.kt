package com.fvlaenix.queemporium.duplicate

import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.database.CompressSize
import com.fvlaenix.queemporium.mock.MockDuplicateImageService
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.service.DuplicateImageService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockDuplicateImageServiceTest {

  private lateinit var mockService: MockDuplicateImageService
  private lateinit var testMessage: Message

  @BeforeEach
  fun setup() {
    mockService = MockDuplicateImageService()

    testMessage = mockk<Message>()
    val attachment = createTestAttachment(
      fileName = "test_image.jpg",
      width = 100,
      height = 100
    )

    val offsetDateTime = mockk<OffsetDateTime>()
    every { offsetDateTime.toEpochSecond() } returns 123456789L

    every { testMessage.attachments } returns listOf(attachment)
    every { testMessage.embeds } returns emptyList()
    every { testMessage.timeCreated } returns offsetDateTime
    every { testMessage.guildId } returns "test_guild"
    every { testMessage.channelId } returns "test_channel"
    every { testMessage.id } returns "test_message"

    val messageChannel = mockk<MessageChannelUnion>()
    every { testMessage.channel } returns messageChannel

  }

  @Test
  fun `test addImageWithCheck with file-specific response`() = runBlocking {
    val duplicateData = DuplicateImageService.DuplicateImageData(
      messageId = "original123",
      numberInMessage = 0,
      additionalImageInfo = AdditionalImageInfo(
        fileName = "original_image.jpg",
        isSpoiler = false,
        originalSizeHeight = 200,
        originalSizeWidth = 300
      ),
      level = 95
    )

    mockService.setResponseForFile("test_image.jpg", listOf(duplicateData))

    val callbackResult = CompletableFuture<Pair<*, List<DuplicateImageService.DuplicateImageData>>>()

    mockService.addImageWithCheck(
      message = testMessage,
      compressSize = CompressSize(width = 100, height = null),
      withHistoryReload = false
    ) { result ->
      callbackResult.complete(result)
    }

    val result = callbackResult.get(30, TimeUnit.SECONDS)

    assertNotNull(result)
    assertEquals(1, result.second.size)
    assertEquals("original123", result.second[0].messageId)
    assertEquals(95, result.second[0].level)
  }

  @Test
  fun `test addImageWithCheck with nextResponse`() = runBlocking {
    val duplicateData = DuplicateImageService.DuplicateImageData(
      messageId = "next_response_original",
      numberInMessage = 0,
      additionalImageInfo = AdditionalImageInfo(
        fileName = "next_response_image.jpg",
        isSpoiler = true,
        originalSizeHeight = 400,
        originalSizeWidth = 600
      ),
      level = 80
    )

    mockService.nextResponse = listOf(duplicateData)

    val callbackResult = CompletableFuture<Pair<*, List<DuplicateImageService.DuplicateImageData>>>()

    mockService.addImageWithCheck(
      message = testMessage,
      compressSize = CompressSize(width = 100, height = null),
      withHistoryReload = false
    ) { result ->
      callbackResult.complete(result)
    }

    val result = callbackResult.get(30, TimeUnit.SECONDS)

    assertNotNull(result)
    assertEquals(1, result.second.size)
    assertEquals("next_response_original", result.second[0].messageId)
    assertEquals(80, result.second[0].level)
    assertTrue(result.second[0].additionalImageInfo.isSpoiler)
  }

  @Test
  fun `test checkServerAliveness`() = runBlocking {
    val readyEvent = mockk<ReadyEvent>()
    val jda = mockk<JDA>()
    every { readyEvent.jda } returns jda

    mockService.isServerAlive = true
    val compressSize = mockService.checkServerAliveness(readyEvent)
    assertNotNull(compressSize)
    assertEquals(500, compressSize.width)

    mockService.isServerAlive = false
    val nullCompressSize = mockService.checkServerAliveness(readyEvent)
    assertNull(nullCompressSize)
  }

  @Test
  fun `test deleteImage`() = runBlocking {
    val deleteData = listOf(
      DuplicateImageService.DeleteImageData("message1", 0),
      DuplicateImageService.DeleteImageData("message2", 1)
    )

    mockService.deleteImage(deleteData)
  }
}