package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.commands.halloffame.HallOfFameCommand
import com.fvlaenix.queemporium.database.EmojiData
import com.fvlaenix.queemporium.database.MessageEmojiData
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.session.ReadyEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class HallOfFameDsl(
  private val setupContext: BotTestSetupContext,
  private val scenarioBuilder: com.fvlaenix.queemporium.testing.scenario.ScenarioBuilder? = null
) {
  private val hallOfFameConnector get() = setupContext.hallOfFameConnector

  fun configure(
    guildId: String,
    hallOfFameChannelId: String,
    threshold: Int = 5,
    adminUserId: String = "admin"
  ) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "hof.configure",
        "guildId" to guildId,
        "channelId" to hallOfFameChannelId,
        "threshold" to threshold,
        "adminUserId" to adminUserId
      )
    )
    val commandMessage = "/shogun-sama set-hall-of-fame $threshold this-channel"

    if (scenarioBuilder != null) {
      scenarioBuilder.sendMessage(guildId, hallOfFameChannelId, adminUserId, commandMessage)
      scenarioBuilder.awaitAll("hof.configure")
      return
    }

    val guild = setupContext.guild(guildId)
    val channel = setupContext.channel(guild, hallOfFameChannelId)
    val adminUser = setupContext.resolveUser(adminUserId)

    setupContext.envWithTime.environment.sendMessage(
      guildName = guild.name,
      channelName = channel.name,
      user = adminUser,
      message = commandMessage
    )
    runBlocking { setupContext.envWithTime.awaitAll() }
  }

  fun configureBlocking(
    guildId: String,
    hallOfFameChannelId: String,
    threshold: Int = 5,
    adminUserId: String = "admin"
  ) = configure(guildId, hallOfFameChannelId, threshold, adminUserId)

  fun configureHallOfFame(
    guildId: String,
    hallOfFameChannelId: String,
    threshold: Int = 5,
    adminUserId: String = "admin"
  ) = configure(guildId, hallOfFameChannelId, threshold, adminUserId)

  fun configureHallOfFameBlocking(
    guildId: String,
    hallOfFameChannelId: String,
    threshold: Int = 5,
    adminUserId: String = "admin"
  ) = configureBlocking(guildId, hallOfFameChannelId, threshold, adminUserId)

  fun seedEmojiReactions(
    guildId: String,
    channelId: String,
    messageIndex: Int,
    emoji: String = "⭐",
    userIds: List<String>
  ) {
    val guild = setupContext.guild(guildId)
    val channel = setupContext.channel(guild, channelId)
    val message = setupContext.message(channel, messageIndex, MessageOrder.OLDEST_FIRST)
    seedEmojiReactions(message, emoji, userIds)
  }

  // TODO should be emojies service responsibility
  fun seedMessageToCount(
    guildId: String,
    channelId: String,
    messageIndex: Int,
    count: Int,
    emoji: String = "⭐"
  ) {
    val userIds = (1..count).map { "hof-reaction-$it" }
    val guild = setupContext.guild(guildId)
    userIds.forEach { id ->
      if (setupContext.envWithTime.userMap[id] == null) {
        val user = setupContext.envWithTime.environment.createUser(id)
        setupContext.envWithTime.environment.createMember(guild, user)
      }
    }
    seedEmojiReactions(guildId, channelId, messageIndex, emoji, userIds)
  }

  fun seedEmojiReactions(
    message: Message,
    emoji: String,
    userIds: List<String>
  ) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "hof.seedEmojiReactions",
        "messageId" to message.id,
        "emoji" to emoji,
        "userIds" to userIds
      )
    )

    val emojiDataConnector = setupContext.emojiDataConnector
    val messageEmojiDataConnector = setupContext.messageEmojiDataConnector

    userIds.forEach { userId ->
      val user = setupContext.resolveUser(userId)
      emojiDataConnector.insert(
        EmojiData(
          messageId = message.id,
          emojiId = emoji,
          authorId = user.id
        )
      )
    }

    val newCount = (messageEmojiDataConnector.get(message.id)?.count ?: 0) + userIds.size
    messageEmojiDataConnector.insert(MessageEmojiData(message.id, newCount))

    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "hof.seededEmojiCount",
        "messageId" to message.id,
        "count" to newCount
      )
    )
  }

  suspend fun triggerRetrieveJob() {
    ScenarioTraceCollector.logDslAction(
      mapOf("action" to "hof.triggerRetrieveJob")
    )
    advanceTimeInternal(9.hours, "hof.triggerRetrieveJob")
  }

  suspend fun triggerSendJob() {
    ScenarioTraceCollector.logDslAction(
      mapOf("action" to "hof.triggerSendJob")
    )
    advanceTimeInternal(4.hours, "hof.triggerSendJob")
  }

  suspend fun triggerBothJobs() {
    triggerRetrieveJob()
    triggerSendJob()
  }

  suspend fun recheckMessage(messageId: String, guildId: String) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "hof.recheckMessage",
        "messageId" to messageId,
        "guildId" to guildId
      )
    )
    val command = org.koin.core.context.GlobalContext.get().get<HallOfFameCommand>()
    command.recheckMessage(messageId, guildId)
    setupContext.envWithTime.awaitAll()
  }

  suspend fun recheckMessage(message: Message, guildId: String) = recheckMessage(message.id, guildId)

  suspend fun recheckGuild(guildId: String) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "hof.recheckGuild",
        "guildId" to guildId
      )
    )
    val command = org.koin.core.context.GlobalContext.get().get<HallOfFameCommand>()
    command.recheckGuild(guildId)
    setupContext.envWithTime.awaitAll()
  }

  fun expectQueued(
    messageId: String,
    isSent: Boolean? = null
  ) {
    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "hof.expectQueued",
          "messageId" to messageId,
          "isSent" to isSent
        )
      )

      val entry = hallOfFameConnector.getMessage(messageId)
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "hof.queueEntry",
          "messageId" to messageId,
          "entry" to entry
        )
      )
      if (entry == null) {
        throw AssertionError("Hall of Fame entry for $messageId is missing")
      }
      isSent?.let { expected ->
        if (entry.isSent != expected) {
          throw AssertionError("Expected Hall of Fame entry isSent=$expected but was ${entry.isSent}")
        }
      }
    }

    scenarioBuilder?.expect("hof.expectQueued") { assertion() } ?: runBlocking { assertion() }
  }

  fun expectNotQueued(messageId: String) {
    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "hof.expectNotQueued",
          "messageId" to messageId
        )
      )
      val entry = hallOfFameConnector.getMessage(messageId)
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "hof.queueEntry",
          "messageId" to messageId,
          "entry" to entry
        )
      )
      if (entry != null) {
        throw AssertionError("Hall of Fame entry for $messageId should not exist")
      }
    }
    scenarioBuilder?.expect("hof.expectNotQueued") { assertion() } ?: runBlocking { assertion() }
  }

  fun expectAnnouncementUpdated(messageId: String, expectedCount: Int) {
    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "hof.expectAnnouncementUpdated",
          "messageId" to messageId,
          "expectedCount" to expectedCount
        )
      )
      val service = setupContext.answerService
        ?: throw AssertionError("MockAnswerService required to assert announcement updates")
      val found = service.answers.any { answer ->
        answer.text.contains("$expectedCount reactions")
      }
      if (!found) {
        throw AssertionError("Announcement for $messageId was not updated to $expectedCount reactions")
      }
    }
    scenarioBuilder?.expect("hof.expectAnnouncementUpdated") { assertion() } ?: runBlocking { assertion() }
  }

  fun expectQueued(message: Message, isSent: Boolean? = null) = expectQueued(message.id, isSent)

  fun expectNotQueued(message: Message) = expectNotQueued(message.id)

  fun restartJobs() {
    ScenarioTraceCollector.logDslAction(
      mapOf("action" to "hof.restartJobs")
    )
    val command = org.koin.core.context.GlobalContext.get().get<HallOfFameCommand>()
    val readyEvent = mockk<ReadyEvent>()
    every { readyEvent.jda } returns setupContext.envWithTime.environment.jda
    command.onEvent(readyEvent)
  }

  fun awaitProcessing(description: String = "hof.awaitProcessing") {
    if (scenarioBuilder != null) {
      scenarioBuilder.awaitAll(description)
    } else {
      runBlocking { setupContext.envWithTime.awaitAll() }
    }
  }

  private suspend fun advanceTimeInternal(duration: Duration, description: String) {
    val timeController = setupContext.envWithTime.timeController
      ?: throw IllegalStateException("TimeController is required for $description. Use withVirtualTime in test setup.")
    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "hof.timeAdvance",
        "durationMs" to duration.inWholeMilliseconds
      )
    )
    timeController.advanceTime(duration)
    setupContext.envWithTime.awaitAll()
  }
}
