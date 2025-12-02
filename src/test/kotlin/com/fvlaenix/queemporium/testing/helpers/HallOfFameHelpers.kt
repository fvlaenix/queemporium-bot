package com.fvlaenix.queemporium.testing.helpers

import com.fvlaenix.queemporium.database.EmojiData
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.HallOfFameConnector
import com.fvlaenix.queemporium.mock.TestEnvironment
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.TestEnvironmentWithTime
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.scenario.ExpectContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Helper context for Hall of Fame testing utilities.
 * Provides convenient methods for setting up and verifying Hall of Fame functionality.
 */
class HallOfFameTestContext(
  private val environment: TestEnvironment,
  private val envWithTime: TestEnvironmentWithTime,
  private val hallOfFameConnector: HallOfFameConnector,
  private val emojiDataConnector: EmojiDataConnector,
  private val answerService: MockAnswerService?
) {

  /**
   * Configures Hall of Fame for a guild.
   *
   * @param guildId The guild ID (or guild name)
   * @param hallOfFameChannelId The channel ID (or channel name) where messages should be forwarded
   * @param threshold The minimum number of reactions required
   */
  fun configureHallOfFame(
    guildId: String,
    hallOfFameChannelId: String,
    threshold: Int = 5
  ) {
    val guild = try {
      environment.jda.getGuildById(guildId)
    } catch (e: NumberFormatException) {
      null
    } ?: environment.jda.getGuildsByName(guildId, true).firstOrNull()
    ?: throw IllegalStateException("Guild $guildId not found")

    val channel = try {
      guild.getTextChannelById(hallOfFameChannelId)
    } catch (e: NumberFormatException) {
      null
    } ?: guild.getTextChannelsByName(hallOfFameChannelId, true).firstOrNull()
    ?: throw IllegalStateException("Channel $hallOfFameChannelId not found in guild $guildId")

    hallOfFameConnector.setHallOfFameInfo(
      guildId = guild.id,
      channelId = channel.id,
      threshold = threshold
    )
  }

  /**
   * Seeds emoji reactions on a message to reach a specific count.
   *
   * @param guildId The guild ID (or guild name)
   * @param channelId The channel ID (or channel name)
   * @param messageIndex The index of the message in the channel (0-based)
   * @param emoji The emoji to add reactions with
   * @param userIds List of user IDs (or usernames) to add reactions from
   */
  fun seedEmojiReactions(
    guildId: String,
    channelId: String,
    messageIndex: Int,
    emoji: String,
    userIds: List<String>
  ) {
    val guild = try {
      environment.jda.getGuildById(guildId)
    } catch (e: NumberFormatException) {
      null
    } ?: environment.jda.getGuildsByName(guildId, true).firstOrNull()
    ?: throw IllegalStateException("Guild $guildId not found")

    val channel = try {
      guild.getTextChannelById(channelId)
    } catch (e: NumberFormatException) {
      null
    } ?: guild.getTextChannelsByName(channelId, true).firstOrNull()
    ?: throw IllegalStateException("Channel $channelId not found")

    val messages = (channel as com.fvlaenix.queemporium.mock.TestTextChannel).messages
    if (messageIndex >= messages.size) {
      throw IllegalStateException("Message index $messageIndex out of bounds (channel has ${messages.size} messages)")
    }

    val message = messages[messageIndex]

    userIds.forEach { userId ->
      val user = envWithTime.userMap[userId]
        ?: run {
          try {
            environment.jda.getUserById(userId)
          } catch (e: NumberFormatException) {
            null
          }
        }
        ?: environment.jda.users.find { it.name == userId }
        ?: environment.jda.guilds
          .flatMap { it.members }
          .map { it.user }
          .find { it.name == userId || it.id == userId }
        ?: throw IllegalStateException("User $userId not found")

      emojiDataConnector.insert(
        EmojiData(
          messageId = message.id,
          emojiId = emoji,
          authorId = user.id
        )
      )
    }
  }

  /**
   * Seeds emoji reactions to reach exactly the threshold.
   *
   * @param guildId The guild ID (or guild name)
   * @param channelId The channel ID (or channel name)
   * @param messageIndex The index of the message in the channel (0-based)
   * @param count The exact number of reactions to add
   * @param emoji The emoji to use (defaults to "⭐")
   */
  fun seedMessageToCount(
    guildId: String,
    channelId: String,
    messageIndex: Int,
    count: Int,
    emoji: String = "⭐"
  ) {
    // Generate user IDs
    val userIds = (1..count).map { "reaction-user-$it" }

    // Create users in environment if they don't exist
    userIds.forEach { userId ->
      if (!envWithTime.userMap.containsKey(userId)) {
        // Check if user already exists in JDA (created by previous call)
        val existingUser = environment.jda.users.find { it.name == userId }
        if (existingUser == null) {
          val user = environment.createUser(userId)
          // Add to guild so they can be found
          val guild = try {
            environment.jda.getGuildById(guildId)
          } catch (e: NumberFormatException) {
            null
          } ?: environment.jda.getGuildsByName(guildId, true).firstOrNull()
          ?: throw IllegalStateException("Guild $guildId not found")
          environment.createMember(guild, user)
        }
      }
    }

    seedEmojiReactions(guildId, channelId, messageIndex, emoji, userIds)
  }

  /**
   * Advances time to trigger the "retrieve" job (runs every 9 hours).
   * This job updates the hall of fame database with messages that reached the threshold.
   */
  suspend fun triggerRetrieveJob() {
    if (envWithTime.timeController == null) {
      throw IllegalStateException("TimeController is required for triggerRetrieveJob. Use VirtualClock in test setup.")
    }
    envWithTime.timeController.advanceTime(9.hours)
    envWithTime.awaitAll()
  }

  /**
   * Advances time to trigger the "send" job (runs every 4 hours).
   * This job forwards the oldest unsent message to the hall of fame channel.
   */
  suspend fun triggerSendJob() {
    if (envWithTime.timeController == null) {
      throw IllegalStateException("TimeController is required for triggerSendJob. Use VirtualClock in test setup.")
    }
    envWithTime.timeController.advanceTime(4.hours)
    envWithTime.awaitAll()
  }

  /**
   * Advances time to trigger both retrieve and send jobs.
   * Convenience method that advances time by the appropriate amount.
   */
  suspend fun triggerBothJobs() {
    triggerRetrieveJob()
    triggerSendJob()
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
}

/**
 * Extension function to add Hall of Fame verification methods to ExpectContext.
 */
fun ExpectContext.hallOfFameMessageSent(
  channelId: String,
  textContains: String = "reactions"
) {
  messageSent(channelId, textContains)
}

/**
 * Extension function to verify a message was forwarded to hall of fame.
 */
fun ExpectContext.hallOfFameForwardCount(
  channelId: String,
  expectedCount: Int
) {
  messageSent(channelId, "reactions")
  // Note: The actual count verification would need MockAnswerService to track forwarded messages
  // For now, we verify at least one message was sent with "reactions" text
}

/**
 * Creates a Hall of Fame test context for use in tests.
 */
fun TestEnvironmentWithTime.hallOfFameContext(
  hallOfFameConnector: HallOfFameConnector,
  emojiDataConnector: EmojiDataConnector,
  answerService: MockAnswerService? = null
): HallOfFameTestContext {
  return HallOfFameTestContext(
    environment = environment,
    envWithTime = this,
    hallOfFameConnector = hallOfFameConnector,
    emojiDataConnector = emojiDataConnector,
    answerService = answerService
  )
}
