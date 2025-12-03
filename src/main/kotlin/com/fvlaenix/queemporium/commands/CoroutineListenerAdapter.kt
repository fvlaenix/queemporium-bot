package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.exception.EXCEPTION_HANDLER
import com.fvlaenix.queemporium.utils.CoroutineUtils
import com.fvlaenix.queemporium.utils.CoroutineUtils.channelTransform
import com.fvlaenix.queemporium.utils.CoroutineUtils.flatChannelTransform
import com.fvlaenix.queemporium.utils.LogContextKeys
import com.fvlaenix.queemporium.utils.Logging
import com.fvlaenix.queemporium.utils.mdcCoroutineContext
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.annotations.TestOnly

private val LOG = Logging.getLogger(CoroutineListenerAdapter::class.java)

open class CoroutineListenerAdapter(val coroutineProvider: BotCoroutineProvider) : ListenerAdapter() {
  private fun getCoroutinePool(): ExecutorCoroutineDispatcher = coroutineProvider.botPool
  private fun getCoroutineScope(): CoroutineScope = coroutineProvider.mainScope

  @TestOnly
  fun testCoroutineScope(): CoroutineScope = getCoroutineScope()
  protected fun messageInfo(message: Message): String =
    "message from ${message.author.name} with url: ${message.jumpUrl}"

  protected fun Message.isFromAdmin(): Boolean = member?.hasPermission(Permission.ADMINISTRATOR) == true

  protected fun Message.isFromRoleAdmin(): Boolean {
    val roles = member?.roles ?: return false
    return roles.any { role -> role.name.equals("admin", ignoreCase = true) }
  }

  open suspend fun onReadySuspend(event: ReadyEvent) {}

  override fun onReady(event: ReadyEvent) {
    getCoroutineScope().launch(getCoroutinePool() + EXCEPTION_HANDLER + mdcCoroutineContext()) {
      try {
        onReadySuspend(event)
      } catch (e: Exception) {
        if (e !is CancellationException) {
          LOG.error("Global Panic: Can't finish ready event", e)
        }
      }
    }
  }

  protected open fun receiveMessageFilter(event: MessageReceivedEvent): Boolean = true

  open suspend fun onMessageReceivedSuspend(event: MessageReceivedEvent) {}

  override fun onMessageReceived(event: MessageReceivedEvent) {
    if (!receiveMessageFilter(event)) return

    val contextMap = mutableMapOf<String, String>()
    if (event.isFromGuild) {
      contextMap[LogContextKeys.GUILD_ID] = event.guild.id
    }
    contextMap[LogContextKeys.CHANNEL_ID] = event.channel.id
    contextMap[LogContextKeys.USER_ID] = event.author.id

    getCoroutineScope().launch(getCoroutinePool() + EXCEPTION_HANDLER + mdcCoroutineContext(contextMap)) {
      try {
        onMessageReceivedSuspend(event)
      } catch (e: Exception) {
        LOG.error("Global Panic: Received ${messageInfo(event.message)}", e)
      }
    }
  }

  open suspend fun onMessageUpdateSuspend(event: MessageUpdateEvent) {}

  override fun onMessageUpdate(event: MessageUpdateEvent) {
    val contextMap = mutableMapOf<String, String>()
    if (event.isFromGuild) {
      contextMap[LogContextKeys.GUILD_ID] = event.guild.id
    }
    contextMap[LogContextKeys.CHANNEL_ID] = event.channel.id
    contextMap[LogContextKeys.USER_ID] = event.author.id

    getCoroutineScope().launch(getCoroutinePool() + EXCEPTION_HANDLER + mdcCoroutineContext(contextMap)) {
      try {
        onMessageUpdateSuspend(event)
      } catch (e: Exception) {
        LOG.error("Global Panic: Updated ${messageInfo(event.message)}", e)
      }
    }
  }

  open suspend fun onMessageDeleteSuspend(event: MessageDeleteEvent) {}

