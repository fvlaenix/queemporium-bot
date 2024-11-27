package com.fvlaenix.queemporium.commands.advent

import com.fvlaenix.queemporium.DiscordBot
import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.service.AnswerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class AdventCommand(
  databaseConfiguration: DatabaseConfiguration,
  val answerService: AnswerService,
) : CoroutineListenerAdapter() {
  val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())
  val adventDataConnector = AdventDataConnector(databaseConfiguration.toDatabase())
  val messagesDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())

  var currentJob: Job? = null

  private suspend fun postMessage(jda: JDA, adventData: AdventData) {
    val postChannel = jda.getTextChannelById(adventData.channelPostId)!!

    val messageData = messagesDataConnector.get(adventData.messageId)!!

    val originalMessageChannelId = jda.getTextChannelById(messageData.channelId)!!
    val originalMessage = originalMessageChannelId.retrieveMessageById(messageData.messageId).complete()!!

    answerService.sendMessage(
      destination = postChannel,
      text = adventData.messageDescription
    ).await()
    // todo make normal answerservice to this
    originalMessage.forwardTo(postChannel).queue ({ }, { e ->
      DiscordBot.MAIN_SCOPE.launch(Dispatchers.IO) {
        answerService.sendMessage(postChannel, "I can't send message! Please look at it by link")
      }
    })
  }

  private fun runAdvent(jda: JDA) {
    currentJob?.cancel()
    currentJob = DiscordBot.MAIN_SCOPE.launch(Dispatchers.IO) {
      do {
        val data = adventDataConnector.getAdvents().filter { data -> !data.isRevealed }.sortedBy { it.epoch }
        val currentEpoch = Instant.now().toEpochMilli()
        if (data.isEmpty()) break
        val currentData = data.first()
        if (currentData.epoch < currentEpoch) {
          postMessage(jda, currentData)
          adventDataConnector.markAsRevealed(currentData.guildPostId, currentData.messageId)
        } else {
          delay((currentData.epoch - currentEpoch).toLong())
        }
      } while (true)
    }
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    runAdvent(event.jda)
  }

  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw.startsWith("/shogun-sama start-advent")

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) {
      answerService.sendReply(event.message, "This only applies to servers, stupid!")
      return
    }
    val postMessage = event.message
    val postGuildId = event.message.guildId!!
    val postChannelId = event.message.channelId
    if (!postMessage.isFromAdmin() && !postMessage.isFromRoleAdmin()) {
      answerService.sendReply(postMessage, "Pathetic, only admins can use this!")
      return
    }
    val jda = event.jda

    val (allCommandsEvents, debugData, errors) = getEventsFromCommand(postMessage.contentRaw,
      jda, postGuildId, postChannelId)

    if (errors.isEmpty()) {
      if (debugData.isNotBlank()) {
        answerService.sendReply(postMessage, debugData)
      }
      adventDataConnector.initializeAdvent(allCommandsEvents)
      runAdvent(event.jda)
    } else {
      answerService.sendReply(
        destination = postMessage,
        text = errors.joinToString("\n")
      )
    }
  }

  private fun getEventsFromCommand(
    text: String,
    jda: JDA,
    postGuildId: String,
    postChannelId: String
  ): Triple<List<AdventData>, String, List<String>> {
    val defaultStart = "01-12-2024-00:00"
    val defaultFinish = "01-01-2025-00:00"
    val defaultGuildId = postGuildId
    val defaultCount = 31

    val commands = text.lines()
    // Split the content to extract arguments
    val arguments = commands[0].split(" ").drop(2) // Drop the "/shogun-sama start-advent" part

    val debugMode = arguments.contains("--debug-mode")
    var debugData: String = ""
    val errors = mutableListOf<String>()

    val allCommandsEvents = commands.drop(1).flatMap { fullCommand ->
      val values = mutableMapOf(
        "start" to defaultStart,
        "finish" to defaultFinish,
        "guildId" to defaultGuildId,
        "channelId" to null,
        "channelName" to null,
        "count" to defaultCount.toString()
      )

      fullCommand.split(" ").forEach { argument ->
        when {
          argument.startsWith("start:") -> values["start"] = argument.removePrefix("start:")
          argument.startsWith("finish:") -> values["finish"] = argument.removePrefix("finish:")
          argument.startsWith("guildId:") -> values["guildId"] = argument.removePrefix("guildId:")
          argument.startsWith("count:") -> values["count"] = argument.removePrefix("count:")
          argument.startsWith("channelId:") -> values["channelId"] = argument.removePrefix("channelId:")
          argument.startsWith("channelName:") -> values["channelName"] = argument.removePrefix("channelName:")
        }
      }

      val start = toEpochMillis(parseDateTime(values["start"]!!))
      val finish = toEpochMillis(parseDateTime(values["finish"]!!))
      val guildId = values["guildId"]!!
      val channelId = when {
        (values["channelId"] != null) -> values["channelId"]!!
        (values["channelName"] != null) -> {
          val guild = jda.getGuildById(guildId)
          if (guild == null) {
            errors.add("Guild $guildId not found")
            return@flatMap emptyList()
          }
          val channel = guild.getTextChannelsByName(values["channelName"]!!, true).singleOrNull()
          if (channel == null) {
            errors.add("Channel ${values["channelName"]} not found")
            return@flatMap emptyList()
          }
          channel.id
        }

        else -> null
      }
      val count = values["count"]?.toIntOrNull() ?: defaultCount

      val startOfYear = toEpochMillis(parseDateTime("01-01-2024-00:00"))
      val endOfYear = toEpochMillis(parseDateTime("01-01-2025-00:00"))

      val channel = run {
        if (channelId == null) {
          null
        } else {
          val guild = jda.getGuildById(guildId)
          if (guild == null) {
            errors.add("Guild $guildId not found")
            return@flatMap emptyList()
          }
          guild.getTextChannelById(channelId)
        }
      }

      val messages = emojiDataConnector.getTopMessages(guildId, channelId, startOfYear, endOfYear, count).reversed()
      assert(messages.size == count)

      val interval = (finish - start) / (count - 1)

      // Generate events
      val events = messages.mapIndexed { index, messageTopData ->
        val messageDescription = if (channel == null) {
          "Message number ${count - index} from ${messageTopData.authorName} with ${messageTopData.emojiesCount} reactions: ${messageTopData.url}!"
        } else {
          "Message number ${count - index} in #${channel.name} from ${messageTopData.authorName} with ${messageTopData.emojiesCount} reactions: ${messageTopData.url}!"
        }

        AdventData(
          messageId = messageTopData.messageId,
          messageDescription = messageDescription,
          guildPostId = postGuildId,
          channelPostId = postChannelId,
          epoch = start + index * interval,
          isRevealed = false
        )
      }

      if (debugMode) {
        debugData += "Added $count posts from channel ${channel?.name}\n"
      }

      events
    }
    return Triple(allCommandsEvents, debugData, errors)
  }

  fun parseDateTime(dateTimeString: String): LocalDateTime {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm")
    return LocalDateTime.parse(dateTimeString, formatter)
  }

  fun toEpochMillis(dateTime: LocalDateTime): Long {
    return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
  }
}