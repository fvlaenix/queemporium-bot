package com.fvlaenix.queemporium.mock

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Message

fun createTestAttachment(
  fileName: String
): Message.Attachment {
  val attachment = mockk<Message.Attachment>()
  every { attachment.fileName } returns fileName
  return attachment
}