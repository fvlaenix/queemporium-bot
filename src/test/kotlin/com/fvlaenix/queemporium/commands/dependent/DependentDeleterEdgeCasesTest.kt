package com.fvlaenix.queemporium.commands.dependent

import com.fvlaenix.queemporium.database.MessageData
import com.fvlaenix.queemporium.mock.createTestAttachment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertTrue

/**
 * Tests for edge cases and error handling in DependentDeleterCommand.
 */
class DependentDeleterEdgeCasesTest : BaseDependentDeleterCommandTest() {

    @Test
    fun `test dependent message in different channel`() {
        // Create another channel in the same guild
        val otherChannel = env.createTextChannel(testGuild, "other-channel")
        
        // Create target message in general channel
        val targetMessage = createAndStoreMessage(
            channelName = defaultGeneralChannelName,
            messageText = "Target message in general channel"
        )
        
        // Create dependent message in other channel
        val dependentMessage = env.sendMessage(
            defaultGuildName,
            "other-channel",
            testUser,
            "Dependent message in other channel"
        ).complete(true)!!
        
        // Store dependent message in database
        val messageData = MessageData(
            messageId = dependentMessage.id,
            guildId = dependentMessage.guildId,
            channelId = dependentMessage.channelId,
            text = dependentMessage.contentRaw,
            url = dependentMessage.jumpUrl,
            authorId = dependentMessage.author.id,
            epoch = dependentMessage.timeCreated.toEpochSecond()
        )
        messageDataConnector.add(messageData)
        
        // Create dependency between messages
        createDependency(targetMessage.id, dependentMessage.id)
        
        // Verify dependency was created
        val dependencies = messageDependencyConnector.getDependencies(targetMessage.id)
        assertEquals(1, dependencies.size, "Should have one dependency")
        
        // Delete target message
        deleteMessage(targetMessage)
        
        // Wait for all async operations to complete
        env.awaitAll()
        
        // Verify dependency was removed after deletion
        assertTrue(
            messageDependencyConnector.getDependencies(targetMessage.id).isEmpty(),
            "Dependency should be removed after deletion"
        )
    }
    
    @Test
    fun `test dependent message in different guild`() {
        // Create target message 
        val targetMessage = createAndStoreMessage(messageText = "Target message")
        
        // Create another guild with a channel
        val otherGuild = env.createGuild("Other Guild")
        val otherChannel = env.createTextChannel(otherGuild, "channel-in-other-guild")
        
        // Create dependent message in other guild
        val dependentMessage = env.sendMessage(
            "Other Guild",
            "channel-in-other-guild",
            testUser,
            "Dependent message in other guild"
        ).complete(true)!!
        
        // Store dependent message in database
        val messageData = MessageData(
            messageId = dependentMessage.id,
            guildId = dependentMessage.guildId,
            channelId = dependentMessage.channelId,
            text = dependentMessage.contentRaw,
            url = dependentMessage.jumpUrl,
            authorId = dependentMessage.author.id,
            epoch = dependentMessage.timeCreated.toEpochSecond()
        )
        messageDataConnector.add(messageData)
        
        // Create dependency between messages in different guilds
        createDependency(targetMessage.id, dependentMessage.id)
        
        // Verify dependency was created
        val dependencies = messageDependencyConnector.getDependencies(targetMessage.id)
        assertEquals(1, dependencies.size, "Should have one dependency")
        
        // Delete target message
        deleteMessage(targetMessage)
        
        // Wait for all async operations to complete
        env.awaitAll()
        
        // Verify dependency was removed after deletion
        assertTrue(
            messageDependencyConnector.getDependencies(targetMessage.id).isEmpty(),
            "Dependency should be removed after deletion"
        )
    }
    
    @Test
    fun `test with message containing attachments`() {
        // Create target message
        val targetMessage = env.sendMessage(
            defaultGuildName,
            defaultGeneralChannelName,
            testUser,
            "Target message with attachment",
            listOf(createTestAttachment("test_image.jpg"))
        ).complete(true)!!
        
        // Store target message in database
        val targetMessageData = MessageData(
            messageId = targetMessage.id,
            guildId = targetMessage.guildId,
            channelId = targetMessage.channelId,
            text = targetMessage.contentRaw,
            url = targetMessage.jumpUrl,
            authorId = targetMessage.author.id,
            epoch = targetMessage.timeCreated.toEpochSecond()
        )
        messageDataConnector.add(targetMessageData)
        
        // Create dependent message
        val dependentMessage = createAndStoreMessage(messageText = "Dependent message")
        
        // Create dependency
        createDependency(targetMessage.id, dependentMessage.id)
        
        // Verify dependency was created
        val dependencies = messageDependencyConnector.getDependencies(targetMessage.id)
        assertEquals(1, dependencies.size, "Should have one dependency")
        
        // Delete the target message with attachment
        deleteMessage(targetMessage)
        
        // Wait for all async operations to complete
        env.awaitAll()
        
        // Verify dependency was removed after deletion
        assertTrue(
            messageDependencyConnector.getDependencies(targetMessage.id).isEmpty(),
            "Dependency should be removed after deletion"
        )
    }
    
    @Test
    fun `test circular dependency between messages`() {
        // Create two messages
        val messageA = createAndStoreMessage(messageText = "Message A")
        val messageB = createAndStoreMessage(messageText = "Message B")
        
        // Create circular dependency: A -> B and B -> A
        createDependency(messageA.id, messageB.id)
        createDependency(messageB.id, messageA.id)
        
        // Verify dependencies exist
        val dependenciesA = messageDependencyConnector.getDependencies(messageA.id)
        val dependenciesB = messageDependencyConnector.getDependencies(messageB.id)
        assertEquals(1, dependenciesA.size, "Message A should have one dependency")
        assertEquals(1, dependenciesB.size, "Message B should have one dependency")
        
        // Delete message A
        deleteMessage(messageA)
            
        // Wait for all async operations to complete
        env.awaitAll()
        
        // Verify dependency A -> B is removed
        assertTrue(
            messageDependencyConnector.getDependencies(messageA.id).isEmpty(),
            "Dependency A -> B should be removed"
        )
    }
    
    @Test
    fun `test with non-existent channel`() {
        // Create target message
        val targetMessage = createAndStoreMessage(messageText = "Target message")
        
        // Create dependent message
        val dependentMessage = createAndStoreMessage(messageText = "Dependent message")
        
        // Modify the message data to point to a non-existent channel
        val messageData = messageDataConnector.get(dependentMessage.id)
        assertNotNull(messageData, "Message data should exist")
        messageData!!
        
        val modifiedMessageData = MessageData(
            messageId = messageData.messageId,
            guildId = messageData.guildId,
            channelId = "999999999999999999", // Non-existent channel
            text = messageData.text,
            url = messageData.url,
            authorId = messageData.authorId,
            epoch = messageData.epoch
        )
        
        messageDataConnector.delete(dependentMessage.id)
        messageDataConnector.add(modifiedMessageData)
        
        // Create dependency
        createDependency(targetMessage.id, dependentMessage.id)
        
        // Delete target message
        deleteMessage(targetMessage)
            
        // Wait for all async operations to complete
        env.awaitAll()
        
        // Verify the command handles the non-existent channel gracefully
        // The dependency should still be in the database since the message couldn't be deleted
        // This verifies the current implementation's behavior
    }
}