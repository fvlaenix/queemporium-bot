package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.service.AnswerServiceImpl
import com.fvlaenix.queemporium.service.CommandsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import java.util.logging.Level
import java.util.logging.Logger

private val LOG = Logger.getLogger(DiscordBot::class.java.name)

class DiscordBot(
  botConfiguration: BotConfiguration,
  databaseConfiguration: DatabaseConfiguration,
  commandsService: CommandsService,
  answerService: AnswerServiceImpl
) {
  companion object {
    @OptIn(DelicateCoroutinesApi::class)
    val MAIN_BOT_POOL = newFixedThreadPoolContext(4, "MainBotPool")
    val MAIN_SCOPE = CoroutineScope(Dispatchers.Default)
  }
  private val jda: JDABuilder = JDABuilder
    .createDefault(
      botConfiguration.token.raw,
      GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT
    )
    .enableIntents(GatewayIntent.GUILD_MEMBERS)
    .setChunkingFilter(ChunkingFilter.ALL)
    .setMemberCachePolicy(MemberCachePolicy.ALL)
    .addEventListeners(
      *commandsService.getCommands(botConfiguration, databaseConfiguration, answerService).toTypedArray()
    )
    .setActivity(Activity.customStatus("Dominates Emporium"))

  fun run() {
    LOG.log(Level.INFO, "Build bot")
    val bot = jda.build()
    LOG.log(Level.INFO, "Staring bot")
    bot.awaitReady()
  }
}