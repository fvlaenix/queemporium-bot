package com.fvlaenix.queemporium.builder

import com.fvlaenix.queemporium.mock.TestEnvironment
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.hooks.ListenerAdapter

class TestBotBuilder(private val environment: TestEnvironment) {
  fun createGuild(name: String, block: GuildBuilder.() -> Unit = {}): Guild {
    val guild = environment.createGuild(name)
    GuildBuilder(environment, guild).apply(block)
    return guild
  }

  fun addListener(listener: ListenerAdapter) {
    environment.addListener(listener)
  }
}

class GuildBuilder(
  private val environment: TestEnvironment,
  private val guild: Guild
) {
  fun withChannel(name: String, block: ChannelBuilder.() -> Unit = {}): MessageChannelUnion {
    val channel = environment.createTextChannel(guild, name)
    ChannelBuilder(environment, channel).apply(block)
    return channel
  }
}

class ChannelBuilder(
  private val environment: TestEnvironment,
  private val channel: MessageChannelUnion
)

fun createEnvironment(block: TestBotBuilder.() -> Unit): TestEnvironment {
  val environment = TestEnvironment()
  val builder = TestBotBuilder(environment)
  builder.block()
  environment.start()
  return environment
}