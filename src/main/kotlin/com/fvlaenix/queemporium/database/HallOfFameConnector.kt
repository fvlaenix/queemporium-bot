package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class HallOfFameInfo(
  val guildId: String,
  val channelId: String,
  val threshold: Int
)

data class HallOfFameMessage(
  val messageId: String,
  val guildId: String,
  val timestamp: Long,
  val isSent: Boolean
)

object HallOfFameInfoTable : Table() {
  val guildId = varchar("guildId", 50).primaryKey()
  val channelId = varchar("channelId", 100)
  val threshold = integer("threshold")
}

object HallOfFameMessagesTable : Table() {
  val messageId = varchar("messageId", 400).primaryKey()
  val guildId = varchar("guildId", 50)
  val timestamp = long("timestamp")
  val isSent = bool("isSent")

  init {
    index(false, guildId)
    index(false, timestamp)
  }
}

class HallOfFameConnector(private val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(HallOfFameInfoTable, HallOfFameMessagesTable)
    }
  }

  fun setHallOfFameInfo(guildId: String, channelId: String, threshold: Int) = transaction(database) {
    val existing = HallOfFameInfoTable.select { HallOfFameInfoTable.guildId eq guildId }.count() > 0
    if (existing) {
      HallOfFameInfoTable.update({ HallOfFameInfoTable.guildId eq guildId }) {
        it[HallOfFameInfoTable.channelId] = channelId
        it[HallOfFameInfoTable.threshold] = threshold
      }
    } else {
      HallOfFameInfoTable.insert {
        it[HallOfFameInfoTable.guildId] = guildId
        it[HallOfFameInfoTable.channelId] = channelId
        it[HallOfFameInfoTable.threshold] = threshold
      }
    }
  }

  fun getHallOfFameInfo(guildId: String): HallOfFameInfo? = transaction(database) {
    HallOfFameInfoTable
      .select { HallOfFameInfoTable.guildId eq guildId }
      .map {
        HallOfFameInfo(
          guildId = it[HallOfFameInfoTable.guildId],
          channelId = it[HallOfFameInfoTable.channelId],
          threshold = it[HallOfFameInfoTable.threshold]
        )
      }
      .singleOrNull()
  }

  fun addMessage(message: HallOfFameMessage): Boolean = transaction(database) {
    val exists = HallOfFameMessagesTable
      .select { HallOfFameMessagesTable.messageId eq message.messageId }
      .limit(1)
      .any()
    if (exists) {
      return@transaction false
    }

    HallOfFameMessagesTable.insert {
      it[messageId] = message.messageId
      it[guildId] = message.guildId
      it[timestamp] = message.timestamp
      it[isSent] = message.isSent
    }
    true
  }

  fun markAsSent(messageId: String): Int = transaction(database) {
    HallOfFameMessagesTable.update({ HallOfFameMessagesTable.messageId eq messageId }) {
      it[isSent] = true
    }
  }

  fun getOldestUnsentMessage(guildId: String): HallOfFameMessage? = transaction(database) {
    HallOfFameMessagesTable
      .select {
        (HallOfFameMessagesTable.guildId eq guildId) and
            (HallOfFameMessagesTable.isSent eq false)
      }
      .orderBy(HallOfFameMessagesTable.timestamp)
      .map {
        HallOfFameMessage(
          messageId = it[HallOfFameMessagesTable.messageId],
          guildId = it[HallOfFameMessagesTable.guildId],
          timestamp = it[HallOfFameMessagesTable.timestamp],
          isSent = it[HallOfFameMessagesTable.isSent]
        )
      }
      .firstOrNull()
  }

  fun getMessage(messageId: String): HallOfFameMessage? = transaction(database) {
    HallOfFameMessagesTable
      .select { HallOfFameMessagesTable.messageId eq messageId }
      .map {
        HallOfFameMessage(
          messageId = it[HallOfFameMessagesTable.messageId],
          guildId = it[HallOfFameMessagesTable.guildId],
          timestamp = it[HallOfFameMessagesTable.timestamp],
          isSent = it[HallOfFameMessagesTable.isSent]
        )
      }
      .singleOrNull()
  }

  fun getAll(): List<HallOfFameInfo> = transaction(database) {
    HallOfFameInfoTable.selectAll().map { resultRow ->
      HallOfFameInfo(
        guildId = resultRow[HallOfFameInfoTable.guildId],
        channelId = resultRow[HallOfFameInfoTable.channelId],
        threshold = resultRow[HallOfFameInfoTable.threshold],
      )
    }
  }
}
