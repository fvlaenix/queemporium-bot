package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.CheckImageResponseImagesInfoKt.checkImageResponseImageInfo
import com.fvlaenix.duplicate.protobuf.GetCompressionSizeResponse
import com.fvlaenix.duplicate.protobuf.addImageResponse
import com.fvlaenix.duplicate.protobuf.addImageResponseOk
import com.fvlaenix.duplicate.protobuf.checkImageResponseImagesInfo
import com.fvlaenix.queemporium.builder.createEnvironment
import com.fvlaenix.queemporium.commands.duplicate.OnlinePictureCompare
import com.fvlaenix.queemporium.database.AdditionalImageInfo
import com.fvlaenix.queemporium.mock.createTestAttachment
import com.fvlaenix.queemporium.verification.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Test for verifying the bot correctly handles excluded channels.
 */
class DuplicateServiceExcludedChannelsTest : BaseGrpcTest() {

    override fun getCommandsForTest(): Array<KClass<*>> {
        return arrayOf(OnlinePictureCompare::class)
    }

    @Test
    fun `test bot ignores excluded channels`() {
        // Configure the compression size response
        duplicateService.compressionSizeResponse = GetCompressionSizeResponse.newBuilder()
            .setX(800)
            .build()
            
        // Create test environment with channels
        val env = createEnvironment {
            createGuild("Test Guild") {
                withChannel("general")
                withChannel("duplicate-channel")
                withChannel("excluded-channel")
            }
        }
        
        // Configure test environment
        val guildInfoConnector = getGuildInfoConnector()
        val testGuild = env.jda.getGuildsByName("Test Guild", false).first()
        val duplicateChannel = testGuild.getTextChannelsByName("duplicate-channel", false).first()
        val excludedChannel = testGuild.getTextChannelsByName("excluded-channel", false).first()
        
        // Set duplicate channel
        guildInfoConnector.setDuplicateInfo(testGuild.id, duplicateChannel.id)
        
        // Add excluded channel
        guildInfoConnector.addExcludedChannel(testGuild.id, excludedChannel.id)
        
        // Create a user
        val user = env.createUser("Test User", false)
        
        // Configure duplicate response that would trigger if the message was processed
        val additionalInfo = AdditionalImageInfo(
            fileName = "test.jpg", 
            isSpoiler = false,
            originalSizeWidth = 100, 
            originalSizeHeight = 100
        )
        
        duplicateService.addImageResponse = addImageResponse {
            this.responseOk = addImageResponseOk {
                this.isAdded = true
                this.imageInfo = checkImageResponseImagesInfo {
                    this.images.add(
                        checkImageResponseImageInfo {
                            this.messageId = "some_original_id"
                            this.numberInMessage = 0
                            this.additionalInfo = Json.encodeToString(additionalInfo)
                            this.level = 95
                        }
                    )
                }
            }
        }
            
        // Send a message to the excluded channel
        env.sendMessage(
            "Test Guild",
            "excluded-channel",
            user,
            "Message in excluded channel",
            listOf(createTestAttachment("test_image.jpg"))
        )
        
        // Wait for processing
        env.awaitAll()
        
        // Verify that no duplicate detection message was sent
        answerService.verify {
            // Should have no messages, as excluded channels are ignored
            messageCount(0)
        }
        
        // Verify that no service requests were made
        val initialRequestsCount = duplicateService.requests.size
        
        // For comparison, send a message to a normal channel
        env.sendMessage(
            "Test Guild",
            "general",
            user,
            "Message in normal channel",
            listOf(createTestAttachment("test_image2.jpg"))
        )
        
        // Wait for processing
        env.awaitAll()
        
        // There should be new requests for the message in the normal channel
        assert(duplicateService.requests.size > initialRequestsCount) { 
            "Should process messages in normal channels but not in excluded channels" 
        }
    }
}