package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.configuration.S3Configuration
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class S3FileServiceTest {

  private fun createMockConfig(): S3Configuration {
    return S3Configuration(
      accessKey = "test-access-key",
      secretKey = "test-secret-key",
      region = "eu-north-1",
      bucketName = "test-bucket"
    )
  }

  @Test
  fun `test filename extraction from path`() = runBlocking {
    val config = createMockConfig()
    val service = S3FileServiceImpl(config)

    val testMethod = S3FileServiceImpl::class.java.getDeclaredMethod("extractFilename", String::class.java)
    testMethod.isAccessible = true

    assertEquals("file.png", testMethod.invoke(service, "path/to/file.png"))
    assertEquals("image.jpg", testMethod.invoke(service, "images/subfolder/image.jpg"))
    assertEquals("document.pdf", testMethod.invoke(service, "document.pdf"))
    assertEquals("file", testMethod.invoke(service, "path/to/"))
    assertEquals("file", testMethod.invoke(service, ""))
  }

  @Test
  fun `test max file size constant`() {
    val maxFileSize = S3FileServiceImpl::class.java.getDeclaredField("MAX_FILE_SIZE")
    maxFileSize.isAccessible = true
    val companion = S3FileServiceImpl::class.java.getDeclaredField("Companion")
    companion.isAccessible = true
    val companionInstance = companion.get(null)
    val maxFileSizeValue = maxFileSize.get(companionInstance) as Long

    assertEquals(10 * 1024 * 1024L, maxFileSizeValue)
  }
}
