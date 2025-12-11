package com.fvlaenix.queemporium.commands.advent

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.EmojiDataConnector
import com.fvlaenix.queemporium.database.MessageDataConnector
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.milliseconds

private val LOG = Logging.getLogger(AdventCommand::class.java)

class AdventCommand(
  databaseConfiguration: DatabaseConfiguration,
  val answerService: AnswerService,
  coroutineProvider: BotCoroutineProvider,
  private val clock: java.time.Clock = java.time.Clock.systemUTC()
) : CoroutineListenerAdapter(coroutineProvider) {
  val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())
  val adventDataConnector = AdventDataConnector(databaseConfiguration.toDatabase())
  val messagesDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())

  var currentJob: Job? = null

  companion object {
    const val COMMAND_PREFIX = "/shogun-sama start-advent"
    const val COMMAND_ADVENT_PREFIX = "/shogun-sama advent"
    const val COMMAND_POST_RIGHT_NOW = "/shogun-sama advent post-right-now"
    const val COMMAND_LIST = "/shogun-sama advent list"
    const val DEFAULT_COUNT = 31
    const val MIN_COUNT = 2

    private const val DEFAULT_START_MONTH_DAY = "01-12"
    private const val DEFAULT_FINISH_MONTH_DAY = "01-01"
    private const val DEFAULT_START_TIME = "00:00"
    private const val DEFAULT_FINISH_TIME = "00:00"

    private const val MESSAGE_GUILD_ONLY = "This only applies to servers, stupid!"
    private const val MESSAGE_ADMIN_ONLY = "Pathetic, only admins can use this!"
    private const val MESSAGE_CANT_SEND = "I can't send message! Please look at it by link"
    private const val MESSAGE_ADVENT_NOT_CONFIGURED = "Advent is not configured for this server."
    private const val MESSAGE_NO_UNREVEALED_ENTRIES = "No unrevealed advent entries left."

    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm")
  }

  private suspend fun postMessage(jda: JDA, adventData: AdventData) {
    val postGuild = jda.getGuildById(adventData.guildPostId) ?: return
    val postChannel = postGuild.getTextChannelById(adventData.channelPostId) ?: return

    val messageData = messagesDataConnector.get(adventData.messageId) ?: run {
      answerService.sendMessage(postChannel, MESSAGE_CANT_SEND)
      return
    }

    val originalGuild = messageData.guildId?.let { jda.getGuildById(it) }
    val originalMessageChannel = originalGuild?.getTextChannelById(messageData.channelId)
      ?: jda.getTextChannelById(messageData.channelId) ?: run {
        answerService.sendMessage(postChannel, MESSAGE_CANT_SEND)
        return
      }

    val originalMessage = try {
      originalMessageChannel.retrieveMessageById(messageData.messageId).complete()
    } catch (e: Exception) {
      answerService.sendMessage(postChannel, MESSAGE_CANT_SEND)
      return
    }

    answerService.sendMessage(
      destination = postChannel,
      text = adventData.messageDescription
    ).await()

    val messageId = answerService.forwardMessage(originalMessage, postChannel).await()
    if (messageId == null) {
      answerService.sendMessage(postChannel, MESSAGE_CANT_SEND)
    }
  }

  private fun rescheduleRemaining(nowMillis: Long, remaining: List<AdventData>) {
    if (remaining.isEmpty()) return

    val sorted = remaining.sortedBy { it.epoch }
    val newStart = nowMillis
    val originalFinalEpoch = sorted.last().epoch
    val finalEpoch = originalFinalEpoch.coerceAtLeast(newStart)

    if (sorted.size == 1) {
      adventDataConnector.updateEpoch(sorted[0].guildPostId, sorted[0].messageId, newStart)
      LOG.info("Rescheduled single remaining entry to epoch $newStart")
      return
    }

    val divisor = sorted.size - 1
    val interval = if (finalEpoch <= newStart) 0L else (finalEpoch - newStart) / divisor

    sorted.forEachIndexed { index, data ->
      val newEpoch = if (interval == 0L) newStart else newStart + interval * index
      adventDataConnector.updateEpoch(data.guildPostId, data.messageId, newEpoch)
      LOG.info("Rescheduled entry ${data.messageId} from epoch ${data.epoch} to $newEpoch")
    }

    LOG.info("Rescheduled ${sorted.size} entries with interval $interval ms")
  }

  private suspend fun handlePostRightNow(event: MessageReceivedEvent) {
    val guildId = event.guild.id
    val jda = event.jda

    LOG.info("Handling post-right-now command for guild $guildId by user ${event.author.name}")

    val allAdvents = adventDataConnector.getAdvents().filter { it.guildPostId == guildId }
    if (allAdvents.isEmpty()) {
      LOG.warn("No advent configured for guild $guildId")
      answerService.sendReply(event.message, MESSAGE_ADVENT_NOT_CONFIGURED)
      return
    }

    val unrevealed = allAdvents.filter { !it.isRevealed }.sortedBy { it.epoch }
    if (unrevealed.isEmpty()) {
      LOG.warn("No unrevealed entries for guild $guildId")
      answerService.sendReply(event.message, MESSAGE_NO_UNREVEALED_ENTRIES)
      return
    }

    val toPost = unrevealed.first()
    val originalPosition = allAdvents.sortedBy { it.epoch }.indexOf(toPost) + 1

    LOG.info("Posting entry #$originalPosition (messageId=${toPost.messageId}) immediately")

    postMessage(jda, toPost)
    adventDataConnector.markAsRevealed(toPost.guildPostId, toPost.messageId)

    val nowMillis = Instant.now(clock).toEpochMilli()
    val remaining = adventDataConnector.getAdvents()
      .filter { it.guildPostId == guildId && !it.isRevealed }

    rescheduleRemaining(nowMillis, remaining)

    runAdvent(jda)

    val responseText = buildString {
      append("Posted Advent entry #$originalPosition.")
      if (remaining.isNotEmpty()) {
        val nextEntry = remaining.sortedBy { it.epoch }.first()
        val nextTime = Instant.ofEpochMilli(nextEntry.epoch)
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
          .withZone(ZoneOffset.UTC)
        append(" Next entry at ${formatter.format(nextTime)} UTC.")
      }
    }

    LOG.info("Post-right-now completed for guild $guildId: $responseText")
    answerService.sendReply(event.message, responseText)
  }

  private suspend fun handleList(event: MessageReceivedEvent) {
    val guildId = event.guild.id

    LOG.info("Handling list command for guild $guildId by user ${event.author.name}")

    val allAdvents = adventDataConnector.getAdvents().filter { it.guildPostId == guildId }
    if (allAdvents.isEmpty()) {
      LOG.warn("No advent configured for guild $guildId")
      answerService.sendReply(event.message, MESSAGE_ADVENT_NOT_CONFIGURED)
      return
    }

    val unrevealed = allAdvents.filter { !it.isRevealed }.sortedBy { it.epoch }
    if (unrevealed.isEmpty()) {
      LOG.warn("No unrevealed entries for guild $guildId")
      answerService.sendReply(event.message, MESSAGE_NO_UNREVEALED_ENTRIES)
      return
    }

    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
      .withZone(ZoneOffset.UTC)

    val responseText = buildString {
      appendLine("Advent queue (UTC):")
      unrevealed.forEachIndexed { index, data ->
        val position = index + 1
        val time = Instant.ofEpochMilli(data.epoch)
        val formattedTime = formatter.format(time)
        val url = messagesDataConnector.get(data.messageId)?.let { messageData ->
          "https://discord.com/channels/${messageData.guildId ?: "@me"}/${messageData.channelId}/${messageData.messageId}"
        } ?: "URL unavailable"
        appendLine("#$position – $formattedTime – $url")
      }
    }

    LOG.info("List command completed for guild $guildId: ${unrevealed.size} unrevealed entries")
    answerService.sendReply(event.message, responseText.trim())
  }

  private fun runAdvent(jda: JDA) {
    currentJob?.cancel()
    currentJob = coroutineProvider.mainScope.launch(Dispatchers.IO) {
      do {
        val data = adventDataConnector.getAdvents().filter { data -> !data.isRevealed }.sortedBy { it.epoch }
        val currentEpoch = Instant.now(clock).toEpochMilli()
        if (data.isEmpty()) break
        val currentData = data.first()
        if (currentData.epoch <= currentEpoch) {
          postMessage(jda, currentData)
          adventDataConnector.markAsRevealed(currentData.guildPostId, currentData.messageId)
        } else {
          coroutineProvider.safeDelay((currentData.epoch - currentEpoch).milliseconds)
        }
      } while (true)
    }
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    runAdvent(event.jda)
  }

  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean {
    val content = event.message.contentRaw
    return content.startsWith(COMMAND_PREFIX) || content.startsWith(COMMAND_ADVENT_PREFIX)
  }

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    LOG.info("Received advent command from user ${event.author.name} in guild ${event.guild?.name}")

    if (!event.isFromGuild) {
      LOG.warn("Command received outside of guild context")
      answerService.sendReply(event.message, MESSAGE_GUILD_ONLY)
      return
    }
    val postMessage = event.message
    val postGuildId = event.message.guildId!!
    val postChannelId = event.message.channelId
    if (!postMessage.isFromAdmin() && !postMessage.isFromRoleAdmin()) {
      LOG.warn("Command received from non-admin user ${event.author.name}")
      answerService.sendReply(postMessage, MESSAGE_ADMIN_ONLY)
      return
    }

    val commandContent = postMessage.contentRaw

    when {
      commandContent.startsWith(COMMAND_POST_RIGHT_NOW) -> {
        handlePostRightNow(event)
      }

      commandContent.startsWith(COMMAND_LIST) -> {
        handleList(event)
      }

      commandContent.startsWith(COMMAND_PREFIX) -> {
        val jda = event.jda

        LOG.info("Parsing advent command for guild $postGuildId, channel $postChannelId")
        val (allCommandsEvents, debugData, errors) = getEventsFromCommand(
          postMessage.contentRaw,
          jda, postGuildId, postChannelId
        )

        if (errors.isEmpty()) {
          LOG.info("Successfully parsed ${allCommandsEvents.size} advent entries")
          if (debugData.isNotBlank()) {
            answerService.sendReply(postMessage, debugData)
          }
          adventDataConnector.initializeAdvent(allCommandsEvents)
          runAdvent(event.jda)
        } else {
          LOG.warn("Command parsing failed with ${errors.size} errors")
          answerService.sendReply(
            destination = postMessage,
            text = errors.joinToString("\n")
          )
        }
      }

      else -> {
        LOG.warn("Unknown advent command: $commandContent")
        answerService.sendReply(
          postMessage,
          "Unknown advent command. Available commands: start-advent, advent post-right-now, advent list"
        )
      }
    }
  }

  private data class AdventParameters(
    val start: String,
    val finish: String,
    val guildId: String,
    val channelId: String?,
    val channelName: String?,
    val count: Int,
    val year: Int
  )

  private fun parseCommandArguments(
    commandLine: String,
    postGuildId: String,
    currentYear: Int
  ): Pair<AdventParameters, List<String>> {
    val errors = mutableListOf<String>()
    // Only drop first 2 elements if line starts with command prefix
    val parts = commandLine.split(" ")
    val arguments = if (parts.size >= 2 && parts[0] == "/shogun-sama" && parts[1] == "start-advent") {
      parts.drop(2)
    } else {
      parts
    }

    val defaultFinishYear = currentYear + 1
    val defaultStart = "$DEFAULT_START_MONTH_DAY-$currentYear-$DEFAULT_START_TIME"
    val defaultFinish = "$DEFAULT_FINISH_MONTH_DAY-$defaultFinishYear-$DEFAULT_FINISH_TIME"

    val values = mutableMapOf(
      "start" to defaultStart,
      "finish" to defaultFinish,
      "guildId" to postGuildId,
      "channelId" to null as String?,
      "channelName" to null as String?,
      "count" to DEFAULT_COUNT.toString(),
      "year" to currentYear.toString()
    )

    arguments.forEach { argument ->
      when {
        argument.startsWith("start:") -> values["start"] = argument.removePrefix("start:")
        argument.startsWith("finish:") -> values["finish"] = argument.removePrefix("finish:")
        argument.startsWith("guildId:") -> values["guildId"] = argument.removePrefix("guildId:")
        argument.startsWith("count:") -> values["count"] = argument.removePrefix("count:")
        argument.startsWith("channelId:") -> values["channelId"] = argument.removePrefix("channelId:")
        argument.startsWith("channelName:") -> values["channelName"] = argument.removePrefix("channelName:")
        argument.startsWith("year:") -> values["year"] = argument.removePrefix("year:")
      }
    }

    val count = values["count"]?.toIntOrNull() ?: DEFAULT_COUNT
    if (count < MIN_COUNT) {
      errors.add("Count must be at least $MIN_COUNT (got $count)")
    }

    val year = values["year"]?.toIntOrNull() ?: currentYear
    if (year < 1970 || year > 2100) {
      errors.add("Year must be between 1970 and 2100 (got $year)")
    }

    val params = AdventParameters(
      start = values["start"]!!,
      finish = values["finish"]!!,
      guildId = values["guildId"]!!,
      channelId = values["channelId"],
      channelName = values["channelName"],
      count = count,
      year = year
    )

    return params to errors
  }

  private fun resolveChannel(
    jda: JDA,
    guildId: String,
    channelId: String?,
    channelName: String?,
    errors: MutableList<String>
  ): TextChannel? {
    if (channelId == null && channelName == null) {
      return null
    }

    val guild = jda.getGuildById(guildId)
    if (guild == null) {
      errors.add("Guild $guildId not found")
      return null
    }

    return when {
      channelId != null -> {
        val channel = guild.getTextChannelById(channelId)
        if (channel == null) {
          errors.add("Channel $channelId not found in guild ${guild.name}")
        }
        channel
      }

      channelName != null -> {
        val channels = guild.getTextChannelsByName(channelName, true)
        when {
          channels.isEmpty() -> {
            errors.add("Channel '$channelName' not found in guild ${guild.name}")
            null
          }

          channels.size > 1 -> {
            errors.add("Multiple channels named '$channelName' found in guild ${guild.name}. Use channelId instead.")
            null
          }

          else -> channels.first()
        }
      }

      else -> null
    }
  }

  private fun getEventsFromCommand(
    text: String,
    jda: JDA,
    postGuildId: String,
    postChannelId: String
  ): Triple<List<AdventData>, String, List<String>> {
    val currentYear = LocalDateTime.now(clock).year
    val commands = text.lines()
    val arguments = commands[0].split(" ").drop(2)

    val debugMode = arguments.contains("--debug-mode")
    var debugData: String = ""
    val errors = mutableListOf<String>()

    LOG.info("Commands after split: ${commands.size} lines, dropped first: ${commands.drop(1).size} lines")
    val allCommandsEvents = commands.drop(1).flatMap { fullCommand ->
      LOG.info("Processing command line: $fullCommand")
      val (params, parseErrors) = parseCommandArguments(fullCommand, postGuildId, currentYear)
      LOG.info("Parse result: params=$params, parseErrors=$parseErrors")
      errors.addAll(parseErrors)
      if (parseErrors.isNotEmpty()) {
        LOG.warn("Parse errors, returning empty")
        return@flatMap emptyList()
      }

      val startDateTime = try {
        parseDateTime(params.start)
      } catch (e: DateTimeParseException) {
        errors.add("Invalid start date format: ${params.start}. Expected format: dd-MM-yyyy-HH:mm")
        return@flatMap emptyList()
      }

      val finishDateTime = try {
        parseDateTime(params.finish)
      } catch (e: DateTimeParseException) {
        errors.add("Invalid finish date format: ${params.finish}. Expected format: dd-MM-yyyy-HH:mm")
        return@flatMap emptyList()
      }

      val start = toEpochMillis(startDateTime)
      val finish = toEpochMillis(finishDateTime)

      if (start >= finish) {
        errors.add("Start time must be before finish time")
        return@flatMap emptyList()
      }

      LOG.info("Resolving channel: guildId=${params.guildId}, channelId=${params.channelId}, channelName=${params.channelName}")
      val channel = resolveChannel(jda, params.guildId, params.channelId, params.channelName, errors)
      LOG.info("Channel resolution result: channel=${channel?.name}, errors=$errors")
      if (errors.isNotEmpty()) {
        LOG.warn("Errors during channel resolution: $errors")
        return@flatMap emptyList()
      }

      val startOfYear = toEpochMillis(parseDateTime("01-12-${params.year - 1}-00:00"))
      val endOfYear = toEpochMillis(parseDateTime("01-01-${params.year + 1}-00:00"))

      // Use the resolved channel ID if a channel was resolved, otherwise use the params channelId (which may be null for all-channels query)
      val queryChannelId = channel?.id ?: params.channelId

      LOG.info("Querying top messages: guildId=${params.guildId}, channelId=$queryChannelId, startOfYear=$startOfYear, endOfYear=$endOfYear, count=${params.count}")

      val messages =
        emojiDataConnector.getTopMessages(params.guildId, queryChannelId, startOfYear, endOfYear, params.count)
          .reversed()

      LOG.info("Query returned ${messages.size} messages")

      if (messages.size != params.count) {
        errors.add("Requested ${params.count} messages but only found ${messages.size} messages with reactions")
        return@flatMap emptyList()
      }

      val interval = (finish - start) / (params.count - 1)

      val events = messages.mapIndexed { index, messageTopData ->
        val messageDescription = if (channel == null) {
          "Message number ${params.count - index} from ${messageTopData.authorName} with ${messageTopData.emojiesCount} reactions: ${messageTopData.url}!"
        } else {
          "Message number ${params.count - index} in #${channel.name} from ${messageTopData.authorName} with ${messageTopData.emojiesCount} reactions: ${messageTopData.url}!"
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
        debugData += "Added ${params.count} posts from channel ${channel?.name ?: "all channels"}\n"
      }

      events
    }
    return Triple(allCommandsEvents, debugData, errors)
  }

  fun parseDateTime(dateTimeString: String): LocalDateTime {
    return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER)
  }

  fun toEpochMillis(dateTime: LocalDateTime): Long {
    return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
  }
}