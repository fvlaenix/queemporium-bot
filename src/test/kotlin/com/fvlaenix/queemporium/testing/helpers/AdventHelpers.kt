package com.fvlaenix.queemporium.testing.helpers

import com.fvlaenix.queemporium.commands.advent.AdventData
import com.fvlaenix.queemporium.commands.advent.AdventDataConnector
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.TestEnvironmentWithTime
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.scenario.ExpectContext
import java.time.Instant
import kotlin.time.Duration

/**
 * Helper context for Advent testing utilities.
 * Provides convenient methods for setting up and verifying Advent calendar functionality.
 */
class AdventTestContext(
  private val environment: TestEnvironment,
  private val envWithTime: TestEnvironmentWithTime,
  private val adventDataConnector: AdventDataConnector,
  private val answerService: MockAnswerService?
) {

  /**
   * Schedules Advent entries for reveal at specific times.
   * This replaces any existing entries for the guild.
   *
   * @param guildId The guild ID where entries will be posted
   * @param postChannelId The channel ID where entries will be revealed
   * @param entries List of (messageId, description, revealTime) tuples
   */
  fun scheduleEntries(
    guildId: String,
    postChannelId: String,
    entries: List<Triple<String, String, Instant>>
  ) {
    val guild = environment.jda.getGuildsByName(guildId, true).firstOrNull()
      ?: throw IllegalStateException("Guild $guildId not found")

    val postChannel = guild.getTextChannelsByName(postChannelId, true).firstOrNull()
      ?: throw IllegalStateException("Channel $postChannelId not found in guild $guildId")

    val adventDataList = entries.map { (messageId, description, revealTime) ->
      AdventData(
        messageId = messageId,
        messageDescription = description,
        guildPostId = guild.id,
        channelPostId = postChannel.id,
        epoch = revealTime.toEpochMilli(),
        isRevealed = false
      )
    }

    adventDataConnector.initializeAdvent(adventDataList)
  }

  /**
   * Schedules multiple Advent entries with evenly distributed reveal times.
   *
   * @param guildId The guild ID where entries will be posted
   * @param postChannelId The channel ID where entries will be revealed
   * @param entries List of (messageId, description) pairs
   * @param startTime The time of the first reveal
   * @param interval The duration between each reveal
   */
  fun scheduleMultipleEntries(
    guildId: String,
    postChannelId: String,
    entries: List<Pair<String, String>>,
    startTime: Instant,
    interval: Duration
  ) {
    val triples = entries.mapIndexed { index, (messageId, description) ->
      val revealTime = startTime.plusMillis(interval.inWholeMilliseconds * index)
      Triple(messageId, description, revealTime)
    }
    scheduleEntries(guildId, postChannelId, triples)
  }

  /**
   * Advances time to the next scheduled Advent reveal.
   * This will trigger the Advent job to post the next entry.
   */
  suspend fun advanceToNextReveal() {
    if (envWithTime.timeController == null) {
      throw IllegalStateException("TimeController is required for advanceToNextReveal. Use VirtualClock in test setup.")
    }

    val nextEntry = adventDataConnector.getAdvents()
      .filter { !it.isRevealed }
      .minByOrNull { it.epoch }
      ?: throw IllegalStateException("No unrevealed Advent entries found")

    val currentTime = envWithTime.timeController.getCurrentTime()
    val nextRevealTime = Instant.ofEpochMilli(nextEntry.epoch)

    if (nextRevealTime.isAfter(currentTime)) {
      val duration = java.time.Duration.between(currentTime, nextRevealTime)
      envWithTime.timeController.advanceTime(kotlin.time.Duration.parse("${duration.toMillis()}ms"))
      envWithTime.awaitAll()
    }
  }

  /**
   * Advances time to reveal all remaining entries.
   */
  suspend fun revealAllEntries() {
    if (envWithTime.timeController == null) {
      throw IllegalStateException("TimeController is required for revealAllEntries. Use VirtualClock in test setup.")
    }

    val lastEntry = adventDataConnector.getAdvents()
      .filter { !it.isRevealed }
      .maxByOrNull { it.epoch }
      ?: return // No unrevealed entries

    val currentTime = envWithTime.timeController.getCurrentTime()
    val lastRevealTime = Instant.ofEpochMilli(lastEntry.epoch)

    if (lastRevealTime.isAfter(currentTime)) {
      val duration = java.time.Duration.between(currentTime, lastRevealTime)
      envWithTime.timeController.advanceTime(kotlin.time.Duration.parse("${duration.toMillis()}ms"))
      envWithTime.awaitAll()
    }
  }

  /**
   * Advances time by a custom duration.
   */
  suspend fun advanceTime(duration: Duration) {
    if (envWithTime.timeController == null) {
      throw IllegalStateException("TimeController is required for advanceTime. Use VirtualClock in test setup.")
    }
    envWithTime.timeController.advanceTime(duration)
    envWithTime.awaitAll()
  }

  /**
   * Gets the count of revealed entries.
   */
  fun getRevealedCount(): Int {
    return adventDataConnector.getAdvents().count { it.isRevealed }
  }

  /**
   * Gets the count of unrevealed entries.
   */
  fun getUnrevealedCount(): Int {
    return adventDataConnector.getAdvents().count { !it.isRevealed }
  }
}

/**
 * Extension function to verify an Advent message was revealed.
 */
fun ExpectContext.adventMessageRevealed(
  channelId: String,
  descriptionContains: String
) {
  messageSent(channelId, descriptionContains)
}

/**
 * Extension function to verify multiple Advent messages were revealed.
 */
fun ExpectContext.adventMessagesRevealed(
  channelId: String,
  count: Int
) {
  messageSentCount(count * 2) // Each entry sends description + forward
}

/**
 * Creates an Advent test context for use in tests.
 */
fun TestEnvironmentWithTime.adventContext(
  adventDataConnector: AdventDataConnector,
  answerService: MockAnswerService? = null
): AdventTestContext {
  return AdventTestContext(
    environment = environment,
    envWithTime = this,
    adventDataConnector = adventDataConnector,
    answerService = answerService
  )
}
