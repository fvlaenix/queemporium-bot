package com.fvlaenix.queemporium.database

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

class CorrectAuthorMappingConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(CorrectAuthorMappingTable)
    }
  }
  
  fun fromText(text: String): List<CorrectAuthorMappingData> = transaction(database) {
    val all = CorrectAuthorMappingTable.selectAll().map { CorrectAuthorMappingData(it[CorrectAuthorMappingTable.from], it[CorrectAuthorMappingTable.to]) }
    all.filter { text.contains(it.from) && !text.contains(it.to) }.map { CorrectAuthorMappingData(it.from, it.to) }
  }
}