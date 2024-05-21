package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageData(
  val imageId: ImageId,
  val additionalImageInfo: AdditionalImageInfo,
  val text: String,
  val author: String,
  val epoch: Long
)

object MessageDataTable : Table() {
  val imageId = varchar("imageId", 1000).primaryKey()
  val additionalImageInfo = varchar("additionalImageInfo", 1000)
  val text = varchar("text", 10000)
  val author = varchar("author", 100)
  val epoch = long("epoch")
}

class MessageDataConnector(private val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(MessageDataTable)
    }
  }
  
  fun add(messageData: MessageData) = transaction(database) {
    MessageDataTable.insert {
      it[imageId] = Json.encodeToString(messageData.imageId)
      it[additionalImageInfo] = Json.encodeToString(messageData.additionalImageInfo)
      it[text] = messageData.text
      it[author] = messageData.author
      it[epoch] = messageData.epoch
    }
  }
  
  companion object {
    fun get(resultRow: ResultRow): MessageData = MessageData(
      Json.decodeFromString(resultRow[MessageDataTable.imageId]),
      Json.decodeFromString(resultRow[MessageDataTable.additionalImageInfo]),
      resultRow[MessageDataTable.text],
      resultRow[MessageDataTable.author],
      resultRow[MessageDataTable.epoch],
    )
  }
}