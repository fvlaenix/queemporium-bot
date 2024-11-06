package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.configuration.BotConfiguration
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration

class MockBot(
  botConfiguration: BotConfiguration,
  databaseConfiguration: DatabaseConfiguration,
  commandsService: CommandsService,
  answerService: AnswerServiceImpl
) {

}