package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageData(
  val messageId: String,
  val guildId: String?,
  val channelId: String,
  val text: String,
  val url: String,
  val authorId: String,
  val epoch: Long,
)

object MessageDataTable : Table() {
  val messageId = varchar("messageId", 400).primaryKey()
  val guildId = varchar("guildId", 100).nullable()
  val channelId = varchar("channelId", 100)
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
    val message = MessageDataTable.select { MessageDataTable.messageId eq messageData.messageId }
    if (message.count() > 0) return@transaction
    MessageDataTable.insert {
      it[messageId] = messageData.messageId
      it[guildId] = messageData.guildId
      it[channelId] = messageData.channelId
      it[text] = messageData.text
      it[url] = messageData.url
      it[authorId] = messageData.authorId
      it[epoch] = messageData.epoch
    }
  }

  fun get(messageId: String): MessageData? = transaction(database) {
    MessageDataTable.select { MessageDataTable.messageId eq messageId }.map { get(it) }.singleOrNull()
  }

  fun delete(messageId: String) = transaction(database) {
    MessageDataTable.deleteWhere { MessageDataTable.messageId eq messageId }
  }

  companion object {
    fun get(resultRow: ResultRow): MessageData = MessageData(
      resultRow[MessageDataTable.messageId],
      resultRow[MessageDataTable.guildId],
      resultRow[MessageDataTable.channelId],
      resultRow[MessageDataTable.text],
      resultRow[MessageDataTable.url],
      resultRow[MessageDataTable.authorId],
      resultRow[MessageDataTable.epoch],
    )
  }
}
