package com.fvlaenix.queemporium.database

import com.fvlaenix.queemporium.author.AuthorMapper
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class CorrectAuthorMappingData(
  val from: String,
  val to: String
)

object CorrectAuthorMappingTable : Table() {
  val from = varchar("from", 255)
  val to = varchar("to", 255)

  init {
    index(true, from, to)
  }
}

class CorrectAuthorMappingConnector(val database: Database) : AuthorMapper() {
  init {
    transaction(database) {
      SchemaUtils.create(CorrectAuthorMappingTable)
    }
  }

  override fun authorMapping(): Map<List<String>, List<String>> =
    transaction(database) {
      CorrectAuthorMappingTable
        .selectAll()
        .associate {
          it[CorrectAuthorMappingTable.from].split("/").map { it.trim() } to
              it[CorrectAuthorMappingTable.to].split("/").map { it.trim() }
        }
    }
}