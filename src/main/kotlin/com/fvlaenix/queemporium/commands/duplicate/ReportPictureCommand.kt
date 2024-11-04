package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.utils.CoroutineUtils
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import kotlin.coroutines.coroutineContext

abstract class ReportPictureCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService
) : CoroutineListenerAdapter() {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())
  private val dependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  
  suspend fun getMessage(compressSize: CompressSize, message: Message) {
    if (message.author.isSystem) return
    val duplicateChannelId = guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) ?: return
    val duplicateChannel = message.guild.getTextChannelById(duplicateChannelId) ?: return

    val messageId = message.id
    val messageData = MessageData(
      messageId = messageId,
      guildId = message.guildId,
      channelId = message.channelId,
      text = message.contentRaw,
      url = message.jumpUrl,
      authorId = message.author.id,
      epoch = message.timeCreated.toEpochSecond(),
    )
    val messageDuplicateData = MessageDuplicateData(
      messageId = messageId,
      hasSource = message.contentRaw.contains("http"),
      countImages = message.attachments.size + message.embeds.size,
      messageProblems = emptyList()
    )
    val fullData = messageDuplicateData.withMessageData(messageData)

    messageDataConnector.add(messageData)
    if (messageDuplicateDataConnector.exists(messageData.messageId)) return
    
    withContext(coroutineContext + CoroutineUtils.CurrentMessageMessageProblemHandler()) {
      assert(coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY] != null)
      
      DuplicateImageService.sendPictures(
        message = message,
        compressSize = compressSize,
        withHistoryReload = true
      ) { (duplicateMessageInfo, originalImageDatas) ->
        val isSpoiler =
          duplicateMessageInfo.additionalImageInfo.isSpoiler || originalImageDatas.any { it.additionalImageInfo.isSpoiler }
        val originalData = originalImageDatas.map {
          messageDuplicateDataConnector.get(it.messageId)!!.withMessageData(messageDataConnector.get(it.messageId)!!) to it
        }
        val duplicateMessageDatas = answerService.sendDuplicateMessageInfo(
          duplicateChannel = duplicateChannel,
          messageAuthorId = message.author.id,
          fileName = duplicateMessageInfo.additionalImageInfo.fileName,
          image = duplicateMessageInfo.bufferedImage,
          messageData = fullData,
          additionalImageInfo = duplicateMessageInfo.additionalImageInfo,
          isSpoiler = isSpoiler,
          originalData = originalData
        )
        duplicateMessageDatas.forEach { duplicateMessageData ->
          val dependentMessage = duplicateMessageData.await() ?: return@forEach
          dependencyConnector.addDependency(
            MessageDependency(
              targetMessage = messageData.messageId,
              dependentMessage = dependentMessage
            )
          )
          originalImageDatas.forEach { originalImageData ->
            dependencyConnector.addDependency(
              MessageDependency(
                targetMessage = originalImageData.messageId,
                dependentMessage = dependentMessage
              )
            )
          }
        }
      }

      val messageProblemsHandler = coroutineContext[CoroutineUtils.CURRENT_MESSAGE_EXCEPTION_CONTEXT_KEY]!!
      messageDuplicateDataConnector.add(messageDuplicateData.copy(messageProblems = messageProblemsHandler.messageProblems))
    }
  }
  
  suspend fun runOverOld(jda: JDA, takeWhile: (Message) -> Boolean, computeMessage: suspend (Message) -> Unit) {
    val computeGuild: (Guild) -> List<MessageChannel> = { guild ->
      val guildId = guild.id
      guild.channels.mapNotNull channel@{ channel ->
        val channelId = channel.id
        if (channel is MessageChannel) {
          if (guildInfoConnector.isChannelExclude(guildId, channelId)) return@channel null
          if (guildInfoConnector.getDuplicateInfoChannel(guildId) == channelId) return@channel null
          channel
        } else {
          null
        }
      }
    }
    
    runOverOld(
      jda = jda, 
      jobName = "DuplicatePicture",
      computeGuild = computeGuild,
      takeWhile = takeWhile,
      computeMessage = computeMessage
    )
  }
}