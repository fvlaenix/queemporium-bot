package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.utils.AnswerUtils
import com.fvlaenix.queemporium.utils.CoroutineUtils
import com.fvlaenix.queemporium.utils.CoroutineUtils.channelTransform
import com.fvlaenix.queemporium.utils.CoroutineUtils.flatChannelTransform
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.coroutineContext

private val LOG = Logger.getLogger(ReportPictureCommand::class.java.name)

abstract class ReportPictureCommand(databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())
  private val dependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  
  suspend fun getMessage(compressSize: CompressSize, message: Message) {
    if (message.author.isSystem) return
    val duplicateChannelId = guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) ?: return
    val duplicateChannel = message.guild.getTextChannelById(duplicateChannelId) ?: return

    val messageId = MessageId(
      guildId = message.guildId!!,
      channelId = message.channel.id,
      messageId = message.id
    )
    val messageData = MessageData(
      messageId = messageId,
      text = message.contentRaw,
      url = message.jumpUrl,
      author = message.author.id,
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
    if (messageDuplicateDataConnector.get(messageData.messageId) != null) return
    
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
          messageDuplicateDataConnector.get(it.imageId.toMessageId())!!.withMessageData(messageDataConnector.get(it.imageId.toMessageId())!!) to it
        }
        val duplicateMessageDatas = AnswerUtils.sendDuplicateMessageInfo(
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
          val dependentMessage = duplicateMessageData.get() ?: return@forEach
          dependencyConnector.addDependency(
            MessageDependency(
              targetMessage = messageData.messageId,
              dependentMessage = dependentMessage
            )
          )
          originalImageDatas.forEach { originalImageData ->
            dependencyConnector.addDependency(
              MessageDependency(
                targetMessage = originalImageData.imageId.toMessageId(),
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
    val guildCounter = CoroutineUtils.AtomicProgressCounter()
    val channelCounter = CoroutineUtils.AtomicProgressCounter()
    val messageCounter = CoroutineUtils.AtomicProgressCounter()
    coroutineScope {
      guildCounter.totalIncrease(jda.guilds.size)
      val channelsChannel = flatChannelTransform(jda.guilds, 2) { guild ->
        val guildId = guild.id
        LOG.log(Level.INFO, "Guild processing ${guildCounter.status()}: ${guild.name}")
        val channels = guild.channels.mapNotNull channel@{ channel ->
          val channelId = channel.id
          if (channel is MessageChannel) {
            if (guildInfoConnector.isChannelExclude(guildId, channelId)) return@channel null
            if (guildInfoConnector.getDuplicateInfoChannel(guildId) == channelId) return@channel null
            channel
          } else {
            null
          }
        }
        channelCounter.totalIncrease(channels.size)
        guildCounter.doneIncrement()
        channels
      }
      val messagesChannel = flatChannelTransform(channelsChannel, 4) { channel ->
        var attempts = 5
        LOG.log(Level.INFO, "Channel processing ${channelCounter.status()}: ${channel.getName()}")
        while (attempts > 0) {
          attempts--
          try {
            val messages = channel.iterableHistory.takeWhile(takeWhile).reversed()
            messageCounter.totalIncrease(messages.size)
            channelCounter.doneIncrement()
            return@flatChannelTransform messages
          } catch (_: InsufficientPermissionException) {
            LOG.log(Level.INFO, "Insufficient permissions for channel ${channel.getName()}")
            channelCounter.doneIncrement()
            return@flatChannelTransform emptyList()
          } catch (e: InterruptedException) {
            LOG.log(Level.WARNING, "Interrupted exception while take messages", e)
          }
        }
        LOG.log(Level.SEVERE, "Can't take channel ${channel.getName()} in few attempts")
        emptyList()
      }
      channelTransform(messagesChannel, 8) { message ->
        val messageNumber = messageCounter.doneIncrement()
        if (messageNumber % 100 == 0) {
          LOG.log(Level.INFO, "Message processing ${messageCounter.status()}")
        }
        computeMessage(message)
      }
    }
  }
}