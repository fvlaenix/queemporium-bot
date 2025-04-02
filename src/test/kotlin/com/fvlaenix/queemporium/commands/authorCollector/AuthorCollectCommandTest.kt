package com.fvlaenix.queemporium.commands.authorCollector

import com.fvlaenix.queemporium.database.AuthorData
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for AuthorCollectCommand functionality
 */
class AuthorCollectCommandTest : BaseAuthorCollectCommandTest() {

  @Test
  fun `test command collects author data on startup`() {
    val initialAuthors = authorDataConnector.getAuthorsByGuildId(testGuild.id)
    assertEquals(0, initialAuthors.size, "Database should be empty before test")

    startEnvironment()

    env.awaitAll()

    val authors = authorDataConnector.getAuthorsByGuildId(testGuild.id)

    assertEquals(
      testUsers.size, authors.size,
      "Should collect data for all test users (${testUsers.size})"
    )

    testUsers.forEach { user ->
      val authorExists = authorDataConnector.hasAuthor(testGuild.id, user.id)
      assertTrue(authorExists, "Author data for user ${user.name} should be stored")

      val author = authors.find { it.authorId == user.id }
      assertEquals(user.name, author?.authorName, "Author name should match user name")
    }
  }

  @Test
  fun `test command replaces existing author data with new data`() {
    // Arrange - add initial data with outdated names
    val initialAuthors = testUsers.map { user ->
      AuthorData(
        authorId = user.id,
        guildId = testGuild.id,
        authorName = "OldName_${user.name}" // Use different name
      )
    }

    // Manually add data through connector
    authorDataConnector.replaceAuthors(initialAuthors, testGuild.id)

    // Verify initial data was stored
    val storedInitial = authorDataConnector.getAuthorsByGuildId(testGuild.id)
    assertEquals(testUsers.size, storedInitial.size, "Initial data should be stored")

    // Act - run the command
    startEnvironment()
    env.awaitAll()

    // Assert - verify data was updated
    val updatedAuthors = authorDataConnector.getAuthorsByGuildId(testGuild.id)

    // Number of authors should remain the same
    assertEquals(
      testUsers.size, updatedAuthors.size,
      "Number of authors should remain the same"
    )

    // Names should be updated
    testUsers.forEach { user ->
      val author = updatedAuthors.find { it.authorId == user.id }
      assertEquals(
        user.name, author?.authorName,
        "Author name should be updated to current user name"
      )
    }
  }

  @Test
  fun `test command handles multiple guilds correctly`() {
    // Arrange - create a second guild with different users
    val secondGuildName = "Second Guild"
    val secondGuild = env.createGuild(secondGuildName)

    // Create users for the second guild
    val secondGuildUsers = (1..3).map { i ->
      env.createUser("SecondGuildUser$i", false)
    }

    // Add users to the second guild
    secondGuildUsers.forEach { user ->
      env.createMember(secondGuild, user)
    }

    // Act - start the command
    startEnvironment()
    env.awaitAll()

    // Assert - verify data for both guilds is stored correctly
    val firstGuildAuthors = authorDataConnector.getAuthorsByGuildId(testGuild.id)
    val secondGuildAuthors = authorDataConnector.getAuthorsByGuildId(secondGuild.id)

    // Check first guild data
    assertEquals(
      testUsers.size, firstGuildAuthors.size,
      "Should collect data for all test users in first guild"
    )

    // Check second guild data
    assertEquals(
      secondGuildUsers.size, secondGuildAuthors.size,
      "Should collect data for all test users in second guild"
    )

    // Verify data correctness for each guild
    testUsers.forEach { user ->
      val authorExists = authorDataConnector.hasAuthor(testGuild.id, user.id)
      assertTrue(authorExists, "Author data for user ${user.name} should be stored in first guild")
    }

    secondGuildUsers.forEach { user ->
      val authorExists = authorDataConnector.hasAuthor(secondGuild.id, user.id)
      assertTrue(authorExists, "Author data for user ${user.name} should be stored in second guild")
    }
  }
}