package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageDuplicateData(
  val messageId: String,
  val hasSource: Boolean,
  val countImages: Int,
  val messageProblems: List<MessageProblem>
) {
  data class FullInfo(
    val messageId: String,
    val text: String,
    val hasSource: Boolean,
    val url: String,
    val author: String,
    val epoch: Long,
    val countImages: Int,
    val messageProblems: List<MessageProblem>
  )

  fun withMessageData(messageData: MessageData): FullInfo {
    assert(messageData.messageId == messageId)
    return FullInfo(
      messageId,
      messageData.text,
      hasSource,
      messageData.url,
      messageData.authorId,
      messageData.epoch,
      countImages,
      messageProblems
    )
  }
}

object MessageDuplicateDataTable : Table() {
  val messageId = varchar("messageId", 400).primaryKey()
  val hasSource = bool("hasSource")
  val countImages = integer("countImages")
  val messageProblems = varchar("messageProblems", 5000)
}

class MessageDuplicateDataConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(MessageDuplicateDataTable)
    }
  }

  fun add(messageDuplicateData: MessageDuplicateData) = transaction(database) {
    val existing =
      MessageDuplicateDataTable.select { MessageDuplicateDataTable.messageId eq messageDuplicateData.messageId }
    if (existing.count() > 0) return@transaction
    MessageDuplicateDataTable.insert {
      it[messageId] = messageDuplicateData.messageId
      it[hasSource] = messageDuplicateData.hasSource
      it[countImages] = messageDuplicateData.countImages
      it[messageProblems] = Json.encodeToString(messageDuplicateData.messageProblems)
    }
  }

  fun exists(messageId: String): Boolean = transaction(database) {
    MessageDuplicateDataTable.select { MessageDuplicateDataTable.messageId eq messageId }.count() > 0
  }

  fun get(messageId: String) = transaction(database) {
    val result =
      MessageDuplicateDataTable.select { MessageDuplicateDataTable.messageId eq messageId }
        .singleOrNull()
    result?.let { get(it) }
  }

  fun delete(messageId: String) = transaction(database) {
    MessageDuplicateDataTable.deleteWhere { MessageDuplicateDataTable.messageId eq messageId }
  }

  companion object {
    fun get(resultRow: ResultRow): MessageDuplicateData = MessageDuplicateData(
      resultRow[MessageDuplicateDataTable.messageId],
      resultRow[MessageDuplicateDataTable.hasSource],
      resultRow[MessageDuplicateDataTable.countImages],
      Json.decodeFromString(resultRow[MessageDuplicateDataTable.messageProblems])
    )
  }
}