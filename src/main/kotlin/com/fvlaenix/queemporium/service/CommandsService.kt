package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.configuration.BotConfiguration
import net.dv8tion.jda.api.hooks.ListenerAdapter

interface CommandsService {
  fun getCommands(
    botConfiguration: BotConfiguration
  ): List<ListenerAdapter>
}