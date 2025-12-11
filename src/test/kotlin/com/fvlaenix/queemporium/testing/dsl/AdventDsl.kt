package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.commands.advent.AdventCommand
import com.fvlaenix.queemporium.commands.advent.AdventData
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AdventDsl(
  private val setupContext: BotTestSetupContext,
  private val scenarioBuilder: com.fvlaenix.queemporium.testing.scenario.ScenarioBuilder? = null
) {
  private val adventDataConnector get() = setupContext.adventDataConnector
  private val answerService get() = setupContext.answerService
  private val envWithTime get() = setupContext.envWithTime

  fun configureViaCommand(
    guildId: String,
    channelId: String,
    adminUserId: String = "admin",
    start: String? = null,
    finish: String? = null,
    count: Int? = null,
    year: Int? = null
  ) {
    val parts = mutableListOf(AdventCommand.COMMAND_PREFIX)
    start?.let { parts.add("start:$it") }
    finish?.let { parts.add("finish:$it") }
    parts.add("guildId:$guildId")
    parts.add("channelId:$channelId")
    count?.let { parts.add("count:$it") }
    year?.let { parts.add("year:$it") }
    val command = parts.joinToString(" ")

    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "advent.configureViaCommand",
        "guildId" to guildId,
        "channelId" to channelId,
        "adminUserId" to adminUserId,
        "params" to mapOf(
          "start" to start,
          "finish" to finish,
          "count" to count,
          "year" to year
        )
      )
    )

    if (scenarioBuilder != null) {
      scenarioBuilder.sendMessage(guildId, channelId, adminUserId, command)
      scenarioBuilder.awaitAll("advent.configureViaCommand")
      return
    }

    val guild = setupContext.guild(guildId)
    val channel = setupContext.channel(guild, channelId)
    val user = resolveUser(adminUserId)

    setupContext.envWithTime.environment.sendMessage(
      guildName = guild.name,
      channelName = channel.name,
      user = user,
      message = command
    )

    runBlocking {
      setupContext.envWithTime.awaitAll()
    }
  }

  // TODO should be replaced with command
  fun scheduleEntries(
    guildId: String,
    postChannelId: String,
    entries: List<Triple<String, String, Instant>>,
    restartLoop: Boolean = true
  ) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "advent.scheduleEntries",
        "guildId" to guildId,
        "postChannelId" to postChannelId,
        "entries" to entries.map {
          mapOf(
            "messageId" to it.first,
            "description" to it.second,
            "epoch" to it.third.toEpochMilli()
          )
        },
        "restartLoop" to restartLoop
      )
    )

    val adventDataList = entries.map { (messageId, description, revealTime) ->
      AdventData(
        messageId = messageId,
        messageDescription = description,
        guildPostId = setupContext.guild(guildId).id,
        channelPostId = setupContext.channel(guildId, postChannelId).id,
        epoch = revealTime.toEpochMilli(),
        isRevealed = false
      )
    }

    adventDataConnector.initializeAdvent(adventDataList)

    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "adventData.initialize",
        "inserted" to adventDataList.size,
        "total" to adventDataConnector.getAdvents().size
      )
    )

    if (restartLoop) {
      restartAdventLoop()
    }
  }

  // TODO should be replaced with command
  fun scheduleMultipleEntries(
    guildId: String,
    postChannelId: String,
    entries: List<Pair<String, String>>,
    startTime: Instant,
    interval: Duration,
    restartLoop: Boolean = true
  ) {
    val triples = entries.mapIndexed { index, (messageId, description) ->
      val revealTime = startTime.plusMillis(interval.inWholeMilliseconds * index)
      Triple(messageId, description, revealTime)
    }
    scheduleEntries(guildId, postChannelId, triples, restartLoop)
  }

  suspend fun advanceToNextReveal() {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "advent.advanceToNextReveal"
      )
    )
    val timeController = envWithTime.timeController
      ?: throw IllegalStateException("TimeController is required for advanceToNextReveal. Use withVirtualTime in test setup.")

    val nextEntry = adventDataConnector.getAdvents()
      .filter { !it.isRevealed }
      .minByOrNull { it.epoch }
      ?: throw IllegalStateException("No unrevealed Advent entries found")

    val currentTime = timeController.getCurrentTime()
    val nextRevealTime = Instant.ofEpochMilli(nextEntry.epoch)
    val duration = java.time.Duration.between(currentTime, nextRevealTime)

    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "adventData.nextUnrevealed",
        "nextMessageId" to nextEntry.messageId,
        "nextEpoch" to nextEntry.epoch,
        "currentEpoch" to currentTime.toEpochMilli()
      )
    )

    if (duration.toMillis() > 0) {
      timeController.advanceTime(duration.toMillis().milliseconds)
    }
    envWithTime.awaitAll()
  }

  suspend fun revealAllEntries() {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "advent.revealAllEntries"
      )
    )
    val timeController = envWithTime.timeController
      ?: throw IllegalStateException("TimeController is required for revealAllEntries. Use withVirtualTime in test setup.")

    val lastEntry = adventDataConnector.getAdvents()
      .filter { !it.isRevealed }
      .maxByOrNull { it.epoch }
      ?: return

    val currentTime = timeController.getCurrentTime()
    val lastRevealTime = Instant.ofEpochMilli(lastEntry.epoch)
    val duration = java.time.Duration.between(currentTime, lastRevealTime)

    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "adventData.lastUnrevealed",
        "lastMessageId" to lastEntry.messageId,
        "lastEpoch" to lastEntry.epoch,
        "currentEpoch" to currentTime.toEpochMilli()
      )
    )

    if (duration.toMillis() > 0) {
      timeController.advanceTime(duration.toMillis().milliseconds)
    }
    envWithTime.awaitAll()
  }

  suspend fun advanceTime(
    duration: Duration
  ) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "advent.advanceTime",
        "durationMs" to duration.inWholeMilliseconds
      )
    )
    val timeController = envWithTime.timeController
      ?: throw IllegalStateException("TimeController is required for advanceTime. Use withVirtualTime in test setup.")

    timeController.advanceTime(duration)
    envWithTime.awaitAll()
  }

  fun expectQueue(
    description: String = "advent.expectQueue",
    block: AdventQueueExpectation.() -> Unit
  ) {
    val expectation = AdventQueueExpectation().apply(block)
    val desc = "$description(${expectation.expectedUnrevealedCount}:${expectation.expectedRevealedCount})"

    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "advent.expectQueue",
          "expectedUnrevealed" to expectation.expectedUnrevealedCount,
          "expectedRevealed" to expectation.expectedRevealedCount
        )
      )

      val unrevealed = getUnrevealedCount()
      val revealed = getRevealedCount()

      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "adventData.queueCounts",
          "unrevealed" to unrevealed,
          "revealed" to revealed
        )
      )

      expectation.expectedUnrevealedCount?.let { expected ->
        if (unrevealed != expected) {
          throw AssertionError("Expected $expected unrevealed entries, but found $unrevealed")
        }
      }

      expectation.expectedRevealedCount?.let { expected ->
        if (revealed != expected) {
          throw AssertionError("Expected $expected revealed entries, but found $revealed")
        }
      }
    }

    scenarioBuilder?.expect(desc) { assertion() } ?: runBlocking { assertion() }
  }

  fun expectMessagePosted(
    channelId: String,
    textContains: String
  ) {
    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "advent.expectMessagePosted",
          "channelId" to channelId,
          "textContains" to textContains
        )
      )

      val service = answerService ?: throw AssertionError("MockAnswerService is required to assert advent messages")
      val found = service.answers.any { it.channelId == channelId && it.text.contains(textContains) }
      if (!found) {
        throw AssertionError("No advent message in channel $channelId contains '$textContains'")
      }
    }

    scenarioBuilder?.expect("advent.expectMessagePosted") { assertion() } ?: runBlocking { assertion() }
  }

  fun getRevealedCount(): Int {
    val count = adventDataConnector.getAdvents().count { it.isRevealed }
    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "adventData.revealedCount",
        "count" to count
      )
    )
    return count
  }

  fun getUnrevealedCount(): Int {
    val count = adventDataConnector.getAdvents().count { !it.isRevealed }
    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "adventData.unrevealedCount",
        "count" to count
      )
    )
    return count
  }

  fun restartAdventLoop() {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "advent.restartAdventLoop"
      )
    )
    val adventCommand = org.koin.core.context.GlobalContext.get().get<AdventCommand>()
    val readyEvent = mockk<ReadyEvent>()
    every { readyEvent.jda } returns envWithTime.environment.jda
    adventCommand.onEvent(readyEvent)
  }

  private fun resolveUser(idOrName: String): User {
    setupContext.envWithTime.userMap[idOrName]?.let { return it }

    setupContext.envWithTime.environment.jda.users.find { it.id == idOrName || it.name == idOrName }?.let { return it }

    setupContext.envWithTime.environment.jda.guilds
      .flatMap { it.members }
      .map { it.user }
      .firstOrNull { it.id == idOrName || it.name == idOrName }
      ?.let { return it }

    return setupContext.envWithTime.environment.createUser(idOrName, false)
  }
}

class AdventQueueExpectation {
  internal var expectedUnrevealedCount: Int? = null
    private set
  internal var expectedRevealedCount: Int? = null
    private set

  fun unrevealedCount(expected: Int) {
    expectedUnrevealedCount = expected
  }

  fun revealedCount(expected: Int) {
    expectedRevealedCount = expected
  }
}
