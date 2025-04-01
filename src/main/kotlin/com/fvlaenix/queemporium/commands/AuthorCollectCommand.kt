package com.fvlaenix.queemporium.commands

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.AuthorData
import com.fvlaenix.queemporium.database.AuthorDataConnector
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.jetbrains.annotations.TestOnly
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

private val LOG = Logger.getLogger(AuthorCollectCommand::class.java.name)

class AuthorCollectCommand(val databaseConfiguration: DatabaseConfiguration) : CoroutineListenerAdapter() {
  private val authorDataConnector = AuthorDataConnector(databaseConfiguration.toDatabase())

  private fun runCollect(jda: JDA) {
    LOG.log(Level.INFO, "Start author collect")
    jda.guilds.forEach { guild ->
      val guildId = guild.id
      guild.loadMembers().onSuccess { members ->
        val authorsData = members.map { member -> AuthorData(member.id, guildId, member.user.name) }
        authorDataConnector.replaceAuthors(authorsData, guild.id)
      }
    }
  }

  override suspend fun onReadySuspend(event: ReadyEvent) {
    while (true) {
      runCollect(event.jda)
      delay(1.days)
    }
  }

  @TestOnly
  fun runTestCollect(jda: JDA) {
    runCollect(jda)
  }
}