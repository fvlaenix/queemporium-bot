package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageEmojiData(
  val messageId: MessageId,
  val count: Int
)

object MessageEmojiDataTable : Table() {
  val messageId = varchar("messageId", 400).primaryKey()
  val count = integer("count")
}

class MessageEmojiDataConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(MessageEmojiDataTable)
    }
  }
  
  fun insert(message: MessageEmojiData) = transaction(database) {
    if (MessageEmojiDataTable.select { MessageEmojiDataTable.messageId eq Json.encodeToString(message.messageId) }.count() > 0) {
      MessageEmojiDataTable.update({ MessageEmojiDataTable.messageId eq Json.encodeToString(message.messageId) }) {
        it[messageId] = Json.encodeToString(message.messageId)
        it[count] = message.count
      }
    } else {
      MessageEmojiDataTable.insert {
        it[messageId] = Json.encodeToString(message.messageId)
        it[count] = message.count
      }
    }
  }
  
  fun get(messageId: MessageId) = transaction(database) {
    MessageEmojiDataTable.select { MessageEmojiDataTable.messageId eq Json.encodeToString(messageId) }
      .singleOrNull()?.let { get(it) }
  }
  
  fun delete(messageId: MessageId) = transaction(database) {
    MessageEmojiDataTable.deleteWhere { MessageEmojiDataTable.messageId eq Json.encodeToString(messageId) }
  }
  
  companion object {
    fun get(resultRow: ResultRow) = MessageEmojiData(
      Json.decodeFromString(resultRow[MessageEmojiDataTable.messageId]),
      resultRow[MessageEmojiDataTable.count]
    )
  }
}