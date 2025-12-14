package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object ImageMappingTable : Table() {
  val key = varchar("key", 255).primaryKey()
  val s3Path = varchar("s3Path", 1024)
}

class ImageMappingConnector(private val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(ImageMappingTable)
    }
  }

  fun get(key: String): String? = transaction(database) {
    ImageMappingTable
      .select { ImageMappingTable.key eq key }
      .map { it[ImageMappingTable.s3Path] }
      .singleOrNull()
  }
}
