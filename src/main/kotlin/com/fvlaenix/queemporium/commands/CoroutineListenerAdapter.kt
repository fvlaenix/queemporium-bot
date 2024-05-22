package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.DiscordBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

open class CoroutineListenerAdapter : ListenerAdapter() {
  private fun getCoroutinePool(): ExecutorCoroutineDispatcher = DiscordBot.MAIN_BOT_POOL
  private fun getCoroutineScope(): CoroutineScope = DiscordBot.MAIN_SCOPE
  
  open suspend fun onReadySuspend(event: ReadyEvent) {}
  
  override fun onReady(event: ReadyEvent) {
    runBlocking {
      onReadySuspend(event)
    }
  }

  open suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {}
  
  override fun onMessageReceived(event: MessageReceivedEvent) {
    getCoroutineScope().launch(getCoroutinePool()) {
      onMessageReceivedSuspend(event)
    }
  }

  open suspend fun onMessageUpdateSuspend(event: MessageUpdateEvent) {}
  
  override fun onMessageUpdate(event: MessageUpdateEvent) {
    getCoroutineScope().launch(getCoroutinePool()) {
      onMessageUpdateSuspend(event)
    }
  }

  open suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {}

  override fun onMessageDelete(event: MessageDeleteEvent) {
    getCoroutineScope().launch(getCoroutinePool()) {
      onMessageDeleteSuspend(event)
    }
  }
}