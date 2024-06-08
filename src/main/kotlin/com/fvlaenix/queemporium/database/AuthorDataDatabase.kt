package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class AuthorData(
  val authorId: String,
  val guildId: String,
  val authorName: String
)

object AuthorDataTable: Table() {
  val authorId = varchar("authorId", 100)
  val guildId = varchar("guildId", 50)
  val authorName = varchar("authorName", 100)
  
  init {
    index(true, authorId, guildId)
  }
}

class AuthorDataConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(AuthorDataTable)
    }
  }
  
  fun insert(author: AuthorData) = transaction(database) {
    if (AuthorDataTable.select { (AuthorDataTable.authorId eq author.authorId) and (AuthorDataTable.guildId eq author.guildId) }.count() > 0) {
      AuthorDataTable.update({ (AuthorDataTable.authorId eq author.authorId) and (AuthorDataTable.guildId eq author.guildId) }) { it[authorName] = author.authorName }
    } else {
      AuthorDataTable.insert {
        it[authorId] = author.authorId
        it[guildId] = author.guildId
        it[authorName] = author.authorName
      }
    }
  }
}