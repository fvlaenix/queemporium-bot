package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageDuplicateData(
  val messageId: MessageId,
  val hasSource: Boolean,
  val countImages: Int,
  val messageProblems: List<MessageProblem>
) {
  data class FullInfo(
    val messageId: MessageId,
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
      messageData.author,
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
    val existing = MessageDuplicateDataTable.select { MessageDuplicateDataTable.messageId eq Json.encodeToString(messageDuplicateData.messageId) }
    if (existing.count() > 0) return@transaction 
    MessageDuplicateDataTable.insert {
      it[messageId] = Json.encodeToString(messageDuplicateData.messageId)
      it[hasSource] = messageDuplicateData.hasSource
      it[countImages] = messageDuplicateData.countImages
      it[messageProblems] = Json.encodeToString(messageDuplicateData.messageProblems)
    }
  }
  
  fun get(messageId: MessageId) = transaction(database) {
    val result =
      MessageDuplicateDataTable.select { MessageDuplicateDataTable.messageId eq Json.encodeToString(messageId) }
        .singleOrNull()
    result?.let { get(it) }
  }
  
  fun delete(messageId: MessageId) = transaction(database) {
    MessageDuplicateDataTable.deleteWhere { MessageDuplicateDataTable.messageId eq Json.encodeToString(messageId) }
  }
  
  companion object {
    fun get(resultRow: ResultRow): MessageDuplicateData = MessageDuplicateData(
      Json.decodeFromString(resultRow[MessageDuplicateDataTable.messageId]),
      resultRow[MessageDuplicateDataTable.hasSource],
      resultRow[MessageDuplicateDataTable.countImages],
      Json.decodeFromString(resultRow[MessageDuplicateDataTable.messageProblems])
    )
  }
}