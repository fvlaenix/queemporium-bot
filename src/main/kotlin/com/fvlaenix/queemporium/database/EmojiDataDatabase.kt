package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class EmojiData(
  val messageId: MessageId,
  val emojiId: String,
  val authorId: String
)

object EmojiDataTable : Table() {
  val messageId = varchar("messageId", 400)
  val emojiId = varchar("emojiId", 100)
  val authorId = varchar("authorId", 100)
  
  init {
    index(false, authorId)
    index(true, messageId, emojiId, authorId)
  }
}

class EmojiDataConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(EmojiDataTable)
    }
  }
  
  fun insert(emojiData: EmojiData) = transaction(database) {
    if (EmojiDataTable.select {
        (EmojiDataTable.emojiId eq emojiData.emojiId) and 
        (EmojiDataTable.messageId eq Json.encodeToString(emojiData.messageId)) and 
        (EmojiDataTable.authorId eq emojiData.authorId)
    }.count() > 0) return@transaction
    EmojiDataTable.insert {
      it[messageId] = Json.encodeToString(emojiData.messageId)
      it[emojiId] = emojiData.emojiId
      it[authorId] = emojiData.authorId
    }
  }
  
  fun delete(messageId: MessageId) = transaction(database) {
    EmojiDataTable.deleteWhere { EmojiDataTable.messageId eq Json.encodeToString(messageId) }
  }
}