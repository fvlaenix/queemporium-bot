package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.service.AnswerService
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

private val COMPRESS_REGEX = "\\d+_p\\d+_master\\d+\\..*".toRegex()

class PixivCompressedDetectorCommand(
  val databaseConfiguration: DatabaseConfiguration,
  val answerService: AnswerService
) : CoroutineListenerAdapter() {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())

  override suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {
    if (event.author.isBot) return
    val attachments = event.message.attachments
    val names = mutableListOf<String>()
    for (attachment in attachments) {
      if (attachment.fileName.matches(COMPRESS_REGEX)) {
        names.add(attachment.fileName)
      }
    }
    if (names.isNotEmpty()) {
      val message = event.message
      val url = message.jumpUrl

      val duplicateChannel = guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) ?: return
      val channel = message.guild.getTextChannelById(duplicateChannel) ?: return
      answerService.sendPixivCompressDetectRequest(channel, message.author.id, url)
    }
  }
}