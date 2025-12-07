package com.fvlaenix.queemporium.testing.fixture

import java.time.OffsetDateTime

@DslMarker
annotation class FixtureDsl

@FixtureDsl
class FixtureBuilder {
  private val enabledFeatures = mutableSetOf<String>()
  private val guilds = mutableListOf<GuildFixture>()
  private val users = mutableMapOf<String, UserFixture>()

  fun enableFeature(featureKey: String) {
    enabledFeatures.add(featureKey)
  }

  fun enableFeatures(vararg featureKeys: String) {
    enabledFeatures.addAll(featureKeys)
  }

  fun user(id: String, block: UserFixtureBuilder.() -> Unit = {}): String {
    val builder = UserFixtureBuilder(id)
    builder.block()
    val userFixture = builder.build()
    users[id] = userFixture
    return id
  }

  fun guild(name: String, block: GuildFixtureBuilder.() -> Unit = {}): GuildFixture {
    val builder = GuildFixtureBuilder(name)
    builder.block()
    val guild = builder.build()
    guilds.add(guild)
    return guild
  }

  fun build(): TestFixture = TestFixture(
    enabledFeatures = enabledFeatures,
    guilds = guilds,
    users = users
  )
}

@FixtureDsl
class GuildFixtureBuilder(private val name: String) {
  private var id: String = name
  private val channels = mutableListOf<ChannelFixture>()

  fun id(value: String) {
    id = value
  }

  fun channel(name: String, block: ChannelFixtureBuilder.() -> Unit = {}): ChannelFixture {
    val builder = ChannelFixtureBuilder(name)
    builder.block()
    val channel = builder.build()
    channels.add(channel)
    return channel
  }

  fun build(): GuildFixture = GuildFixture(
    name = name,
    id = id,
    channels = channels
  )
}

@FixtureDsl
class ChannelFixtureBuilder(private val name: String) {
  private var id: String = name
  private val messages = mutableListOf<MessageFixture>()

  fun id(value: String) {
    id = value
  }

  fun message(
    author: String,
    text: String,
    block: MessageFixtureBuilder.() -> Unit = {}
  ): MessageFixture {
    val builder = MessageFixtureBuilder(author, text)
    builder.block()
    val message = builder.build()
    messages.add(message)
    return message
  }

  fun build(): ChannelFixture = ChannelFixture(
    name = name,
    id = id,
    messages = messages
  )
}

@FixtureDsl
class MessageFixtureBuilder(
  private val author: String,
  private val text: String
) {
  private val attachments = mutableListOf<String>()
  private val reactions = mutableListOf<ReactionFixture>()
  private var timeCreated: OffsetDateTime? = null

  fun attachment(url: String) {
    attachments.add(url)
  }

  fun timeCreated(time: OffsetDateTime) {
    timeCreated = time
  }

  fun reaction(emoji: String, block: ReactionFixtureBuilder.() -> Unit) {
    val builder = ReactionFixtureBuilder(emoji)
    builder.block()
    val reaction = builder.build()
    reactions.add(reaction)
  }

  fun build(): MessageFixture = MessageFixture(
    author = author,
    text = text,
    attachments = attachments,
    reactions = reactions,
    timeCreated = timeCreated
  )
}

@FixtureDsl
class ReactionFixtureBuilder(private val emoji: String) {
  private val users = mutableListOf<String>()

  fun user(userId: String) {
    users.add(userId)
  }

  fun users(vararg userIds: String) {
    users.addAll(userIds)
  }

  fun build(): ReactionFixture = ReactionFixture(
    emoji = emoji,
    users = users
  )
}

@FixtureDsl
class UserFixtureBuilder(private val id: String) {
  private var name: String = id
  private var isBot: Boolean = false
  private var discriminator: String = "0000"

  fun name(value: String) {
    name = value
  }

  fun isBot(value: Boolean) {
    isBot = value
  }

  fun discriminator(value: String) {
    discriminator = value
  }

  fun build(): UserFixture = UserFixture(
    name = name,
    id = id,
    isBot = isBot,
    discriminator = discriminator
  )
}

fun fixture(block: FixtureBuilder.() -> Unit): TestFixture {
  val builder = FixtureBuilder()
  builder.block()
  return builder.build()
}
