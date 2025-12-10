package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.mock.TestTextChannel
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

enum class MessageOrder {
  OLDEST_FIRST,
  NEWEST_FIRST
}

object GuildResolver {
  fun resolve(jda: JDA, nameOrId: String): Guild {
    // 1. Try exact ID
    if (nameOrId.matches(Regex("\\d+"))) {
      val guild = jda.getGuildById(nameOrId)
      if (guild != null) {
        logDslLookup("Guild", "ID=$nameOrId", guild.id)
        return guild
      }
    }

    // 2. Try exact name (case-sensitive)
    val exactMatches = jda.getGuildsByName(nameOrId, false)
    if (exactMatches.size == 1) {
      val guild = exactMatches.first()
      logDslLookup("Guild", "Name(Exact)=$nameOrId", guild.id)
      return guild
    } else if (exactMatches.size > 1) {
      throw ambiguousError("Guild", nameOrId, exactMatches.map { "${it.name}(${it.id})" })
    }

    // 3. Try case-insensitive name
    val looseMatches = jda.getGuildsByName(nameOrId, true)
    if (looseMatches.size == 1) {
      val guild = looseMatches.first()
      logDslLookup("Guild", "Name(Loose)=$nameOrId", guild.id)
      return guild
    } else if (looseMatches.size > 1) {
      throw ambiguousError("Guild", nameOrId, looseMatches.map { "${it.name}(${it.id})" })
    }

    // 4. No match
    val available = jda.guilds.joinToString(", ") { "${it.name}(${it.id})" }
    throw IllegalStateException("Guild '$nameOrId' not found. Available: [$available]")
  }
}

object ChannelResolver {
  fun resolve(guild: Guild, nameOrId: String): TextChannel {
    // 1. Try exact ID
    if (nameOrId.matches(Regex("\\d+"))) {
      val channel = guild.getTextChannelById(nameOrId)
      if (channel != null) {
        logDslLookup("TextChannel", "ID=$nameOrId", channel.id)
        return channel
      }
    }

    // 2. Try exact name (case-sensitive)
    val exactMatches = guild.getTextChannelsByName(nameOrId, false)
    if (exactMatches.size == 1) {
      val channel = exactMatches.first()
      logDslLookup("TextChannel", "Name(Exact)=$nameOrId", channel.id)
      return channel
    } else if (exactMatches.size > 1) {
      throw ambiguousError("TextChannel", nameOrId, exactMatches.map { "${it.name}(${it.id})" })
    }

    // 3. Try case-insensitive name
    val looseMatches = guild.getTextChannelsByName(nameOrId, true)
    if (looseMatches.size == 1) {
      val channel = looseMatches.first()
      logDslLookup("TextChannel", "Name(Loose)=$nameOrId", channel.id)
      return channel
    } else if (looseMatches.size > 1) {
      throw ambiguousError("TextChannel", nameOrId, looseMatches.map { "${it.name}(${it.id})" })
    }

    // 4. No match
    val available = guild.textChannels.joinToString(", ") { "${it.name}(${it.id})" }
    throw IllegalStateException("TextChannel '$nameOrId' not found in guild '${guild.name}'. Available: [$available]")
  }
}

object MessageResolver {
  fun resolve(channel: MessageChannel, index: Int, order: MessageOrder): Message {
    if (channel !is TestTextChannel) {
      throw UnsupportedOperationException("Message resolution by index is only supported for TestTextChannel. Got: ${channel::class.simpleName}")
    }

    val messages = channel.messages
    if (messages.isEmpty()) {
      throw IllegalStateException("Channel '${channel.name}' has no messages.")
    }

    val sortedMessages = when (order) {
      MessageOrder.OLDEST_FIRST -> messages // Assuming insertion order is oldest first
      MessageOrder.NEWEST_FIRST -> messages.reversed()
    }

    if (index < 0 || index >= sortedMessages.size) {
      throw IllegalStateException("Message index $index out of bounds (size: ${sortedMessages.size}) for channel '${channel.name}'.")
    }

    val message = sortedMessages[index]
    logDslLookup("Message", "Channel=${channel.name}, Index=$index, Order=$order", message.id)
    return message
  }

  fun resolveById(channel: MessageChannel, messageId: String): Message {
    try {
      // blocking call is acceptable in tests
      val message = channel.retrieveMessageById(messageId).complete()
      logDslLookup("Message", "ID=$messageId", message.id)
      return message
    } catch (e: Exception) {
      throw IllegalStateException("Message '$messageId' not found in channel '${channel.name}'", e)
    }
  }
}

private fun logDslLookup(type: String, criteria: String, resultId: String) {
  ScenarioTraceCollector.logCustom(
    "DSL_LOOKUP", mapOf(
      "type" to type,
      "criteria" to criteria,
      "resultId" to resultId
    )
  )
}

private fun ambiguousError(entity: String, criteria: String, matches: List<String>): IllegalStateException {
  return IllegalStateException("Ambiguous $entity '$criteria'. Matches: $matches")
}
