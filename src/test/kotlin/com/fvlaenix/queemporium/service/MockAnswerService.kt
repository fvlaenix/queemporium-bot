package com.fvlaenix.queemporium.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.concurrent.atomic.AtomicInteger

class MockAnswerService : AnswerService() {
  val currentAnswer = AtomicInteger()
  val answers: MutableList<String> = mutableListOf()

  override suspend fun sendMessage(
    destination: MessageChannel,
    text: String,
    imageWithFileNames: List<ImageUploadInfo>
  ): Deferred<String?> {
    answers.add(text)
    return CompletableDeferred(currentAnswer.incrementAndGet().toString())
  }
}