package com.fvlaenix.queemporium.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class EmojiData(
  val messageId: String,
  val emojiId: String,
  val authorId: String
)

data class EmojiTopMessageData(
  val messageId: String,
  val authorName: String,
  val url: String,
  val emojiesCount: Int
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

  fun FieldSet.selectByTimeAndGuildChannel(
    guildId: String,
    channelId: String?,
    startEpoch: Long,
    endEpoch: Long
  ): Query =
    if (channelId != null) {
      select {
        (MessageDataTable.epoch greater startEpoch / 1000) and
            (MessageDataTable.epoch lessEq endEpoch / 1000) and
            (MessageDataTable.guildId eq guildId) and
            (MessageDataTable.channelId eq channelId)
      }
    } else {
      select {
        (MessageDataTable.epoch greater startEpoch / 1000) and
            (MessageDataTable.epoch lessEq endEpoch / 1000) and
            (MessageDataTable.guildId eq guildId)
      }
    }

  fun getTopMessages(
    guildId: String,
    channelId: String?,
    startEpoch: Long,
    endEpoch: Long,
    count: Int
  ): List<EmojiTopMessageData> = transaction(database) {
    val countEmojis = EmojiDataTable.messageId.count().alias("count_emojis")

    MessageDataTable
      .join(
        EmojiDataTable,
        JoinType.LEFT,
        additionalConstraint = { MessageDataTable.messageId eq EmojiDataTable.messageId }
      )
      .join(
        AuthorDataTable,
        JoinType.INNER,
        additionalConstraint = {
          (MessageDataTable.authorId eq AuthorDataTable.authorId) and
              (AuthorDataTable.guildId eq guildId)
        }
      )
      .slice(
        MessageDataTable.messageId,
        AuthorDataTable.authorName,
        MessageDataTable.url,
        countEmojis
      )
      .selectByTimeAndGuildChannel(guildId, channelId, startEpoch, endEpoch)
      .groupBy(
        MessageDataTable.messageId,
        AuthorDataTable.authorName,
        MessageDataTable.url
      )
      .orderBy(countEmojis, SortOrder.DESC)
      .limit(count)
      .map { row ->
        EmojiTopMessageData(
          messageId = row[MessageDataTable.messageId],
          authorName = row[AuthorDataTable.authorName],
          url = row[MessageDataTable.url],
          emojiesCount = row[countEmojis]
        )
      }
  }

  fun getMessagesAboveThreshold(
    guildId: String,
    threshold: Int,
  ): List<String> = transaction(database) {
    (EmojiDataTable.join(MessageDataTable, JoinType.INNER) {
      EmojiDataTable.messageId eq MessageDataTable.messageId
    })
      .slice(EmojiDataTable.messageId, EmojiDataTable.messageId.count())
      .select { MessageDataTable.guildId eq guildId }
      .groupBy(EmojiDataTable.messageId)
      .having { EmojiDataTable.messageId.count() greaterEq threshold }
      .map { it[EmojiDataTable.messageId] }
  }
}