package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageEmojiData(
  val messageId: String,
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
    if (MessageEmojiDataTable.select { MessageEmojiDataTable.messageId eq message.messageId }.count() > 0) {
      MessageEmojiDataTable.update({ MessageEmojiDataTable.messageId eq message.messageId }) {
        it[messageId] = message.messageId
        it[count] = message.count
      }
    } else {
      MessageEmojiDataTable.insert {
        it[messageId] = message.messageId
        it[count] = message.count
      }
    }
  }
  
  fun get(messageId: String) = transaction(database) {
    MessageEmojiDataTable.select { MessageEmojiDataTable.messageId eq messageId }
      .singleOrNull()?.let { get(it) }
  }
  
  fun delete(messageId: String) = transaction(database) {
    MessageEmojiDataTable.deleteWhere { MessageEmojiDataTable.messageId eq messageId }
  }
  
  companion object {
    fun get(resultRow: ResultRow) = MessageEmojiData(
      resultRow[MessageEmojiDataTable.messageId],
      resultRow[MessageEmojiDataTable.count]
    )
  }
}