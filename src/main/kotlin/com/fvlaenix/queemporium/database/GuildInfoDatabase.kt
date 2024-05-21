package com.fvlaenix.queemporium.database

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class GuildInfo(
  val guildId: String,
  val duplicateChannelInfo: String?,
  val excludedChannelsIds: Set<String>
)

object GuildInfoTable : Table() {
  val guildId = varchar("guildId", 50).primaryKey()
  val duplicateChannelInfo = varchar("duplicateChannelInfo", length = 100).nullable()
  val excludedChannelsIds = varchar("excludedChannelsIds", length = 10000)
}

class GuildInfoConnector(private val database: Database) {
  init {
    transaction(database) {
      SchemaUtils.create(GuildInfoTable)
    }
  }
  
  fun getGuildInfo(guildId: String): GuildInfo? = transaction(database) {
    val guild = GuildInfoTable.select { GuildInfoTable.guildId eq guildId }
    if (guild.count() > 0) get(guild.single())
    else null
  }

  private fun setGuildInfo(guildId: String, duplicateId: String?, excludedChannelsId: Set<String>) =
    transaction(database) {
      val excludedChannelIdsString = Json.encodeToString(excludedChannelsId)
      val guild = GuildInfoTable.select { GuildInfoTable.guildId eq guildId }
      if (guild.count() > 0) {
        GuildInfoTable.update({ GuildInfoTable.guildId eq guildId }) {
          it[excludedChannelsIds] = excludedChannelIdsString
          it[duplicateChannelInfo] = duplicateId
        }
      } else {
        GuildInfoTable.insert {
          it[GuildInfoTable.guildId] = guildId
          it[excludedChannelsIds] = excludedChannelIdsString
          it[duplicateChannelInfo] = duplicateId
        }
      }
    }

  fun setDuplicateInfo(guildId: String, duplicateId: String?) {
    val guild = getGuildInfo(guildId) ?: getEmptyChannel(guildId)
    setGuildInfo(guild.guildId, duplicateId, guild.excludedChannelsIds)
  }

  fun addExcludedChannel(guildId: String, id: String) {
    val guild = getGuildInfo(guildId) ?: getEmptyChannel(guildId)
    setGuildInfo(guild.guildId, guild.duplicateChannelInfo, guild.excludedChannelsIds + id)
  }

  fun excludeExcludedChannel(guildId: String, id: String) {
    val guild = getGuildInfo(guildId) ?: getEmptyChannel(guildId)
    setGuildInfo(guild.guildId, guild.duplicateChannelInfo, guild.excludedChannelsIds - id)
  }

  fun getDuplicateInfoChannel(guildId: String): String? {
    val guild = getGuildInfo(guildId) ?: getEmptyChannel(guildId)
    return guild.duplicateChannelInfo
  }

  fun isChannelExclude(guildId: String, id: String): Boolean {
    val guild = getGuildInfo(guildId) ?: getEmptyChannel(guildId)
    return guild.excludedChannelsIds.contains(id)
  }

  fun getAllGuilds(): List<GuildInfo> = transaction(database) {
    return@transaction GuildInfoTable.selectAll().map { get(it) }
  }
  
  companion object {
    fun get(resultRow: ResultRow): GuildInfo = GuildInfo(
      resultRow[GuildInfoTable.guildId],
      resultRow[GuildInfoTable.duplicateChannelInfo],
      Json.decodeFromString(resultRow[GuildInfoTable.excludedChannelsIds])
    )

    fun getEmptyChannel(guildId: String): GuildInfo = GuildInfo(
      guildId, null, setOf()
    )
  }
}