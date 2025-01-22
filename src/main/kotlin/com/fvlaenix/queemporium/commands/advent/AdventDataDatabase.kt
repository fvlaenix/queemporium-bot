package com.fvlaenix.queemporium.commands.advent

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class AdventData(
  val messageId: String,
  val messageDescription: String,
  val guildPostId: String,
  val channelPostId: String,
  val epoch: Long,
  val isRevealed: Boolean
)

object AdventDataTable : Table() {
  val messageId = varchar("messageId", 255)
  val messageDescription = varchar("messageDescription", 1023)
  val guildPostId = varchar("guildPostId", 255)
  val channelPostId = varchar("channelPostId", 255)
  val epoch = long("epoch")
  val isRevealed = bool("isRevealed")
}

class AdventDataConnector(val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(AdventDataTable)
    }
  }

  fun initializeAdvent(adventsData: List<AdventData>) = transaction(database) {
    assert(adventsData.map { it.guildPostId }.distinct().size == 1)
    AdventDataTable.deleteWhere { AdventDataTable.guildPostId eq adventsData.first().guildPostId }
    adventsData.forEach { adventData ->
      AdventDataTable.insert {
        it[AdventDataTable.messageId] = adventData.messageId
        it[AdventDataTable.messageDescription] = adventData.messageDescription
        it[AdventDataTable.guildPostId] = adventData.guildPostId
        it[AdventDataTable.channelPostId] = adventData.channelPostId
        it[AdventDataTable.epoch] = adventData.epoch
        it[AdventDataTable.isRevealed] = adventData.isRevealed
      }
    }
  }

  fun getAdvents(): List<AdventData> = transaction(database) {
    AdventDataTable.selectAll().map { get(it) }
  }

  fun markAsRevealed(guildId: String, messageId: String) = transaction(database) {
    AdventDataTable.update({
      (AdventDataTable.guildPostId eq guildId) and
          (AdventDataTable.messageId eq messageId)
    }) {
      it[isRevealed] = true
    }
  }

  companion object {
    private fun get(resultRow: ResultRow): AdventData = AdventData(
      resultRow[AdventDataTable.messageId],
      resultRow[AdventDataTable.messageDescription],
      resultRow[AdventDataTable.guildPostId],
      resultRow[AdventDataTable.channelPostId],
      resultRow[AdventDataTable.epoch],
      resultRow[AdventDataTable.isRevealed]
    )
  }
}