package com.fvlaenix.queemporium.commands

import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.LogManager

private val LOG = LogManager.getLogManager().getLogger(LoggerMessageCommand::class.java.name)

class LoggerMessageCommand : CoroutineListenerAdapter() {
  override suspend fun onReadySuspend(event: ReadyEvent) {
    LOG.log(Level.INFO, "Ready event got")
  }

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    LOG.log(Level.INFO, "Received ${messageInfo(event.message)}")
  }

  override suspend fun onMessageUpdateSuspend(event: MessageUpdateEvent) {
    LOG.log(Level.INFO, "Update ${messageInfo(event.message)}")
  }

  override suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {
    LOG.log(Level.INFO, "Delete message with id ${event.messageId}")
  }
}