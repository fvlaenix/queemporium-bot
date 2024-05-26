package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageDependency(
  val targetMessage: MessageId,
  val dependentMessage: MessageId,
)

object MessageDependencyTable : Table() {
  val targetMessage = varchar("targetMessage", 1000)
  val dependentMessage = varchar("dependentMessage", 1000)
  
  init {
    index(false, targetMessage)
  }
}

class MessageDependencyConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(MessageDependencyTable)
    }
  }
  
  fun addDependency(messageDependency: MessageDependency) = transaction(database) {
    MessageDependencyTable.insert {
      it[targetMessage] = Json.encodeToString(messageDependency.targetMessage)
      it[dependentMessage] = Json.encodeToString(messageDependency.dependentMessage)
    }
  }
  
  fun getDependencies(targetMessage: MessageId): List<MessageId> = transaction(database) {
    MessageDependencyTable.select { MessageDependencyTable.targetMessage eq Json.encodeToString(targetMessage) }
      .map { Json.decodeFromString<MessageId>(it[MessageDependencyTable.dependentMessage]) }
  }
  
  fun removeMessage(messageId: MessageId) = transaction(database) {
    MessageDependencyTable.deleteWhere { 
      (MessageDependencyTable.targetMessage eq Json.encodeToString(messageId)) or 
              (MessageDependencyTable.dependentMessage eq Json.encodeToString(messageId))
    }
  }
}