package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.ImageMappingConnector
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.service.S3FileResult
import com.fvlaenix.queemporium.service.S3FileService
import com.fvlaenix.queemporium.utils.Logging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SendImageCommand(
  private val answerService: AnswerService,
  private val imageMappingConnector: ImageMappingConnector,
  private val s3FileService: S3FileService,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {

  companion object {
    private val LOG = Logging.getLogger(SendImageCommand::class.java)
    private const val COMMAND_PREFIX = "/shogun-sama image"

    const val ERROR_GUILD_ONLY = "This only applies to servers."
    const val ERROR_USAGE = "Usage: /shogun-sama image <key>"
    const val ERROR_KEY_NOT_FOUND = "No file for key"
    const val ERROR_FILE_NOT_FOUND = "File not found in storage."
    const val ERROR_FILE_TOO_LARGE = "File too large (max 10 MB)."
    const val ERROR_FETCH_FAILED = "Failed to fetch file. Try again later."
  }

  override fun receiveMessageFilter(event: MessageReceivedEvent): Boolean =
    event.message.contentRaw.startsWith(COMMAND_PREFIX)

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (!event.isFromGuild) {
      answerService.sendReply(event.message, ERROR_GUILD_ONLY)
      return
    }

    val key = parseKey(event.message.contentRaw)
    if (key == null) {
      answerService.sendReply(event.message, ERROR_USAGE)
      return
    }

    LOG.info("Processing image request: key=$key")

    val s3Path = imageMappingConnector.get(key)
    if (s3Path == null) {
      LOG.warn("Key not found in database: key=$key")
      answerService.sendReply(event.message, "$ERROR_KEY_NOT_FOUND `$key`.")
      return
    }

    when (val result = s3FileService.fetchFile(s3Path)) {
      is S3FileResult.Success -> {
        LOG.info("Successfully fetched file for key=$key, filename=${result.data.filename}, size=${result.data.bytes.size}")
        answerService.sendFile(
          destination = event.channel,
          filename = result.data.filename,
          bytes = result.data.bytes
        )
      }

      is S3FileResult.NotFound -> {
        LOG.warn("S3 file not found for key=$key, s3Path=$s3Path")
        answerService.sendReply(event.message, ERROR_FILE_NOT_FOUND)
      }

      is S3FileResult.TooLarge -> {
        LOG.warn("S3 file too large for key=$key, s3Path=$s3Path")
        answerService.sendReply(event.message, ERROR_FILE_TOO_LARGE)
      }

      is S3FileResult.Error -> {
        LOG.error("Error fetching file for key=$key, s3Path=$s3Path: ${result.message}")
        answerService.sendReply(event.message, ERROR_FETCH_FAILED)
      }
    }
  }

  private fun parseKey(messageContent: String): String? {
    val parts = messageContent.trim().split(Regex("\\s+"))
    if (parts.size < 3) return null
    return parts[2].takeIf { it.isNotEmpty() }
  }
}
