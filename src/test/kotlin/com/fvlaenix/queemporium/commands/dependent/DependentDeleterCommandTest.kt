package com.fvlaenix.queemporium.commands.dependent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for DependentDeleterCommand functionality.
 * Tests various scenarios related to deleting dependent messages when a target message is deleted.
 */
class DependentDeleterCommandTest : BaseDependentDeleterCommandTest() {

  @Test
  fun `test deleting message with dependent messages deletes the dependent messages`() {
    // Create original message (target)
    val targetMessage = createAndStoreMessage(messageText = "Original message")

    // Create dependent message
    val dependentMessage = createAndStoreMessage(messageText = "Dependent message")

    // Create dependency between messages
    createDependency(targetMessage.id, dependentMessage.id)

    // Verify dependency was created
    val dependencies = messageDependencyConnector.getDependencies(targetMessage.id)
    assertEquals(1, dependencies.size, "Should have one dependency")
    assertEquals(dependentMessage.id, dependencies[0], "Dependent message ID should match")

    // Verify both messages exist before deletion
    assertNotNull(messageDataConnector.get(targetMessage.id), "Target message should exist in database")
    assertNotNull(messageDataConnector.get(dependentMessage.id), "Dependent message should exist in database")

    // Delete the target message
    deleteMessage(targetMessage)

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify dependent message is also deleted from DB
    assertFalse(
      messageDependencyConnector.getDependencies(targetMessage.id).contains(dependentMessage.id),
      "Dependency should be removed after deletion"
    )
  }

  @Test
  fun `test deleting message without dependent messages`() {
    // Create message with no dependencies
    val message = createAndStoreMessage(messageText = "Message with no dependencies")

    // Verify no dependencies exist
    val dependencies = messageDependencyConnector.getDependencies(message.id)
    assertTrue(dependencies.isEmpty(), "Should have no dependencies")

    // Delete the message
    deleteMessage(message)

    // Wait for all async operations to complete
    env.awaitAll()

    // No assertions needed - just verifying no exceptions are thrown
  }

  @Test
  fun `test deleting message with multiple dependent messages`() {
    // Create original message (target)
    val targetMessage = createAndStoreMessage(messageText = "Original message")

    // Create multiple dependent messages
    val dependentMessage1 = createAndStoreMessage(messageText = "Dependent message 1")
    val dependentMessage2 = createAndStoreMessage(messageText = "Dependent message 2")
    val dependentMessage3 = createAndStoreMessage(messageText = "Dependent message 3")

    // Create dependencies
    createDependency(targetMessage.id, dependentMessage1.id)
    createDependency(targetMessage.id, dependentMessage2.id)
    createDependency(targetMessage.id, dependentMessage3.id)

    // Verify dependencies were created
    val dependencies = messageDependencyConnector.getDependencies(targetMessage.id)
    assertEquals(3, dependencies.size, "Should have three dependencies")

    // Delete the target message
    deleteMessage(targetMessage)

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify all dependencies are removed
    assertTrue(
      messageDependencyConnector.getDependencies(targetMessage.id).isEmpty(),
      "All dependencies should be removed after deletion"
    )
  }

  @Test
  fun `test deleting message with missing dependent message`() {
    // Create original message (target)
    val targetMessage = createAndStoreMessage(messageText = "Original message")

    // Add a dependency to a non-existent message
    val nonExistentMessageId = "999999999999999999"
    createDependency(targetMessage.id, nonExistentMessageId)

    // Verify dependency was created in database
    val dependencies = messageDependencyConnector.getDependencies(targetMessage.id)
    assertEquals(1, dependencies.size, "Should have one dependency even if message doesn't exist")

    // Delete the target message
    deleteMessage(targetMessage)

    // Wait for all async operations to complete
    env.awaitAll()

    // No assertions needed - just verifying no exceptions are thrown
  }

  @Test
  fun `test deletion with chained dependencies`() {
    // Create chain of messages A -> B -> C (where A is target, B depends on A, C depends on B)
    val messageA = createAndStoreMessage(messageText = "Message A")
    val messageB = createAndStoreMessage(messageText = "Message B")
    val messageC = createAndStoreMessage(messageText = "Message C")

    // Create dependencies: A -> B and B -> C
    createDependency(messageA.id, messageB.id)
    createDependency(messageB.id, messageC.id)

    // Verify dependencies exist
    assertEquals(1, messageDependencyConnector.getDependencies(messageA.id).size)
    assertEquals(1, messageDependencyConnector.getDependencies(messageB.id).size)

    // Delete message A
    deleteMessage(messageA)

    // Wait for all async operations to complete
    env.awaitAll()

    // Verify B is deleted
    assertTrue(
      messageDependencyConnector.getDependencies(messageA.id).isEmpty(),
      "Dependency A -> B should be removed"
    )

    assertTrue(
      messageDependencyConnector.getDependencies(messageB.id).isEmpty(),
      "Dependency B -> C should be removed"
    )
  }
}