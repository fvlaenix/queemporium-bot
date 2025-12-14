package com.fvlaenix.queemporium.commands.advent

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.koin.BaseKoinTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AdventDataConnectorTest : BaseKoinTest() {

  @Test
  fun `initializeAdvent rejects entries from different guilds`() {
    val koin = setupBotKoin {}
    val database = koin.get<DatabaseConfiguration>().toDatabase()
    val connector = AdventDataConnector(database)

    val advents = listOf(
      AdventData(
        messageId = "1",
        messageDescription = "First entry",
        guildPostId = "guild-1",
        channelPostId = "channel-1",
        epoch = 0,
        isRevealed = false
      ),
      AdventData(
        messageId = "2",
        messageDescription = "Second entry",
        guildPostId = "guild-2",
        channelPostId = "channel-2",
        epoch = 0,
        isRevealed = false
      )
    )

    assertFailsWith<IllegalArgumentException> {
      connector.initializeAdvent(advents)
    }
  }
}
