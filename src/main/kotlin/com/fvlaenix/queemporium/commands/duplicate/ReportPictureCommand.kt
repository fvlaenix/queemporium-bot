package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import com.fvlaenix.queemporium.utils.AnswerUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

private val LOG = Logger.getLogger(ReportPictureCommand::class.java.name)

abstract class ReportPictureCommand(databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
  private val guildInfoConnector = GuildInfoConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val dependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  
  suspend fun getMessage(compressSize: CompressSize, message: Message) {
    if (message.author.isSystem) return
    val duplicateChannelId = guildInfoConnector.getDuplicateInfoChannel(message.guildId!!) ?: return
    val duplicateChannel = message.guild.getTextChannelById(duplicateChannelId) ?: return

    val messageData = MessageData(
      messageId = MessageId(
        guildId = message.guildId!!,
        channelId = message.channel.id,
        messageId = message.id
      ),
      text = message.contentRaw,
      hasSource = message.contentRaw.contains("http"),
      url = message.jumpUrl,
      author = message.author.id,
      epoch = message.timeCreated.toEpochSecond(),
      countImages = message.attachments.size + message.embeds.size,
      messageProblems = MessageProblems(emptyList())
    )

    if (messageDataConnector.get(messageData.messageId) != null) return
    
    DuplicateImageService.sendPictures(
      message = message,
      compressSize = compressSize,
      withHistoryReload = true
    ) { (duplicateMessageInfo, originalImageDatas) ->
      val isSpoiler =
        duplicateMessageInfo.additionalImageInfo.isSpoiler || originalImageDatas.any { it.additionalImageInfo.isSpoiler }
      val originalData = originalImageDatas.map {
        messageDataConnector.get(it.imageId.toMessageId())!! to it
      }
      val duplicateMessageDatas = AnswerUtils.sendDuplicateMessageInfo(
        duplicateChannel = duplicateChannel,
        messageAuthorId = message.author.id,
        fileName = duplicateMessageInfo.additionalImageInfo.fileName,
        image = duplicateMessageInfo.bufferedImage,
        messageData = messageData,
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
    messageDataConnector.add(messageData)
  }
  
  @OptIn(DelicateCoroutinesApi::class)
  suspend fun runOverOld(jda: JDA, takeWhile: (Message) -> Boolean, computeMessage: suspend (Message) -> Unit) {
    val channelsThreadContext = newFixedThreadPoolContext(16, "Report Pictures Thread Pool")

    val channelsChannel = Channel<MessageChannel>(Channel.UNLIMITED)
    val channelsWork = AtomicInteger(0)
    val channelsDone = AtomicInteger(0)
    val messageChannel = Channel<Message>(Channel.UNLIMITED)
    val messageWork = AtomicInteger(0)
    val messageDone = AtomicInteger(0)
    coroutineScope {
      LOG.log(Level.INFO, "Start revenge on guilds")
      jda.guilds.forEach guild@{ guild ->
        val guildId = guild.id
        LOG.log(Level.INFO, "Start revenge on guild ${guild.name}")
        guild.channels.forEach channel@{ channel ->
          val channelId = channel.id
          LOG.log(Level.INFO, "Start revenge on ${channel.name}")
          if (channel is MessageChannel) {
            if (guildInfoConnector.isChannelExclude(guildId, channelId)) return@channel
            if (guildInfoConnector.getDuplicateInfoChannel(guildId) == channelId) return@channel

            channelsChannel.send(channel)
            channelsWork.incrementAndGet()
          }
        }
      }
      channelsChannel.close()

      val channelsJobs = mutableListOf<Job>()
      LOG.log(Level.INFO, "Start revenge on channels")
      for (channel in channelsChannel) {
        val job = launch(channelsThreadContext + EXCEPTION_HANDLER) {
          channelsDone.incrementAndGet()
          var isLoaded = false
          while (!isLoaded) {
            LOG.log(Level.INFO, "Start revenge on channel [${channelsDone.get()}/${channelsWork.get()}]: ${channel.name}")
            try {
              val messages = channel.iterableHistory.takeWhile(takeWhile).reversed()
              messages.forEach { message ->
                messageChannel.send(message)
                messageWork.incrementAndGet()
              }
              isLoaded = true
            } catch (_: InsufficientPermissionException) { 
              LOG.log(Level.INFO, "Insufficient permissions for channel ${channel.name}")
              isLoaded = true
            } catch (e: InterruptedException) {
              LOG.log(Level.WARNING, "Interrupted exception while take messages", e)
            }
          }
          LOG.log(Level.INFO, "Finish revenge on messages: ${channel.name}")
        }
        channelsJobs.add(job)
      }
      launch(channelsThreadContext + EXCEPTION_HANDLER) {
        channelsJobs.joinAll()
        LOG.log(Level.FINE, "Finish revenges on channels")
        messageChannel.close()
      }

      val messageJobs = mutableListOf<Job>()
      LOG.log(Level.INFO, "Start revenge on messages")
      for (message in messageChannel) {
        val job = launch(channelsThreadContext + EXCEPTION_HANDLER) {
          val messageNumber = messageDone.getAndIncrement()
          if (messageNumber % 100 == 0) {
            LOG.log(Level.INFO, "Start revenge on message [$messageNumber/${messageWork.get()}] from channel: ${message.channel.name}")
          }
          computeMessage(message)
        }
        messageJobs.add(job)
      }
      launch(channelsThreadContext + EXCEPTION_HANDLER) {
        messageJobs.joinAll()
        LOG.log(Level.FINE, "Finish revenges on messages")
      }
    }
  }
}