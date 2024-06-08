package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class MessageDependency(
  val targetMessage: String,
  val dependentMessage: String,
)

object MessageDependencyTable : Table() {
  val targetMessage = varchar("targetMessage", 300)
  val dependentMessage = varchar("dependentMessage", 300)
  
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
      it[targetMessage] = messageDependency.targetMessage
      it[dependentMessage] = messageDependency.dependentMessage
    }
  }
  
  fun getDependencies(targetMessage: String): List<String> = transaction(database) {
    MessageDependencyTable.select { MessageDependencyTable.targetMessage eq targetMessage }
      .map { it[MessageDependencyTable.dependentMessage] }
  }
  
  fun removeMessage(messageId: String) = transaction(database) {
    MessageDependencyTable.deleteWhere { 
      (MessageDependencyTable.targetMessage eq messageId) or 
              (MessageDependencyTable.dependentMessage eq messageId)
    }
  }
}