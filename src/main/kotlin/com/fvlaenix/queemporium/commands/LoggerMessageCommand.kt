package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent

private val LOG = Logging.getLogger(LoggerMessageCommand::class.java)

class LoggerMessageCommand(coroutineProvider: BotCoroutineProvider) : CoroutineListenerAdapter(coroutineProvider) {
  override suspend fun onReadySuspend(event: ReadyEvent) {
    LOG.info("Ready event got")
  }

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    LOG.info("Received ${messageInfo(event.message)}")
  }

  override suspend fun onMessageUpdateSuspend(event: MessageUpdateEvent) {
    LOG.info("Update ${messageInfo(event.message)}")
  }

  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    LOG.info("Delete message with id ${event.messageId}")
  }
}
