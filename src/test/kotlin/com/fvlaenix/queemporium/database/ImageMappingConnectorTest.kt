package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ImageMappingConnectorTest {
  private lateinit var database: Database
  private lateinit var connector: ImageMappingConnector

  @BeforeEach
  fun setup() {
    val dbFile = File.createTempFile("test-image-mapping", ".db")
    dbFile.deleteOnExit()

    database = Database.connect(
      url = "jdbc:h2:${dbFile.absolutePath}",
      driver = "org.h2.Driver",
      user = "",
      password = ""
    )
    connector = ImageMappingConnector(database)
  }

  @AfterEach
  fun cleanup() {
  }

  @Test
  fun `test get existing mapping`() {
    transaction(database) {
      ImageMappingTable.insert {
        it[key] = "test-key"
        it[s3Path] = "path/to/file.png"
      }
    }

    val result = connector.get("test-key")
    assertEquals("path/to/file.png", result)
  }

  @Test
  fun `test get non-existing mapping`() {
    val result = connector.get("non-existing-key")
    assertNull(result)
  }

  @Test
  fun `test get with special characters in key`() {
    transaction(database) {
      ImageMappingTable.insert {
        it[key] = "special-key_123"
        it[s3Path] = "images/special/file-name_123.jpg"
      }
    }

    val result = connector.get("special-key_123")
    assertEquals("images/special/file-name_123.jpg", result)
  }

  @Test
  fun `test multiple mappings`() {
    transaction(database) {
      ImageMappingTable.insert {
        it[key] = "key1"
        it[s3Path] = "path1.png"
      }
      ImageMappingTable.insert {
        it[key] = "key2"
        it[s3Path] = "path2.jpg"
      }
      ImageMappingTable.insert {
        it[key] = "key3"
        it[s3Path] = "path3.gif"
      }
    }

    assertEquals("path1.png", connector.get("key1"))
    assertEquals("path2.jpg", connector.get("key2"))
    assertEquals("path3.gif", connector.get("key3"))
    assertNull(connector.get("key4"))
  }
}
