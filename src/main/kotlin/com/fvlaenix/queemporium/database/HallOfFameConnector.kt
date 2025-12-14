package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

enum class HallOfFameState {
  NOT_SELECTED,
  TO_SEND,
  POSTED
}

data class HallOfFameInfo(
  val guildId: String,
  val channelId: String,
  val threshold: Int
)

data class HallOfFameMessage(
  val messageId: String,
  val guildId: String,
  val timestamp: Long,
  val state: HallOfFameState,
  val hofMessageId: String?,
  val thresholdCrossDetectedAt: Long
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
  val state = varchar("state", 20)
  val hofMessageId = varchar("hofMessageId", 400).nullable()
  val thresholdCrossDetectedAt = long("thresholdCrossDetectedAt")

  init {
    index(false, guildId)
    index(false, timestamp)
    index(false, guildId, thresholdCrossDetectedAt)
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
      it[state] = message.state.name
      it[hofMessageId] = message.hofMessageId
      it[thresholdCrossDetectedAt] = message.thresholdCrossDetectedAt
    }
    true
  }

  fun markAsPosted(messageId: String, hofMessageId: String): Int = transaction(database) {
    HallOfFameMessagesTable.update({ HallOfFameMessagesTable.messageId eq messageId }) {
      it[state] = HallOfFameState.POSTED.name
      it[HallOfFameMessagesTable.hofMessageId] = hofMessageId
    }
  }

  fun getOldestToSendMessage(guildId: String): HallOfFameMessage? = transaction(database) {
    HallOfFameMessagesTable
      .select {
        (HallOfFameMessagesTable.guildId eq guildId) and
            (HallOfFameMessagesTable.state eq HallOfFameState.TO_SEND.name)
      }
      .orderBy(
        HallOfFameMessagesTable.thresholdCrossDetectedAt to SortOrder.ASC,
        HallOfFameMessagesTable.timestamp to SortOrder.ASC
      )
      .map {
        HallOfFameMessage(
          messageId = it[HallOfFameMessagesTable.messageId],
          guildId = it[HallOfFameMessagesTable.guildId],
          timestamp = it[HallOfFameMessagesTable.timestamp],
          state = HallOfFameState.valueOf(it[HallOfFameMessagesTable.state]),
          hofMessageId = it[HallOfFameMessagesTable.hofMessageId],
          thresholdCrossDetectedAt = it[HallOfFameMessagesTable.thresholdCrossDetectedAt]
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
          state = HallOfFameState.valueOf(it[HallOfFameMessagesTable.state]),
          hofMessageId = it[HallOfFameMessagesTable.hofMessageId],
          thresholdCrossDetectedAt = it[HallOfFameMessagesTable.thresholdCrossDetectedAt]
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

  fun computeHistogram(guildId: String, threshold: Int, lookbackDays: Long): Map<Long, Int> = transaction(database) {
    val emojiDataConnector = EmojiDataConnector(database)
    val messageDataConnector = MessageDataConnector(database)

    val messagesAboveThreshold = emojiDataConnector.getMessagesAboveThreshold(guildId, threshold)
    val cutoffEpoch = System.currentTimeMillis() - (lookbackDays * 24 * 60 * 60 * 1000)

    val messageEpochs = messagesAboveThreshold.mapNotNull { messageId ->
      messageDataConnector.get(messageId)?.epoch
    }.filter { it >= cutoffEpoch }

    val bins = listOf(1L, 2L, 3L, 5L, 7L, 14L, 30L, 60L, 90L)
    val nowEpoch = System.currentTimeMillis()

    bins.associateWith { days ->
      val binCutoff = nowEpoch - (days * 24 * 60 * 60 * 1000)
      messageEpochs.count { it >= binCutoff }
    }
  }

  fun markMessagesAsToSend(guildId: String, maxAgeDays: Long, currentTimeMillis: Long = System.currentTimeMillis()) =
    transaction(database) {
      val cutoffEpoch = currentTimeMillis - (maxAgeDays * 24 * 60 * 60 * 1000)
      HallOfFameMessagesTable.update({
        (HallOfFameMessagesTable.guildId eq guildId) and
            (HallOfFameMessagesTable.state eq HallOfFameState.NOT_SELECTED.name) and
            (HallOfFameMessagesTable.thresholdCrossDetectedAt greaterEq cutoffEpoch)
      }) {
        it[state] = HallOfFameState.TO_SEND.name
      }
    }

  fun updateOrInsertMessage(message: HallOfFameMessage) = transaction(database) {
    val existing = HallOfFameMessagesTable
      .select { HallOfFameMessagesTable.messageId eq message.messageId }
      .limit(1)
      .any()

    if (existing) {
      HallOfFameMessagesTable.update({ HallOfFameMessagesTable.messageId eq message.messageId }) {
        it[state] = message.state.name
        it[hofMessageId] = message.hofMessageId
        it[thresholdCrossDetectedAt] = message.thresholdCrossDetectedAt
      }
    } else {
      HallOfFameMessagesTable.insert {
        it[messageId] = message.messageId
        it[guildId] = message.guildId
        it[timestamp] = message.timestamp
        it[state] = message.state.name
        it[hofMessageId] = message.hofMessageId
        it[thresholdCrossDetectedAt] = message.thresholdCrossDetectedAt
      }
    }
  }

  fun cancelBacklog(guildId: String) = transaction(database) {
    HallOfFameMessagesTable.update({
      (HallOfFameMessagesTable.guildId eq guildId) and
          (HallOfFameMessagesTable.state eq HallOfFameState.TO_SEND.name)
    }) {
      it[state] = HallOfFameState.NOT_SELECTED.name
    }
  }
}
