package com.fvlaenix.queemporium.mock

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageHistory

class TestMessageRetrieveAction private constructor(
  private val jda: JDA,
  private val messageHistory: MessageHistory?,
  private val error: Throwable?
) {
  fun complete(): MessageHistory {
    if (error != null) {
      throw error
    }
    return messageHistory!!
  }

  companion object {
    fun create(jda: JDA, messageHistory: MessageHistory): MessageHistory.MessageRetrieveAction {
      val action = mockk<MessageHistory.MessageRetrieveAction>(relaxed = true)

      every { action.complete() } returns messageHistory

      return action
    }

    fun createWithError(jda: JDA, error: Throwable): MessageHistory.MessageRetrieveAction {
      val action = mockk<MessageHistory.MessageRetrieveAction>(relaxed = true)

      every { action.complete() } throws error

      return action
    }
  }
}
