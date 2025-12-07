package com.fvlaenix.queemporium.builder

import com.fvlaenix.queemporium.mock.TestEnvironment
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.koin.core.context.GlobalContext

/**
 * Legacy environment builder for gRPC integration tests.
 *
 * New tests should use the modern DSL (testBot/testBotFixture) instead.
 * This builder is retained for:
 * - Existing gRPC tests that require attachment support
 * - Tests that need direct JDA API access for advanced scenarios
 * - Backward compatibility with legacy test code
 */
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

    koinContext.getAll<ListenerAdapter>().forEach { command ->
      addListener(command)
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

/**
 * Creates a test environment for gRPC integration tests.
 *
 * LEGACY API: New tests should use testBot {} or testBotFixture {} instead.
 * This builder is retained for tests requiring attachment support or direct JDA access.
 *
 * @param autoStart Whether to automatically start the environment (default: true)
 * @param block Builder configuration lambda
 * @return Configured test environment
 */
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
