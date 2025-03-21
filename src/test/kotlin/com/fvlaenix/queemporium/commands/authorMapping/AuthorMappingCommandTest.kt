package com.fvlaenix.queemporium.commands.authorMapping

import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Test

/**
 * Tests for AuthorMappingCommand functionality
 */
class AuthorMappingCommandTest : BaseAuthorMappingCommandTest() {

  @Test
  fun `test message with incorrect author name sends notification`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Send message with incorrect author name and attachment
    val message = sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author!"
    )

    // Verify that a notification was sent
    mockAnswerService.verify {
      messageCount(1)
      lastMessageContains("made mistake in author name")
      lastMessageContains("Change name from incorrect-author to correct-author")
      lastMessageContains(message.jumpUrl)
    }
  }

  @Test
  fun `test message without attachments is ignored`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Send message without attachments
    env.sendMessage(
      defaultGuildName,
      defaultGeneralChannelName,
      testUser,
      "Check out this artwork by incorrect-author!"
    ).queue()

    env.awaitAll()

    // Verify that no notification was sent
    mockAnswerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test message without incorrect author name is ignored`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Send message with correct author name and attachment
    sendMessageWithAttachment(
      messageText = "Check out this artwork by correct-author!"
    )

    // Verify that no notification was sent
    mockAnswerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test message with both correct and incorrect author names is ignored`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Send message with both correct and incorrect author names and attachment
    sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author and correct-author!"
    )

    // Verify that no notification was sent
    mockAnswerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test message from bot is ignored`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Create bot user
    val botUser = env.createUser("Bot User", true)

    // Send message from bot with incorrect author name and attachment
    env.sendMessage(
      defaultGuildName,
      defaultGeneralChannelName,
      botUser,
      "Check out this artwork by incorrect-author!",
      listOf(createTestAttachment("test_image.jpg"))
    ).queue()

    env.awaitAll()

    // Verify that no notification was sent
    mockAnswerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test without duplicate channel configured is ignored`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Remove duplicate channel configuration
    guildInfoConnector.setDuplicateInfo(testGuild.id, null)

    // Send message with incorrect author name and attachment
    sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author!"
    )

    // Verify that no notification was sent
    mockAnswerService.verify {
      messageCount(0)
    }
  }

  @Test
  fun `test with multiple incorrect author names`() {
    // Setup author mapping with multiple incorrect names
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author-1", "incorrect-author-2"),
      correctNames = listOf("correct-author")
    )

    // Send message with the first incorrect author name and attachment
    val message1 = sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author-1!"
    )

    // Verify notification for the first message
    mockAnswerService.verify {
      messageCount(1)
      lastMessageContains("Change name from incorrect-author-1 to correct-author")
      lastMessageContains(message1.jumpUrl)
    }

    // Clear messages
    mockAnswerService.answers.clear()

    // Send message with the second incorrect author name and attachment
    val message2 = sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author-2!"
    )

    // Verify notification for the second message
    mockAnswerService.verify {
      messageCount(1)
      lastMessageContains("Change name from incorrect-author-2 to correct-author")
      lastMessageContains(message2.jumpUrl)
    }
  }

  @Test
  fun `test with multiple correct author names`() {
    // Setup author mapping with multiple correct names
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author-1", "correct-author-2")
    )

    // Send message with incorrect author name and attachment
    val message = sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author!"
    )

    // Verify that notification includes all correct names
    mockAnswerService.verify {
      messageCount(1)
      lastMessageContains("Change name from incorrect-author to correct-author-1 / correct-author-2")
      lastMessageContains(message.jumpUrl)
    }
  }

  @Test
  fun `test complex author name matching`() {
    // Setup author mapping with names that include special characters
    addAuthorMapping(
      incorrectNames = listOf("Artist-X", "X.Artist"),
      correctNames = listOf("X_Artist", "Artist_X")
    )

    // Send message with incorrect author name in complex text
    val message = sendMessageWithAttachment(
      messageText = "This is a complex text with many words and an incorrect author name Artist-X somewhere in the middle."
    )

    // Verify that notification was sent with correct mapping
    mockAnswerService.verify {
      messageCount(1)
      lastMessageContains("Change name from Artist-X to X_Artist / Artist_X")
    }
  }

  @Test
  fun `test notification is sent to correct channel`() {
    // Setup author mapping
    addAuthorMapping(
      incorrectNames = listOf("incorrect-author"),
      correctNames = listOf("correct-author")
    )

    // Create another channel
    val anotherChannel = env.createTextChannel(testGuild, "another-channel")

    // Set another channel as duplicate channel
    guildInfoConnector.setDuplicateInfo(testGuild.id, anotherChannel.id)

    // Send message with incorrect author name and attachment
    sendMessageWithAttachment(
      messageText = "Check out this artwork by incorrect-author!"
    )

    // Verify that notification was sent (and implicitly to the correct channel)
    mockAnswerService.verify {
      messageCount(1)
      lastMessageContains("made mistake in author name")
      message {
        channelId(anotherChannel.id)
      }
    }
  }
}