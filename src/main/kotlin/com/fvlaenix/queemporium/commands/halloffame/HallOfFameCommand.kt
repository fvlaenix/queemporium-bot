package com.fvlaenix.queemporium.commands.halloffame

import com.fvlaenix.queemporium.commands.CoroutineListenerAdapter
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.service.AnswerService
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.hours

private val LOG = Logger.getLogger(HallOfFameCommand::class.java.name)

class HallOfFameCommand(
  databaseConfiguration: DatabaseConfiguration,
  private val answerService: AnswerService,
  coroutineProvider: BotCoroutineProvider
) : CoroutineListenerAdapter(coroutineProvider) {
  private val hallOfFameConnector = HallOfFameConnector(databaseConfiguration.toDatabase())
  private val messageDataConnector = MessageDataConnector(databaseConfiguration.toDatabase())
  private val dependencyConnector = MessageDependencyConnector(databaseConfiguration.toDatabase())
  private val emojiDataConnector = EmojiDataConnector(databaseConfiguration.toDatabase())

  private var currentJob: Job? = null

  private fun updateHallOfFameMessages(hallOfFameInfo: HallOfFameInfo) {
    LOG.log(Level.INFO, "Updating Hall of Fame messages for guild ${hallOfFameInfo.guildId}")
    val messages = emojiDataConnector.getMessagesAboveThreshold(
      guildId = hallOfFameInfo.guildId,
      threshold = hallOfFameInfo.threshold,
    )

    messages.forEach { message ->
      val dataMessage = messageDataConnector.get(message) ?: return@forEach

      hallOfFameConnector.addMessage(
        HallOfFameMessage(
          messageId = message,
          guildId = hallOfFameInfo.guildId,
          timestamp = dataMessage.epoch,
          isSent = false
        )
      )
    }
  }

  private suspend fun processGuild(jda: JDA, guildId: String) {
    val hallOfFameInfo = hallOfFameConnector.getHallOfFameInfo(guildId) ?: return

    // Get oldest unsent message
    val message = hallOfFameConnector.getOldestUnsentMessage(guildId) ?: return

    try {
      val guild = jda.getGuildById(guildId) ?: return
      val dataMessage = messageDataConnector.get(message.messageId) ?: return

      val sourceChannel = guild.getTextChannelById(dataMessage.channelId) ?: return
      val targetChannel = guild.getTextChannelById(hallOfFameInfo.channelId) ?: return
      val sourceMessage = sourceChannel.retrieveMessageById(message.messageId).complete() ?: return

      val messageUrl = sourceMessage.jumpUrl
      val reactionCount = sourceMessage.reactions.sumOf { it.count }

      val sentMessage = answerService.sendMessage(
        destination = targetChannel,
        text = "Message reached $reactionCount reactions! 🎉\nOriginal message: $messageUrl"
      ).await()
      val forwardMessage = answerService.forwardMessage(
        message = sourceMessage,
        destination = targetChannel,
        failedCallback = {
          LOG.log(Level.SEVERE, "Failed to forward message: $it")
        }
      ).await()

      if (sentMessage != null) {
        hallOfFameConnector.markAsSent(message.messageId)
        dependencyConnector.addDependency(
          MessageDependency(
            targetMessage = message.messageId,
            dependentMessage = sentMessage
          )
        )
      }
      if (forwardMessage != null) {
        dependencyConnector.addDependency(
          MessageDependency(
            targetMessage = message.messageId,
            dependentMessage = forwardMessage
          )
        )
      }
    } catch (e: Exception) {
      LOG.log(Level.SEVERE, "Failed to process Hall of Fame message ${message.messageId}", e)
    }
  }

  private fun runHallOfFame(jda: JDA) {
    currentJob?.cancel()
    coroutineProvider.mainScope.launch(CoroutineName("Hall of Fame Send Part")) {
      while (true) {
        jda.guilds.forEach { guild ->
          processGuild(jda, guild.id)
        }
        coroutineProvider.safeDelay(4.hours)
      }
    }
    coroutineProvider.mainScope.launch(CoroutineName("Hall of Fame Retrieve Part")) {
      while (true) {
        hallOfFameConnector.getAll().forEach { info ->
          updateHallOfFameMessages(info)
        }
        coroutineProvider.safeDelay(9.hours)
      }
    }
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    runHallOfFame(event.jda)
  }
}