  override fun onMessageDelete(event: MessageDeleteEvent) {
    val contextMap = mutableMapOf<String, String>()
    if (event.isFromGuild) {
      contextMap[LogContextKeys.GUILD_ID] = event.guild.id
    }
    contextMap[LogContextKeys.CHANNEL_ID] = event.channel.id

    getCoroutineScope().launch(getCoroutinePool() + EXCEPTION_HANDLER + mdcCoroutineContext(contextMap)) {
      try {
        onMessageDeleteSuspend(event)
      } catch (e: Exception) {
        LOG.error("Global Panic: Deleted message with id ${event.messageId}", e)
      }
    }
  }

  protected suspend fun runOverOld(
    jda: JDA,
    jobName: String,
    guildThreshold: Int,
    channelThreshold: Int,
    messageThreshold: Int,
    computeGuild: suspend (Guild) -> List<MessageChannel>,
    computeChannel: suspend (MessageChannel) -> List<Message>,
    computeMessage: suspend (Message) -> Unit,
    isShuffled: Boolean
  ) {
    val guildCounter = CoroutineUtils.AtomicProgressCounter()
    val channelCounter = CoroutineUtils.AtomicProgressCounter()
    val messageCounter = CoroutineUtils.AtomicProgressCounter()
    coroutineScope {
      guildCounter.totalIncrease(jda.guilds.size)
      val guilds = if (isShuffled) {
        jda.guilds.shuffled()
      } else {
        jda.guilds
      }
      val channelsChannel = flatChannelTransform(guilds, guildThreshold) { guild ->
        LOG.info("$jobName: Guild processing ${guildCounter.status()}: ${guild.name}")
        val channels = computeGuild(guild)
        channelCounter.totalIncrease(channels.size)
        guildCounter.doneIncrement()
        channels
      }
      val messagesChannel = flatChannelTransform(channelsChannel, channelThreshold) { channel ->
        LOG.info("$jobName: Channel processing ${channelCounter.status()}: ${channel.getName()}")
        val messages = computeChannel(channel)
        messageCounter.totalIncrease(messages.size)
        channelCounter.doneIncrement()
        messages
      }
      channelTransform(messagesChannel, messageThreshold) { message ->
        val messageNumber = messageCounter.doneIncrement()
        if (messageNumber % 100 == 0) {
          LOG.info("$jobName: Message processing ${messageCounter.status()}")
        }
        computeMessage(message)
      }
    }
    LOG.info("$jobName: Finish onReady")
  }

  protected suspend fun runOverOld(
    jda: JDA,
    jobName: String,
    guildThreshold: Int = 2,
    channelThreshold: Int = 4,
    messageThreshold: Int = Runtime.getRuntime().availableProcessors(),
    computeGuild: suspend (Guild) -> List<MessageChannel>,
    takeWhile: (Message) -> Boolean,
    computeMessage: suspend (Message) -> Unit,
    isShuffled: Boolean = false
  ) {
    val computeChannel: (MessageChannel) -> List<Message> = channel@{ channel ->
      var attempts = 5
      while (attempts > 0) {
        attempts--
        try {
          val messages = channel.iterableHistory.takeWhile(takeWhile).reversed()
          return@channel if (isShuffled) {
            messages.shuffled()
          } else {
            messages
          }
        } catch (_: InsufficientPermissionException) {
          LOG.info("$jobName: Insufficient permissions for channel ${channel.getName()}")
          return@channel emptyList()
        } catch (e: InterruptedException) {
          LOG.warn("$jobName: Interrupted exception while take messages", e)
        }
      }
      LOG.error("$jobName: Can't take channel ${channel.getName()} in few attempts")
      emptyList()
    }
    runOverOld(
      jda,
      jobName,
      guildThreshold,
      channelThreshold,
      messageThreshold,
      computeGuild,
      computeChannel,
      computeMessage,
      isShuffled
    )
  }
}
