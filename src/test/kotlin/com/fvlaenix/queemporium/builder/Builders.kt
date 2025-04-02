package com.fvlaenix.queemporium.builder

import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.CommandsServiceImpl
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.koin.core.context.GlobalContext

class TestBotBuilder(private val environment: TestEnvironment) {
  fun createGuild(name: String, block: GuildBuilder.() -> Unit = {}): Guild {
    val guild = environment.createGuild(name)
    GuildBuilder(environment, guild).apply(block)
    return guild
  }

  fun addListener(listener: ListenerAdapter) {
    environment.addListener(listener)
  }

  fun addCommandsFromKoin() {
    val koinContext = GlobalContext.getOrNull() ?: return

    if (koinContext.getAll<CommandsServiceImpl>().isNotEmpty()) {
      val commandsService = koinContext.get<CommandsServiceImpl>()

      if (koinContext.getAll<BotConfiguration>().isNotEmpty()) {
        val botConfiguration = koinContext.get<BotConfiguration>()

        commandsService.getCommands(botConfiguration).forEach { command ->
          addListener(command)
        }
      }
    }
  }
}

class GuildBuilder(
  private val environment: TestEnvironment,
  private val guild: Guild
) {
  fun withChannel(name: String, block: ChannelBuilder.() -> Unit = {}): MessageChannelUnion {
    val channel = environment.createTextChannel(guild, name)
    ChannelBuilder().apply(block)
    return channel
  }
}

class ChannelBuilder()

fun createEnvironment(autoStart: Boolean = true, block: TestBotBuilder.() -> Unit): TestEnvironment {
  val environment = TestEnvironment()
  val builder = TestBotBuilder(environment)
  builder.addCommandsFromKoin()
  builder.block()

  if (autoStart) {
    environment.start()
  }

  return environment
}