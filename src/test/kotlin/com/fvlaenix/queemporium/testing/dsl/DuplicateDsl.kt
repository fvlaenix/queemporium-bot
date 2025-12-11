package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.database.MessageDependencyConnector
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.mock.MockDuplicateImageService
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.testing.fixture.awaitAll
import com.fvlaenix.queemporium.testing.trace.ScenarioTraceCollector
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Message

class DuplicateDsl(
  private val setupContext: BotTestSetupContext,
  private val scenarioBuilder: com.fvlaenix.queemporium.testing.scenario.ScenarioBuilder? = null
) {
  private val guildInfoConnector by lazy { GuildInfoConnector(setupContext.databaseConfig.toDatabase()) }
  private val messageDuplicateDataConnector: MessageDuplicateDataConnector
    get() = setupContext.messageDuplicateDataConnector
  private val dependencyConnector: MessageDependencyConnector
    get() = setupContext.messageDependencyConnector
  private val mockService: MockDuplicateImageService
    get() {
      val service = org.koin.core.context.GlobalContext.get().get<DuplicateImageService>()
      return service as? MockDuplicateImageService
        ?: throw IllegalStateException("DuplicateImageService is not MockDuplicateImageService")
    }

  fun configureDuplicateChannel(guildId: String, channelId: String) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "duplicates.configureDuplicateChannel",
        "guildId" to guildId,
        "channelId" to channelId
      )
    )
    guildInfoConnector.setDuplicateInfo(guildId, channelId)
    ScenarioTraceCollector.logDslDbCheck(
      mapOf(
        "check" to "guildInfo.duplicateChannel",
        "guildId" to guildId,
        "channelId" to channelId
      )
    )
  }

  fun excludeChannel(guildId: String, channelId: String) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "duplicates.excludeChannel",
        "guildId" to guildId,
        "channelId" to channelId
      )
    )
    guildInfoConnector.addExcludingChannel(guildId, channelId)
  }

  fun sendMessage(
    guildId: String,
    channelId: String,
    userId: String,
    text: String,
    attachments: List<Message.Attachment> = emptyList()
  ): Message? {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "duplicates.sendMessage",
        "guildId" to guildId,
        "channelId" to channelId,
        "userId" to userId,
        "text" to text,
        "attachments" to attachments.map { it.fileName }
      )
    )
    return if (scenarioBuilder != null) {
      scenarioBuilder.sendMessage(guildId, channelId, userId, text, attachments)
      null
    } else {
      val guild = setupContext.guild(guildId)
      val channel = setupContext.channel(guild, channelId)
      val user = setupContext.resolveUser(userId)
      val msg = setupContext.envWithTime.environment.sendMessage(
        guildName = guild.name,
        channelName = channel.name,
        user = user,
        message = text,
        attachments = attachments
      ).complete(true)
      msg
    }
  }

  fun awaitProcessing(description: String = "duplicates.awaitProcessing") {
    ScenarioTraceCollector.logDslAction(
      mapOf("action" to "duplicates.awaitProcessing", "description" to description)
    )
    if (scenarioBuilder != null) {
      scenarioBuilder.awaitAll(description)
    } else {
      runBlocking { setupContext.envWithTime.awaitAll() }
    }
  }

  fun stubResponse(
    fileName: String,
    build: DuplicateResponseBuilder.() -> Unit
  ) {
    val builder = DuplicateResponseBuilder()
    builder.build()
    val responses = builder.entries.map {
      DuplicateImageService.DuplicateImageData(
        messageId = it.messageId,
        numberInMessage = it.numberInMessage,
        additionalImageInfo = it.additionalImageInfo,
        level = it.level
      )
    }
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "duplicates.stubResponse",
        "fileName" to fileName,
        "responses" to responses.map { resp ->
          mapOf(
            "messageId" to resp.messageId,
            "numberInMessage" to resp.numberInMessage,
            "fileName" to resp.additionalImageInfo.fileName,
            "level" to resp.level
          )
        }
      )
    )
    mockService.setResponseForFile(fileName, responses)
  }

  fun stubNoResponse(fileName: String) {
    ScenarioTraceCollector.logDslAction(
      mapOf(
        "action" to "duplicates.stubNoResponse",
        "fileName" to fileName
      )
    )
    mockService.setResponseForFile(fileName, null)
  }

  fun countAddImageRequests(): Int {
    return mockService.countAddImageRequests()
  }

  fun expectReported(
    originalMessage: Message,
    duplicateMessage: Message
  ) {
    expectReported(originalMessage.id, duplicateMessage.id)
  }

  fun expectReported(
    originalMessageId: String,
    duplicateMessageId: String
  ) {
    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "duplicates.expectReported",
          "originalMessageId" to originalMessageId,
          "duplicateMessageId" to duplicateMessageId
        )
      )
      val depsOriginal = dependencyConnector.getDependencies(originalMessageId)
      val depsDuplicate = dependencyConnector.getDependencies(duplicateMessageId)
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "messageDependency.forOriginal",
          "originalMessageId" to originalMessageId,
          "dependencies" to depsOriginal
        )
      )
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "messageDependency.forDuplicate",
          "duplicateMessageId" to duplicateMessageId,
          "dependencies" to depsDuplicate
        )
      )
      if (depsOriginal.isEmpty() && depsDuplicate.isEmpty()) {
        throw AssertionError("No dependencies found for original $originalMessageId or duplicate $duplicateMessageId")
      }
      val duplicateData = messageDuplicateDataConnector.get(duplicateMessageId)
      if (duplicateData == null) {
        throw AssertionError("MessageDuplicateData not stored for duplicate $duplicateMessageId")
      }
    }
    scenarioBuilder?.expect("duplicates.expectReported") { assertion() } ?: runBlocking { assertion() }
  }

  fun expectNoReport(messageId: String) {
    val assertion: suspend () -> Unit = {
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "duplicates.expectNoReport",
          "messageId" to messageId
        )
      )
      val deps = dependencyConnector.getDependencies(messageId)
      val duplicateData = messageDuplicateDataConnector.get(messageId)
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "messageDependency.forMessage",
          "messageId" to messageId,
          "dependencies" to deps
        )
      )
      if (deps.isNotEmpty()) {
        throw AssertionError("Expected no dependencies for $messageId, found $deps")
      }
      if (duplicateData != null) {
        throw AssertionError("Expected no duplicate data for $messageId, found $duplicateData")
      }
    }
    scenarioBuilder?.expect("duplicates.expectNoReport") { assertion() } ?: runBlocking { assertion() }
  }

  fun expectNoReport(message: Message) = expectNoReport(message.id)

  fun expectDependencies(
    targetMessageId: String,
    block: DependencyExpectation.() -> Unit
  ) {
    val expectation = DependencyExpectation().apply(block)
    val assertion: suspend () -> Unit = {
      val deps = dependencyConnector.getDependencies(targetMessageId)
      ScenarioTraceCollector.logDslAssert(
        mapOf(
          "assert" to "duplicates.expectDependencies",
          "targetMessageId" to targetMessageId,
          "expectedContains" to expectation.containsIds
        )
      )
      ScenarioTraceCollector.logDslDbCheck(
        mapOf(
          "check" to "messageDependency.forMessage",
          "messageId" to targetMessageId,
          "dependencies" to deps
        )
      )
      expectation.containsIds.forEach { expected ->
        if (!deps.contains(expected)) {
          throw AssertionError("Expected dependency $expected for $targetMessageId not found. Actual: $deps")
        }
      }
    }
    scenarioBuilder?.expect("duplicates.expectDependencies") { assertion() } ?: runBlocking { assertion() }
  }
}

class DuplicateResponseBuilder {
  internal val entries = mutableListOf<DuplicateEntry>()

  fun duplicate(
    messageId: String,
    numberInMessage: Int = 0,
    additionalImageInfo: com.fvlaenix.queemporium.database.AdditionalImageInfo,
    level: Long = 90
  ) {
    entries.add(DuplicateEntry(messageId, numberInMessage, additionalImageInfo, level))
  }
}

data class DuplicateEntry(
  val messageId: String,
  val numberInMessage: Int,
  val additionalImageInfo: com.fvlaenix.queemporium.database.AdditionalImageInfo,
  val level: Long
)

class DependencyExpectation {
  internal val containsIds = mutableListOf<String>()
  fun contains(messageId: String) {
    containsIds.add(messageId)
  }
}
