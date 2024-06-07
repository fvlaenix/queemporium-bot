package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageData(
  val messageId: MessageId,
  val text: String,
  val url: String,
  val authorId: String,
  val epoch: Long,
)

object MessageDataTable : Table() {
  val messageId = varchar("messageId", 400).primaryKey()
  val text = varchar("text", 5000)
  val url = varchar("url", 300)
  val authorId = varchar("authorId", 100)
  val epoch = long("epoch")
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
      it[url] = messageData.url
      it[authorId] = messageData.authorId
      it[epoch] = messageData.epoch
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
      resultRow[MessageDataTable.url],
      resultRow[MessageDataTable.authorId],
      resultRow[MessageDataTable.epoch],
    )
  }
}
