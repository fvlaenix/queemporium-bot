package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class PingCommand(
  private val answerService: AnswerService,
) : CoroutineListenerAdapter() {
  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw == "/shogun-sama ping"

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    answerService.sendReply(event.message, "Pong!")
  }
}