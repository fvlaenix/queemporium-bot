package com.fvlaenix.queemporium.testing.fixture

import java.time.OffsetDateTime

data class TestFixture(
  val enabledFeatures: Set<String> = emptySet(),
  val guilds: List<GuildFixture> = emptyList(),
  val users: Map<String, UserFixture> = emptyMap()
)

data class GuildFixture(
  val name: String,
  val id: String = name,
  val channels: List<ChannelFixture> = emptyList()
)

data class ChannelFixture(
  val name: String,
  val id: String = name,
  val messages: List<MessageFixture> = emptyList()
)

data class MessageFixture(
  val author: String,
  val text: String,
  val attachments: List<String> = emptyList(),
  val reactions: List<ReactionFixture> = emptyList(),
  val timeCreated: OffsetDateTime? = null
)

data class ReactionFixture(
  val emoji: String,
  val users: List<String>
)

data class UserFixture(
  val name: String,
  val id: String = name,
  val isBot: Boolean = false,
  val discriminator: String = "0000"
)
