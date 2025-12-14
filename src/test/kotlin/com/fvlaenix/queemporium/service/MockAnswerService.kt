package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.service.AnswerService.ImageUploadInfo
import com.fvlaenix.queemporium.testing.trace.BotMessageEvent
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

data class MockAnswer(
  val channelId: String,
  val text: String,
  val imageWithFileNames: List<ImageUploadInfo>,
  val forwardFrom: String? = null,
)

class MockAnswerService : AnswerService() {
  val currentAnswer = AtomicInteger()
  val answers: MutableList<MockAnswer> = CopyOnWriteArrayList()

  override suspend fun sendMessage(
    destination: MessageChannel,
    text: String,
    imageWithFileNames: List<ImageUploadInfo>
  ): Deferred<String?> {
    ScenarioTraceCollector.addEvent(
      BotMessageEvent(
        timestamp = Instant.now(),
        channelId = destination.id,
        text = text,
        images = imageWithFileNames
      )
    )
    answers.add(
      MockAnswer(
        channelId = destination.id,
        text = text,
        imageWithFileNames = imageWithFileNames
      )
    )
    val id = currentAnswer.incrementAndGet().toString()
    return CompletableDeferred(id)
  }

  override suspend fun forwardMessage(
    message: Message,
    destination: MessageChannel,
    successCallback: (Message) -> Unit,
    failedCallback: (Throwable) -> Unit
  ): Deferred<String?> {
    ScenarioTraceCollector.addEvent(
      BotMessageEvent(
        timestamp = Instant.now(),
        channelId = destination.id,
        text = "",
        images = emptyList(),
        forwardFrom = message.id
      )
    )
    answers.add(
      MockAnswer(
        channelId = destination.id,
        text = "",
        imageWithFileNames = emptyList(),
        forwardFrom = message.id
      )
    )
    return CompletableDeferred(currentAnswer.incrementAndGet().toString())
  }

  override suspend fun sendFile(
    destination: MessageChannel,
    filename: String,
    bytes: ByteArray
  ): Deferred<String?> {
    ScenarioTraceCollector.addEvent(
      BotMessageEvent(
        timestamp = Instant.now(),
        channelId = destination.id,
        text = "File: $filename (${bytes.size} bytes)",
        images = emptyList()
      )
    )
    answers.add(
      MockAnswer(
        channelId = destination.id,
        text = "File: $filename (${bytes.size} bytes)",
        imageWithFileNames = emptyList()
      )
    )
    val id = currentAnswer.incrementAndGet().toString()
    return CompletableDeferred(id)
  }
}
