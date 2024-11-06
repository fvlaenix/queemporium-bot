package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MockCommandService(
  val adapters: List<ListenerAdapter>
) : CommandsService {
  override fun getCommands(
    botConfiguration: BotConfiguration,
    databaseConfiguration: DatabaseConfiguration,
    answerService: AnswerServiceImpl
  ): List<ListenerAdapter> = adapters
}