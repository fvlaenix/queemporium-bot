package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag

private val LOG = Logging.getLogger(DiscordBot::class.java)

class DiscordBot(
  token: String,
  listeners: List<ListenerAdapter>
) {

  private val jda: JDABuilder = JDABuilder
    .createDefault(
      token,
      GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT
    )
    .enableIntents(GatewayIntent.GUILD_MEMBERS)
    .setChunkingFilter(ChunkingFilter.ALL)
    .setMemberCachePolicy(MemberCachePolicy.ALL)
    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
    .addEventListeners(*listeners.toTypedArray())
    .setActivity(Activity.customStatus("Dominates Emporium"))

  fun run() {
    LOG.info("Build bot")
    val bot = jda.build()
    LOG.info("Staring bot")
    bot.awaitReady()
  }
}