package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.service.AnswerService.ImageUploadInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.concurrent.atomic.AtomicInteger

data class MockAnswer(
  val text: String,
  val imageWithFileNames: List<ImageUploadInfo>,
  val forwardFrom: String? = null
)

class MockAnswerService : AnswerService() {
  val currentAnswer = AtomicInteger()
  val answers: MutableList<MockAnswer> = mutableListOf()

  override suspend fun sendMessage(
    destination: MessageChannel,
    text: String,
    imageWithFileNames: List<ImageUploadInfo>
  ): Deferred<String?> {
    answers.add(MockAnswer(text, imageWithFileNames))
    val id = currentAnswer.incrementAndGet().toString()
    return CompletableDeferred(id)
  }

  override suspend fun forwardMessage(
    message: Message,
    destination: MessageChannel,
    successCallback: (Message) -> Unit,
    failedCallback: (Throwable) -> Unit
  ): Deferred<String?> {
    answers.add(MockAnswer(
      "",
      emptyList(),
      message.id
    ))
    return CompletableDeferred(currentAnswer.incrementAndGet().toString())
  }
}