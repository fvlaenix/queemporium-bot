package com.fvlaenix.queemporium.mock

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

object TestMessageHistoryFactory {
  fun create(channel: MessageChannel, messages: List<Message>): MessageHistory {
    val messageHistory = mockk<MessageHistory>(relaxed = true)

    every { messageHistory.getMessageById(any<String>()) } answers {
      val messageId = firstArg<String>()
      messages.find { it.id == messageId }
    }

    every { messageHistory.retrievedHistory } returns messages

    return messageHistory
  }
}
