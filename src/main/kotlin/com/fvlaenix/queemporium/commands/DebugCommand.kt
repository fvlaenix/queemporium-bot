package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import net.dv8tion.jda.api.events.session.ReadyEvent

class DebugCommand(coroutineProvider: BotCoroutineProvider) : CoroutineListenerAdapter(coroutineProvider) {
  override fun onReady(event: ReadyEvent) {
    super.onReady(event)
  }
}