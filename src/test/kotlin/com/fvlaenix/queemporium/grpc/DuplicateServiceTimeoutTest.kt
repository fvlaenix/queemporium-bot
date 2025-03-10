package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.verification.verify
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.Ignore

/**
 * Test for verifying the bot properly handles timeout from DuplicateImagesService.
 */
class DuplicateServiceTimeoutTest : BaseGrpcTest() {

    override fun getCommandsForTest(): Array<KClass<*>> {
        return arrayOf(OnlinePictureCompare::class)
    }

    @Test
    @Ignore("Not supported yet")
    fun `test bot handles service timeout`() {
        // Configure the compression size response
        duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
            .setX(800)
            .build()
            
        // Create test environment
        val env = createEnvironment {
            createGuild("Test Guild") {
                withChannel("general")
                withChannel("duplicate-channel")
            }
        }
        
        // Configure duplicate detection channel
        val guildInfoConnector = getGuildInfoConnector()
        val testGuild = env.jda.getGuildsByName("Test Guild", false).first()
        val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()
        guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)
        
        // Simulate server timeout with a short duration for testing
        simulateServerTimeout(2000) // 2 seconds timeout
        
        // Create a user
        val user = env.createUser("Test User", false)
        
        // Send a message that should trigger a timeout
        env.sendMessage(
            "Test Guild",
            "general",
            user,
            "This message should trigger a timeout",
            listOf(createTestAttachment("timeout_test.jpg"))
        )
        
        // Wait for processing
        env.awaitAll()
        
        // Verify that the bot handled the timeout
        answerService.verify {
            // Should have at least one message
            messageCount(1)
            
            // Message should indicate service unavailability
            messagesContain("unavailable")
        }
    }
}