package com.fvlaenix.queemporium.testing.scenario

import com.fvlaenix.queemporium.mock.TestEmoji
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.mock.TestTextChannel
import com.fvlaenix.queemporium.testing.dsl.ChannelResolver
import com.fvlaenix.queemporium.testing.dsl.GuildResolver
import com.fvlaenix.queemporium.testing.fixture.TestEnvironmentWithTime
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.time.TimeController
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User

class ScenarioRunner(
  private val environment: TestEnvironment,
  private val timeController: TimeController?,
  private val testEnvWithTime: TestEnvironmentWithTime,
  private val answerService: com.fvlaenix.queemporium.service.AnswerService? = null
) {
  private val context = ScenarioContext(answerService = answerService)
  private val userCache = testEnvWithTime.userMap.toMutableMap()
  private val guildCache = mutableMapOf<String, Guild>()

  init {
    // Pre-populate messagesByRef with fixture messages
    environment.jda.guilds.forEach { guild ->
      guild.channels.filterIsInstance<TestTextChannel>().forEach { channel ->
        channel.messages.toList().forEachIndexed { index, message ->
          val ref = MessageRef(guild.name, channel.name, index)
          context.messagesByRef[ref] = message
          context.messagesById[message.id] = message
        }
      }
    }
  }

  suspend fun run(steps: List<ScenarioStep>) {
    for (step in steps) {
      executeStep(step)
    }
  }

  private suspend fun executeStep(step: ScenarioStep) {
    when (step) {
      is SendMessageStep -> executeSendMessage(step)
      is AddReactionStep -> executeAddReaction(step)
      is AddReactionByIdStep -> executeAddReactionById(step)
      is AdvanceTimeStep -> executeAdvanceTime(step)
      is AwaitAllStep -> executeAwaitAll()
      is ExpectationStep -> executeExpectation(step)
    }
  }

  private fun executeSendMessage(step: SendMessageStep) {
    val guild = getGuild(step.guildId)
    val channel = ChannelResolver.resolve(guild, step.channelId)

    val user = getUser(step.userId)

    val message = environment.sendMessage(
      guildName = guild.name,
      channelName = channel.name,
      user = user,
      message = step.text,
      attachments = step.attachments
    ).complete(true) as? Message

    if (message != null) {
      context.sentMessages.add(message)

      val channelMessages = (channel as TestTextChannel).messages
      val messageIndex = channelMessages.indexOf(message)
      if (messageIndex >= 0) {
        val ref = MessageRef(step.guildId, step.channelId, messageIndex)
        context.messagesByRef[ref] = message
      }
      context.messagesById[message.id] = message
    }
  }

  private fun executeAddReaction(step: AddReactionStep) {
    val message = context.messagesByRef[step.messageRef]
      ?: throw IllegalStateException("Message not found for reference: ${step.messageRef}")

    val user = getUser(step.userId)
    val emoji = TestEmoji(step.emoji)

    environment.addReactionWithEvent(message, emoji, user)
  }

  private fun executeAddReactionById(step: AddReactionByIdStep) {
    val message = findMessageById(step.messageId)
      ?: throw IllegalStateException("Message not found for id: ${step.messageId}")

    val user = getUser(step.userId)
    val emoji = TestEmoji(step.emoji)

    environment.addReactionWithEvent(message, emoji, user)
  }

  private suspend fun executeAdvanceTime(step: AdvanceTimeStep) {
    if (timeController == null) {
      throw IllegalStateException("Cannot advance time without a VirtualClock")
    }

    timeController.advanceTime(step.duration)
    testEnvWithTime.awaitAll()
  }

  private suspend fun executeAwaitAll() {
    testEnvWithTime.awaitAll()
  }

  private suspend fun executeExpectation(step: ExpectationStep) {
    step.assertion(context)
  }

  private fun getUser(userId: String): User {
    return userCache.getOrPut(userId) {
      environment.jda.guilds
        .flatMap { it.members }
        .map { it.user }
        .find { it.name == userId || it.id == userId }
        ?: throw IllegalStateException("User $userId not found")
    }
  }

  private fun getGuild(guildId: String): Guild {
    return guildCache.getOrPut(guildId) {
      GuildResolver.resolve(environment.jda, guildId)
    }
  }

  private fun findMessageById(messageId: String): Message? {
    context.messagesById[messageId]?.let { return it }

    environment.jda.guilds.forEach { guild ->
      guild.channels.filterIsInstance<TestTextChannel>().forEach { channel ->
        val message = channel.messages.toList().find { it.id == messageId }
        if (message != null) {
          context.messagesById[messageId] = message
          return message
        }
      }
    }
    return null
  }
}

suspend fun TestEnvironmentWithTime.runScenario(
  answerService: com.fvlaenix.queemporium.service.AnswerService? = null,
  block: ScenarioBuilder.() -> Unit
) {
  val steps = scenario(block)
  val runner = ScenarioRunner(environment, timeController, this, answerService)
  runner.run(steps)
}

fun TestEnvironmentWithTime.runScenarioBlocking(
  answerService: com.fvlaenix.queemporium.service.AnswerService? = null,
  block: ScenarioBuilder.() -> Unit
) {
  runBlocking {
    runScenario(answerService, block)
  }
}
