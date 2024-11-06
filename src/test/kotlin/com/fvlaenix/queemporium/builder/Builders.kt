package com.fvlaenix.queemporium.builder

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

class BotBuilder internal constructor(val jda: JDA) {
  val guilds = mutableListOf<GuildBuilder>()

  fun guild(id: Long, name: String, block: GuildBuilder.() -> Unit): GuildBuilder {
    return GuildBuilder(jda, id, name).apply(block).apply { guilds.add(this) }
  }
}

class GuildBuilder internal constructor(val jda: JDA, val id: Long, val name: String) {
  val channels = mutableListOf<ChannelBuilder>()

  fun channel(id: Long, name: String, block: ChannelBuilder.() -> Unit): ChannelBuilder {
    return ChannelBuilder(this, id, name).apply(block).apply { channels.add(this) }
  }
}

class ChannelBuilder internal constructor(val guild: GuildBuilder, val id: Long, val name: String) {
  val messages = mutableListOf<MessageBuilder>()

  fun message(block: MessageBuilder.() -> Unit): MessageBuilder {
    return MessageBuilder().apply(block).apply { messages.add(this) }
  }
}

class MessageBuilder internal constructor() {

}

fun bot(jda: JDA, block: BotBuilder.() -> Unit): BotBuilder {
  return BotBuilder(jda).apply(block)
}