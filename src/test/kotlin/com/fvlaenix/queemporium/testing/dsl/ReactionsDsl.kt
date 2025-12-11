package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.database.EmojiDataTable
import com.fvlaenix.queemporium.testing.scenario.ScenarioBuilder
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import net.dv8tion.jda.api.entities.Message
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class ReactionsDsl(
  private val setupContext: BotTestSetupContext,
  private val scenarioBuilder: ScenarioBuilder
) {
  fun add(
    guildId: String,
    channelId: String,
    messageIndex: Int,
    emoji: String,
    userId: String
  ) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "reactions.add",
        "guildId" to guildId,
        "channelId" to channelId,
        "messageIndex" to messageIndex,
        "emoji" to emoji,
        "userId" to userId
      )
    )
    scenarioBuilder.addReaction(guildId, channelId, messageIndex, emoji, userId)
  }

  fun add(
    messageId: String,
    emoji: String,
    userId: String
  ) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "reactions.add",
        "messageId" to messageId,
        "emoji" to emoji,
        "userId" to userId
      )
    )
    scenarioBuilder.addReactionById(messageId, emoji, userId)
  }

  fun add(
    message: Message,
    emoji: String,
    userId: String
  ) {
    add(message.id, emoji, userId)
  }

  fun awaitProcessing(description: String = "reactions.awaitProcessing") {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "reactions.awaitProcessing",
        "description" to description
      )
    )
    scenarioBuilder.awaitAll(description)
  }

  fun expectCount(
    message: Message,
    expected: Int
  ) {
    val desc = "reactions.expectCount(${message.id})"
    scenarioBuilder.expect(desc) {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "reactions.expectCount",
          "messageId" to message.id,
          "expected" to expected
        )
      )
      val actual = setupContext.messageEmojiDataConnector.get(message.id)?.count ?: 0
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "messageEmojiData.count",
          "messageId" to message.id,
          "actual" to actual
        )
      )
      if (actual != expected) {
        throw AssertionError("Expected $expected reactions for message ${message.id}, but found $actual")
      }
    }
  }

  fun expectPersisted(
    message: Message,
    block: ReactionPersistedExpectation.() -> Unit
  ) {
    val expectation = ReactionPersistedExpectation().apply(block)
    val desc = "reactions.expectPersisted(${message.id})"
    scenarioBuilder.expect(desc) {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "reactions.expectPersisted",
          "messageId" to message.id,
          "expectedCount" to expectation.expectedCount,
          "expectedEmojis" to expectation.expectedEmojis,
          "expectedUserEmojis" to expectation.expectedUserEmojis
        )
      )

      val actualCount = setupContext.messageEmojiDataConnector.get(message.id)?.count
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "messageEmojiData.count",
          "messageId" to message.id,
          "actual" to actualCount
        )
      )

      expectation.expectedCount?.let { expectedCount ->
        if (actualCount != expectedCount) {
          throw AssertionError("Expected reaction count $expectedCount for message ${message.id}, but found $actualCount")
        }
      }

      expectation.expectedEmojis.forEach { emoji ->
        val hasEmoji = hasEmoji(message.id, emoji)
        ScenarioTraceCollector.logDslDbCheck(
          mapOf(
            "check" to "emojiData.contains",
            "messageId" to message.id,
            "emoji" to emoji,
            "exists" to hasEmoji
          )
        )
        if (!hasEmoji) {
          throw AssertionError("Expected emoji '$emoji' to be persisted for message ${message.id}")
        }
      }

      expectation.expectedUserEmojis.forEach { (userId, emoji) ->
        val hasEmoji = hasEmoji(message.id, emoji, userId)
        ScenarioTraceCollector.logDslDbCheck(
          mapOf(
            "check" to "emojiData.containsForUser",
            "messageId" to message.id,
            "emoji" to emoji,
            "userId" to userId,
            "exists" to hasEmoji
          )
        )
        if (!hasEmoji) {
          throw AssertionError("Expected emoji '$emoji' from user '$userId' to be persisted for message ${message.id}")
        }
      }
    }
  }

  private fun hasEmoji(messageId: String, emoji: String, authorId: String? = null): Boolean {
    val database = setupContext.emojiDataConnector.database
    return transaction(database) {
      val baseCondition = (EmojiDataTable.messageId eq messageId) and (EmojiDataTable.emojiId eq emoji)
      val condition = authorId?.let { baseCondition and (EmojiDataTable.authorId eq it) } ?: baseCondition
      EmojiDataTable.select { condition }.count() > 0
    }
  }
}

class ReactionPersistedExpectation {
  internal var expectedCount: Int? = null
    private set
  internal val expectedEmojis: MutableList<String> = mutableListOf()
  internal val expectedUserEmojis: MutableList<Pair<String, String>> = mutableListOf()

  fun count(expected: Int) {
    expectedCount = expected
  }

  fun contains(emoji: String) {
    expectedEmojis.add(emoji)
  }

  fun containsForUser(userId: String, emoji: String) {
    expectedUserEmojis.add(userId to emoji)
  }
}
