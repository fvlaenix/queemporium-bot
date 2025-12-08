package com.fvlaenix.queemporium.testing.scenario

import net.dv8tion.jda.api.entities.Message
import kotlin.time.Duration

sealed class ScenarioStep

data class SendMessageStep(
  val guildId: String,
  val channelId: String,
  val userId: String,
  val text: String,
  val attachments: List<Message.Attachment> = emptyList()
) : ScenarioStep()

data class AddReactionStep(
  val messageRef: MessageRef,
  val emoji: String,
  val userId: String
) : ScenarioStep()

data class AdvanceTimeStep(
  val duration: Duration
) : ScenarioStep()

data class AwaitAllStep(
  val description: String = "await all jobs"
) : ScenarioStep()

data class ExpectationStep(
  val description: String,
  val assertion: suspend (ScenarioContext) -> Unit
) : ScenarioStep()

data class MessageRef(
  val guildId: String,
  val channelId: String,
  val index: Int
)

data class ScenarioContext(
  val sentMessages: MutableList<Message> = mutableListOf(),
  val messagesByRef: MutableMap<MessageRef, Message> = mutableMapOf(),
  val answerService: com.fvlaenix.queemporium.service.AnswerService? = null
)
