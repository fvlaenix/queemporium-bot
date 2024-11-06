package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.requests.restaction.CacheRestAction
import org.jetbrains.annotations.Unmodifiable
import java.time.OffsetDateTime
import java.util.*

class TestUser(
  private val testJda: JDA,
  private val idLong: Long,
  private val name: String,
  private val isBot: Boolean = false,
  private val discriminator: String = "0000"
) : User {
  override fun getJDA(): JDA = testJda

  override fun getName(): String = name
  override fun getDiscriminator(): String = discriminator
  override fun getId(): String = idLong.toString()
  override fun getIdLong(): Long = idLong
  override fun isBot(): Boolean = isBot
  override fun isSystem(): Boolean = false

  override fun openPrivateChannel(): CacheRestAction<PrivateChannel> {
    TODO("Not yet implemented")
  }

  override fun getEffectiveName(): String = name
  override fun getGlobalName(): String? = name
  override fun getMutualGuilds(): @Unmodifiable List<Guild?> = emptyList()
  override fun hasPrivateChannel(): Boolean = false
  override fun getAvatarId(): String? = null
  override fun getDefaultAvatarId(): String = "0"
  override fun getAsTag(): String = "$name#$discriminator"
  override fun getAvatarUrl(): String? = null
  override fun getDefaultAvatarUrl(): String = "https://example.com/default-avatar.png"
  override fun getEffectiveAvatarUrl(): String = getDefaultAvatarUrl()
  override fun retrieveProfile(): CacheRestAction<User.Profile?> {
    TODO("Not yet implemented")
  }

  override fun getFlagsRaw(): Int = 0
  override fun getFlags(): EnumSet<User.UserFlag?> = EnumSet.noneOf(User.UserFlag::class.java)
  override fun getTimeCreated(): OffsetDateTime = OffsetDateTime.now()
  override fun getAsMention(): String {
    TODO("Not yet implemented")
  }
}