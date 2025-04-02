package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for SetDuplicateChannelCommand functionality.
 */
class SetDuplicateChannelCommandTest : BaseSetDuplicateChannelCommandTest() {

  private val command = "/shogun-sama beg-this-channel-duplicate"

  @Test
  fun `test admin can set duplicate channel`() {
    // Send command as admin
    env.sendMessage(
      defaultGuildName,
      defaultGeneralChannelName,
      adminUser,
      command
    ).queue()

    // Wait for processing
    env.awaitAll()

    // Verify channel was set as duplicate channel
    val duplicateChannel = guildInfoConnector.getDuplicateInfoChannel(testGuild.id)
    assertNotNull(duplicateChannel, "Duplicate channel should be set")
    assertEquals(generalChannel.id, duplicateChannel, "General channel should be set as duplicate channel")

    // Verify the response message
    answerService.verify {
      messageCount(1)
      lastMessageContains("My verdicts will now appear here!")
    }
  }

  @Test
  fun `test non-admin cannot set duplicate channel`() {
    // Send command as regular user
    env.sendMessage(
      defaultGuildName,
      defaultGeneralChannelName,
      regularUser,
      command
    ).queue()

    // Wait for processing
    env.awaitAll()

    // Verify channel was not set as duplicate channel
    val duplicateChannel = guildInfoConnector.getDuplicateInfoChannel(testGuild.id)
    assertEquals(null, duplicateChannel, "Duplicate channel should not be set by non-admin")

    // Verify the response message indicates permission denied
    answerService.verify {
      messageCount(1)
      lastMessageContains("only admins can use this")
    }
  }

  @Test
  fun `test command in DM channel fails`() {
    // Create a special user for direct messages
    val dmUser = env.createUser("DM User", false)

    // Send the direct message
    env.sendDirectMessage(dmUser, command).queue()

    // Wait for processing
    env.awaitAll()

    // Verify response indicates command only works in servers
    answerService.verify {
      messageCount(1)
      lastMessageContains("only applies to servers")
    }
  }

  @Test
  fun `test can update duplicate channel to new channel`() {
    // Create a second channel
    val newChannelName = "new-duplicate-channel"
    env.createTextChannel(testGuild, newChannelName)

    // First set general channel as duplicate channel
    env.sendMessage(
      defaultGuildName,
      defaultGeneralChannelName,
      adminUser,
      command
    ).queue()

    env.awaitAll()

    // Verify first channel was set
    val firstDuplicateChannel = guildInfoConnector.getDuplicateInfoChannel(testGuild.id)
    assertEquals(generalChannel.id, firstDuplicateChannel, "General channel should be set initially")

    // Now set the new channel as duplicate channel
    env.sendMessage(
      defaultGuildName,
      newChannelName,
      adminUser,
      command
    ).queue()

    env.awaitAll()

    // Verify new channel was set
    val newDuplicateChannel = guildInfoConnector.getDuplicateInfoChannel(testGuild.id)
    val actualNewChannel = testGuild.getTextChannelsByName(newChannelName, false).first()
    assertEquals(actualNewChannel.id, newDuplicateChannel, "New channel should be set as duplicate channel")

    // Verify we got two success messages
    answerService.verify {
      messageCount(2)
      messagesContain("My verdicts will now appear here!")
    }
  }
}