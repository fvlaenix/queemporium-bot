package com.fvlaenix.queemporium.database

import com.fvlaenix.queemporium.koin.BaseKoinTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HallOfFameConnectorTest : BaseKoinTest() {

  @Test
  fun `addMessage returns false for duplicate message`() {
    val koin = setupBotKoin {}
    val databaseConfig: com.fvlaenix.queemporium.configuration.DatabaseConfiguration = koin.get()
    val connector = HallOfFameConnector(databaseConfig.toDatabase())

    val message = HallOfFameMessage(
      messageId = "message-id",
      guildId = "guild-id",
      timestamp = 123L,
      isSent = false
    )

    assertTrue(connector.addMessage(message))
    assertFalse(connector.addMessage(message))
  }
}
