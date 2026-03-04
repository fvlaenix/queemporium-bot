package com.fvlaenix.queemporium.database

import com.fvlaenix.queemporium.koin.BaseKoinTest
import org.junit.jupiter.api.Assertions.*
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
      state = HallOfFameState.NOT_SELECTED,
      hofMessageId = null,
      thresholdCrossDetectedAt = System.currentTimeMillis()
    )

    assertTrue(connector.addMessage(message))
    assertFalse(connector.addMessage(message))
  }

  @Test
  fun `markMessagesAsToSend uses message timestamp for max age filtering`() {
    val koin = setupBotKoin {}
    val databaseConfig: com.fvlaenix.queemporium.configuration.DatabaseConfiguration = koin.get()
    val connector = HallOfFameConnector(databaseConfig.toDatabase())

    val guildId = "guild-id"
    val nowMillis = 1_700_000_000_000L
    val oneDayMillis = 24L * 60L * 60L * 1000L
    val sevenDaysMillis = 7L * oneDayMillis

    val recentTimestampSeconds = (nowMillis - 2L * oneDayMillis) / 1000L
    val oldTimestampSeconds = (nowMillis - 30L * oneDayMillis) / 1000L

    connector.addMessage(
      HallOfFameMessage(
        messageId = "recent",
        guildId = guildId,
        timestamp = recentTimestampSeconds,
        state = HallOfFameState.NOT_SELECTED,
        hofMessageId = null,
        thresholdCrossDetectedAt = nowMillis
      )
    )
    connector.addMessage(
      HallOfFameMessage(
        messageId = "old",
        guildId = guildId,
        timestamp = oldTimestampSeconds,
        state = HallOfFameState.NOT_SELECTED,
        hofMessageId = null,
        thresholdCrossDetectedAt = nowMillis
      )
    )

    val updated = connector.markMessagesAsToSend(
      guildId = guildId,
      maxAgeMillis = sevenDaysMillis,
      currentTimeMillis = nowMillis
    )

    assertEquals(1, updated)
    assertEquals(HallOfFameState.TO_SEND, connector.getMessage("recent")?.state)
    assertEquals(HallOfFameState.NOT_SELECTED, connector.getMessage("old")?.state)
  }
}
