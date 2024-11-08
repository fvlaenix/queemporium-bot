package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class EmojiData(
  val messageId: String,
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

  private fun insertUnderTransaction(emojiData: EmojiData) {
    if (EmojiDataTable.select {
        (EmojiDataTable.emojiId eq emojiData.emojiId) and
            (EmojiDataTable.messageId eq emojiData.messageId) and
            (EmojiDataTable.authorId eq emojiData.authorId)
      }.count() > 0) return
    EmojiDataTable.insert {
      it[messageId] = emojiData.messageId
      it[emojiId] = emojiData.emojiId
      it[authorId] = emojiData.authorId
    }
  }

  fun insert(emojiData: EmojiData) = transaction(database) {
    insertUnderTransaction(emojiData)
  }

  fun insert(emojiDatas: Collection<EmojiData>) = transaction(database) {
    emojiDatas.forEach { emojiData ->
      insertUnderTransaction(emojiData)
    }
  }
  
  fun delete(messageId: String) = transaction(database) {
    EmojiDataTable.deleteWhere { EmojiDataTable.messageId eq messageId }
  }
}