package com.fvlaenix.queemporium.verification

import com.fvlaenix.queemporium.service.MockAnswer
import com.fvlaenix.queemporium.service.MockAnswerService
import kotlin.test.assertEquals

class MessageVerificationBuilder(
  private val message: MockAnswer
) {
  fun text(expected: String) {
    assertEquals(expected, message.text, "Message '${message.text}' is not equal to '$expected'")
  }

  fun channelId(id: String) {
    assertEquals(id, message.channelId, "Expected channel id $id but found ${message.channelId}")
  }
}

fun MockAnswerService.verify(block: VerificationBuilder.() -> Unit) {
  VerificationBuilder(this).apply(block)
}