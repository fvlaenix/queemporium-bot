package com.fvlaenix.queemporium.commands.excludeChannel

import com.fvlaenix.queemporium.commands.ExcludeChannelCommand
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ExcludeChannelCommand functionality.
 */
class ExcludeChannelCommandTest : BaseExcludeChannelCommandTest() {

  @Test
  fun `test add channel to excluded list by admin`() {
    // Send command to add channel to excluded list (should work for admin user)
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = ExcludeChannelCommand.COMMAND_ADD_TO_EXCLUDE,
      isAdmin = true
    )

    // Verify channel was added to excluded list
    assertTrue(
      guildInfoConnector.isChannelExcluded(testGuild.id, generalChannel.id),
      "Channel should be added to excluded list"
    )

    // Verify the response message
    answerService.verify {
      messageCount(1)
      lastMessageContains("All right, if you beg me")
    }
  }

  @Test
  fun `test remove channel from excluded list by admin`() {
    // First add the channel to excluded list
    guildInfoConnector.addExcludingChannel(testGuild.id, generalChannel.id)

    // Send command to remove channel from excluded list
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = ExcludeChannelCommand.COMMAND_REMOVE_FROM_EXCLUDE,
      isAdmin = true
    )

    env.awaitAll()

    // Verify channel was removed from excluded list
    assertFalse(
      guildInfoConnector.isChannelExcluded(testGuild.id, generalChannel.id),
      "Channel should be removed from excluded list"
    )

    // Verify the response message
    answerService.verify {
      messageCount(1)
      lastMessageContains("I'm eyeing this channel now")
    }
  }

  @Test
  fun `test add channel that is already excluded`() {
    // First add the channel to excluded list
    guildInfoConnector.addExcludingChannel(testGuild.id, generalChannel.id)

    // Send command to add channel that is already excluded
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = ExcludeChannelCommand.COMMAND_ADD_TO_EXCLUDE,
      isAdmin = true
    )

    // Verify the response message indicates channel is already excluded
    answerService.verify {
      messageCount(1)
      lastMessageContains("I don't watch this channel anyway")
    }
  }

  @Test
  fun `test remove channel that is not excluded`() {
    // Ensure channel is not in excluded list
    guildInfoConnector.removeExcludingChannel(testGuild.id, generalChannel.id)

    // Send command to remove channel that is not excluded
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = ExcludeChannelCommand.COMMAND_REMOVE_FROM_EXCLUDE,
      isAdmin = true
    )

    // Verify the response message indicates channel is not excluded
    answerService.verify {
      messageCount(1)
      lastMessageContains("I'm watching this channel as it is")
    }
  }

  @Test
  fun `test non-admin user cannot add channel to excluded list`() {
    // Send command to add channel to excluded list (should fail for non-admin user)
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = ExcludeChannelCommand.COMMAND_ADD_TO_EXCLUDE,
      isAdmin = false
    )

    // Verify channel was not added to excluded list
    assertFalse(
      guildInfoConnector.isChannelExcluded(testGuild.id, generalChannel.id),
      "Channel should not be added to excluded list by non-admin"
    )

    // Verify the response message indicates permission denied
    answerService.verify {
      messageCount(1)
      lastMessageContains("only admins can use this")
    }
  }

  @Test
  fun `test non-admin user cannot remove channel from excluded list`() {
    // First add the channel to excluded list
    guildInfoConnector.addExcludingChannel(testGuild.id, generalChannel.id)

    // Send command to remove channel from excluded list (should fail for non-admin user)
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = ExcludeChannelCommand.COMMAND_REMOVE_FROM_EXCLUDE,
      isAdmin = false
    )

    // Verify channel was not removed from excluded list
    assertTrue(
      guildInfoConnector.isChannelExcluded(testGuild.id, generalChannel.id),
      "Channel should not be removed from excluded list by non-admin"
    )

    // Verify the response message indicates permission denied
    answerService.verify {
      messageCount(1)
      lastMessageContains("only admins can use this")
    }
  }

  @Test
  fun `test command in DM channel fails`() {
    // Create a direct message scenario
    sendDirectMessage(
      command = ExcludeChannelCommand.COMMAND_ADD_TO_EXCLUDE
    )

    // Verify response indicates command only works in servers
    answerService.verify {
      messageCount(1)
      lastMessageContains("only applies to servers")
    }
  }

  @Test
  fun `test unrelated commands are ignored`() {
    // Send unrelated command
    sendCommand(
      channelName = defaultGeneralChannelName,
      command = "/shogun-sama unrelated-command",
      isAdmin = true
    )

    // Verify no response
    answerService.verify {
      messageCount(0)
    }
  }
}