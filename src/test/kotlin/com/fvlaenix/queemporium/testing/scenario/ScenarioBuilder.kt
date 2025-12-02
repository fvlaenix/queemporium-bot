package com.fvlaenix.queemporium.testing.scenario

import kotlin.time.Duration

@DslMarker
annotation class ScenarioDsl

@ScenarioDsl
class ScenarioBuilder {
  private val steps = mutableListOf<ScenarioStep>()

  fun sendMessage(
    guildId: String,
    channelId: String,
    userId: String,
    text: String
  ) {
    steps.add(SendMessageStep(guildId, channelId, userId, text))
  }

  fun addReaction(
    messageRef: MessageRef,
    emoji: String,
    userId: String
  ) {
    steps.add(AddReactionStep(messageRef, emoji, userId))
  }

  fun addReaction(
    guildId: String,
    channelId: String,
    messageIndex: Int,
    emoji: String,
    userId: String
  ) {
    val ref = MessageRef(guildId, channelId, messageIndex)
    steps.add(AddReactionStep(ref, emoji, userId))
  }

  fun advanceTime(duration: Duration) {
    steps.add(AdvanceTimeStep(duration))
  }

  fun awaitAll(description: String = "await all jobs") {
    steps.add(AwaitAllStep(description))
  }

  fun expect(description: String = "expectation", block: suspend ExpectContext.() -> Unit) {
    steps.add(ExpectationStep(description) { context ->
      val expectContext = ExpectContext(context)
      expectContext.block()
    })
  }

  fun messageRef(guildId: String, channelId: String, index: Int): MessageRef {
    return MessageRef(guildId, channelId, index)
  }

  fun build(): List<ScenarioStep> = steps.toList()
}

@ScenarioDsl
class ExpectContext(
  private val scenarioContext: ScenarioContext
) {
  private val mockAnswerService = scenarioContext.answerService as? com.fvlaenix.queemporium.service.MockAnswerService

  fun messageSentCount(count: Int) {
    if (mockAnswerService != null) {
      val actual = mockAnswerService.answers.size
      if (actual != count) {
        throw AssertionError("Expected $count bot messages sent, but got $actual")
      }
    } else {
      val actual = scenarioContext.sentMessages.size
      if (actual != count) {
        throw AssertionError("Expected $count messages sent, but got $actual")
      }
    }
  }

  fun noMessagesSent() {
    messageSentCount(0)
  }

  fun lastMessageContains(text: String) {
    if (mockAnswerService != null) {
      val lastAnswer = mockAnswerService.answers.lastOrNull()
        ?: throw AssertionError("No bot messages have been sent")

      if (!lastAnswer.text.contains(text)) {
        throw AssertionError("Last bot message '${lastAnswer.text}' does not contain '$text'")
      }
    } else {
      val lastMessage = scenarioContext.sentMessages.lastOrNull()
        ?: throw AssertionError("No messages have been sent")

      if (!lastMessage.contentRaw.contains(text)) {
        throw AssertionError("Last message '${lastMessage.contentRaw}' does not contain '$text'")
      }
    }
  }

  fun messageSent(channelId: String, textContains: String) {
    if (mockAnswerService != null) {
      val found = mockAnswerService.answers.any { answer ->
        answer.channelId == channelId && answer.text.contains(textContains)
      }

      if (!found) {
        throw AssertionError("No bot message in channel $channelId contains text '$textContains'")
      }
    } else {
      val found = scenarioContext.sentMessages.any { message ->
        message.channel.id == channelId && message.contentRaw.contains(textContains)
      }

      if (!found) {
        throw AssertionError("No message in channel $channelId contains text '$textContains'")
      }
    }
  }

  fun singleMessageSent(channelId: String? = null, textContains: String? = null) {
    if (mockAnswerService != null) {
      val answers = mockAnswerService.answers.filter { answer ->
        (channelId == null || answer.channelId == channelId) &&
            (textContains == null || answer.text.contains(textContains))
      }

      if (answers.size != 1) {
        throw AssertionError("Expected exactly 1 bot message matching criteria, but found ${answers.size}")
      }
    } else {
      val messages = scenarioContext.sentMessages.filter { message ->
        (channelId == null || message.channel.id == channelId) &&
            (textContains == null || message.contentRaw.contains(textContains))
      }

      if (messages.size != 1) {
        throw AssertionError("Expected exactly 1 message matching criteria, but found ${messages.size}")
      }
    }
  }
}

fun scenario(block: ScenarioBuilder.() -> Unit): List<ScenarioStep> {
  val builder = ScenarioBuilder()
  builder.block()
  return builder.build()
}
