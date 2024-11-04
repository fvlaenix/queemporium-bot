package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.commands.CommandsConstructor
import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.service.AnswerServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import net.dv8tion.jda.api.JDA
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
  answerService: AnswerServiceImpl
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
    .enableIntents(GatewayIntent.GUILD_MEMBERS)
    .setChunkingFilter(ChunkingFilter.ALL)
    .setMemberCachePolicy(MemberCachePolicy.ALL)
    .addEventListeners(
      *CommandsConstructor.convert(
        botConfiguration,
        databaseConfiguration,
        answerService
      ).toTypedArray()
    )
    .setActivity(Activity.customStatus("Dominates Emporium"))
    .build()

  fun run() {
    LOG.log(Level.INFO, "Staring bot")
    jda.awaitReady()
  }
}