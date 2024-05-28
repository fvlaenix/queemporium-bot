package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageData(
  val messageId: MessageId,
  val text: String,
  val hasSource: Boolean,
  val url: String,
  val author: String,
  val epoch: Long,
  val countImages: Int,
  val messageProblems: MessageProblems
)

object MessageDataTable : Table() {
  val messageId = varchar("messageId", 400).primaryKey()
  val text = varchar("text", 5000)
  val hasSource = bool("hasSource")
  val url = varchar("url", 300)
  val author = varchar("author", 100)
  val epoch = long("epoch")
  val countImages = integer("countImages")
  val messageProblems = varchar("messageProblems", 1500)
}

class MessageDataConnector(private val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(MessageDataTable)
    }
  }
  
  fun add(messageData: MessageData) = transaction(database) {
    val message = MessageDataTable.select { MessageDataTable.messageId eq Json.encodeToString(messageData.messageId) }
    if (message.count() > 0) return@transaction 
    MessageDataTable.insert {
      it[messageId] = Json.encodeToString(messageData.messageId)
      it[text] = messageData.text
      it[hasSource] = messageData.hasSource
      it[url] = messageData.url
      it[author] = messageData.author
      it[epoch] = messageData.epoch
      it[countImages] = messageData.countImages
      it[messageProblems] = Json.encodeToString(messageData.messageProblems)
    }
  }
  
  fun get(messageId: MessageId): MessageData? = transaction(database) {
    MessageDataTable.select { MessageDataTable.messageId eq Json.encodeToString(messageId) }.map { get(it) }.singleOrNull()
  }
  
  fun delete(messageId: MessageId) = transaction(database) { 
    MessageDataTable.deleteWhere { MessageDataTable.messageId eq Json.encodeToString(messageId) }
  }
  
  companion object {
    fun get(resultRow: ResultRow): MessageData = MessageData(
      Json.decodeFromString(resultRow[MessageDataTable.messageId]),
      resultRow[MessageDataTable.text],
      resultRow[MessageDataTable.hasSource],
      resultRow[MessageDataTable.url],
      resultRow[MessageDataTable.author],
      resultRow[MessageDataTable.epoch],
      resultRow[MessageDataTable.countImages],
      Json.decodeFromString(resultRow[MessageDataTable.messageProblems])
    )
  }
}
