package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.DiscordBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.logging.Level
import java.util.logging.LogManager

val LOG = LogManager.getLogManager().getLogger(CoroutineListenerAdapter::class.java.name)

open class CoroutineListenerAdapter : ListenerAdapter() {
  private fun getCoroutinePool(): ExecutorCoroutineDispatcher = DiscordBot.MAIN_BOT_POOL
  private fun getCoroutineScope(): CoroutineScope = DiscordBot.MAIN_SCOPE
  
  private fun messageInfo(message: Message): String =
    "message from ${message.author.name} with url: ${message.jumpUrl}"
  
  protected fun Message.isFromAdmin(): Boolean = member?.hasPermission(Permission.ADMINISTRATOR) == true
  
  open suspend fun onReadySuspend(event: ReadyEvent) {}
  
  override fun onReady(event: ReadyEvent) {
    runBlocking {
      try {
        onReadySuspend(event)
      } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Global Panic: Can't finish ready event", e)
      }
    }
  }
  
  protected open fun receiveMessageFilter(event: MessageReceivedEvent): Boolean = true
  
  open suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {}
  
  override fun onMessageReceived(event: MessageReceivedEvent) {
    if (!receiveMessageFilter(event)) return
    getCoroutineScope().launch(getCoroutinePool()) {
      try {
        onMessageReceivedSuspend(event)
      } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Global Panic: Received ${messageInfo(event.message)}", e)
      }
    }
  }

  open suspend fun onMessageUpdateSuspend(event: MessageUpdateEvent) {}
  
  override fun onMessageUpdate(event: MessageUpdateEvent) {
    getCoroutineScope().launch(getCoroutinePool()) {
      try {
        onMessageUpdateSuspend(event)
      } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Global Panic: Updated ${messageInfo(event.message)}", e)
      }
    }
  }

  open suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {}

  override fun onMessageDelete(event: MessageDeleteEvent) {
    getCoroutineScope().launch(getCoroutinePool()) {
      try {
        onMessageDeleteSuspend(event)
      } catch (e: Exception) {
        LOG.log(Level.SEVERE, "Global Panic: Deleted message with id ${event.messageId}", e)
      }
    }
  }
}