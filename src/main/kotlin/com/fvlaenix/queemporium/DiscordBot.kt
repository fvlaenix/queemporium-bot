package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.commands.CommandsConstructor
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.logging.Level
import java.util.logging.LogManager

private val LOG = LogManager.getLogManager().getLogger(DiscordBot::class.java.name)

class DiscordBot(
  botConfiguration: BotConfiguration,
  databaseConfiguration: DatabaseConfiguration
) {
  companion object {
    @OptIn(DelicateCoroutinesApi::class)
    val MAIN_BOT_POOL = newFixedThreadPoolContext(4, "MainBotPool")
    val MAIN_SCOPE = CoroutineScope(Dispatchers.Default)
  }
  private val jda: JDA = JDABuilder
    .createDefault(
      botConfiguration.token,
      GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT
    )
    .addEventListeners(CommandsConstructor.convert(botConfiguration, databaseConfiguration))
    .setActivity(Activity.customStatus("Dominates Emporium"))
    .build()

  fun run() {
    LOG.log(Level.INFO, "Staring bot")
    jda.awaitReady()
  }
}