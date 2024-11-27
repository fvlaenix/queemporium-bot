package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class PermissionsInfoCommand(
  val answerService: AnswerService
) : CoroutineListenerAdapter() {
  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw == "/shogun-sama show-permissions"

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) {
      answerService.sendReply(event.message, "This only applies to servers, stupid!")
      return
    }
    val message = event.message
    val isRealAdmin = message.isFromAdmin()
    val isRoleAdmin = message.isFromRoleAdmin()
    answerService.sendReply(
      destination = message,
      text = """
        Result:
        Real Admin: $isRealAdmin
        Role Admin: $isRoleAdmin
        Weak: true
      """.trimIndent()
    )
  }
}