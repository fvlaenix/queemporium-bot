package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.commands.advent.AdventDataConnector
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.*
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.mock.TestTextChannel
import com.fvlaenix.queemporium.service.MockAnswerService
import com.fvlaenix.queemporium.testing.fixture.FixtureBuilder
import com.fvlaenix.queemporium.testing.fixture.TestEnvironmentWithTime
import com.fvlaenix.queemporium.testing.fixture.setupWithFixture
import com.fvlaenix.queemporium.testing.fixture.setupWithFixtureAndModules
import com.fvlaenix.queemporium.testing.helpers.LoggerTestContext
import com.fvlaenix.queemporium.testing.scenario.ScenarioBuilder
import com.fvlaenix.queemporium.testing.time.VirtualClock
import kotlinx.coroutines.runBlocking
import java.time.Instant

@DslMarker
annotation class BotTestDsl

// TODO remove koin.inject and so on from test classes
@BotTestDsl
class BotTestContext {
  internal val fixtureBuilder = FixtureBuilder()
  internal var virtualClock: VirtualClock? = null
  internal var answerService: MockAnswerService? = null
  internal val setupActions = mutableListOf<BotTestSetupContext.() -> Unit>()
  internal val scenarioSteps = mutableListOf<suspend BotTestScenarioContext.() -> Unit>()
  internal val beforeFeatureLoadModules = mutableListOf<org.koin.core.module.Module>()

  internal var envWithTime: TestEnvironmentWithTime? = null
  internal var databaseConfig: DatabaseConfiguration? = null
  internal var messageDataConnector: MessageDataConnector? = null

  internal var _loggerContext: LoggerTestContext? = null

  fun before(block: FixtureBuilder.() -> Unit) {
    fixtureBuilder.block()
  }

  fun withVirtualTime(startTime: Instant = Instant.now()) {
    virtualClock = VirtualClock(startTime)
  }

  fun withAnswerService(service: MockAnswerService = MockAnswerService()) {
    answerService = service
  }

  fun setup(block: BotTestSetupContext.() -> Unit) {
    setupActions.add(block)
  }

  fun scenario(block: suspend BotTestScenarioContext.() -> Unit) {
    scenarioSteps.add(block)
  }

  fun registerModuleBeforeFeatureLoad(module: org.koin.core.module.Module) {
    beforeFeatureLoadModules.add(module)
  }
}

@BotTestDsl
class BotTestScenarioContext(private val setupContext: BotTestSetupContext) {
  private val scenarioBuilder = ScenarioBuilder()

  private val hallOfFameDsl by lazy { setupContext.hallOfFameWithScenario(scenarioBuilder) }
  val hallOfFame: HallOfFameDsl
    get() = hallOfFameDsl

  private val adventDsl by lazy { setupContext.adventWithScenario(scenarioBuilder) }
  val advent: AdventDsl
    get() = adventDsl

  private val reactionsDsl by lazy { ReactionsDsl(setupContext, scenarioBuilder) }
  val reactions: ReactionsDsl
    get() = reactionsDsl

  private val duplicatesDsl by lazy { setupContext.duplicatesWithScenario(scenarioBuilder) }
  val duplicates: DuplicateDsl
    get() = duplicatesDsl

  val logger: LoggerTestContext
    get() = setupContext.logger

  val answerService: MockAnswerService?
    get() = setupContext.answerService

  val envWithTime: TestEnvironmentWithTime
    get() = setupContext.envWithTime

  val koin: org.koin.core.Koin
    get() = org.koin.core.context.GlobalContext.get()

  fun sendMessage(
    guildId: String,
    channelId: String,
    userId: String,
    text: String,
    attachments: List<net.dv8tion.jda.api.entities.Message.Attachment> = emptyList()
  ) {
    scenarioBuilder.sendMessage(guildId, channelId, userId, text, attachments)
  }

  fun addReaction(messageRef: com.fvlaenix.queemporium.testing.scenario.MessageRef, emoji: String, userId: String) {
    scenarioBuilder.addReaction(messageRef, emoji, userId)
  }

  fun addReaction(guildId: String, channelId: String, messageIndex: Int, emoji: String, userId: String) {
    scenarioBuilder.addReaction(guildId, channelId, messageIndex, emoji, userId)
  }

  fun advanceTime(duration: kotlin.time.Duration) {
    scenarioBuilder.advanceTime(duration)
  }

  fun awaitAll(description: String = "await all jobs") {
    scenarioBuilder.awaitAll(description)
  }

  fun expect(
    description: String = "expectation",
    block: suspend com.fvlaenix.queemporium.testing.scenario.ExpectContext.() -> Unit
  ) {
    scenarioBuilder.expect(description, block)
  }

  fun messageRef(guildId: String, channelId: String, index: Int): com.fvlaenix.queemporium.testing.scenario.MessageRef {
    return scenarioBuilder.messageRef(guildId, channelId, index)
  }

  fun guild(nameOrId: String): net.dv8tion.jda.api.entities.Guild {
    return GuildResolver.resolve(envWithTime.environment.jda, nameOrId)
  }

  fun channel(
    guild: net.dv8tion.jda.api.entities.Guild,
    nameOrId: String
  ): net.dv8tion.jda.api.entities.channel.concrete.TextChannel {
    return ChannelResolver.resolve(guild, nameOrId)
  }

  fun channel(
    guildNameOrId: String,
    channelNameOrId: String
  ): net.dv8tion.jda.api.entities.channel.concrete.TextChannel {
    val g = guild(guildNameOrId)
    return channel(g, channelNameOrId)
  }

  fun message(
    channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel,
    index: Int = 0,
    order: MessageOrder = MessageOrder.OLDEST_FIRST
  ): net.dv8tion.jda.api.entities.Message {
    return MessageResolver.resolve(channel, index, order)
  }

  fun message(
    guildName: String,
    channelName: String,
    index: Int = 0,
    order: MessageOrder = MessageOrder.OLDEST_FIRST
  ): net.dv8tion.jda.api.entities.Message {
    val ch = channel(guildName, channelName)
    return message(ch, index, order)
  }

  fun messageById(
    channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel,
    messageId: String
  ): net.dv8tion.jda.api.entities.Message {
    return MessageResolver.resolveById(channel, messageId)
  }

  fun messageById(
    guildName: String,
    channelName: String,
    messageId: String
  ): net.dv8tion.jda.api.entities.Message {
    val ch = channel(guildName, channelName)
    return messageById(ch, messageId)
  }

  fun latestMessage(channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel): net.dv8tion.jda.api.entities.Message {
    return message(channel, 0, MessageOrder.NEWEST_FIRST)
  }

  fun firstMessage(channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel): net.dv8tion.jda.api.entities.Message {
    return message(channel, 0, MessageOrder.OLDEST_FIRST)
  }

  fun resolveUser(idOrName: String): net.dv8tion.jda.api.entities.User {
    envWithTime.userMap[idOrName]?.let { return it }

    envWithTime.environment.jda.users.find { it.id == idOrName || it.name == idOrName }?.let { return it }

    envWithTime.environment.jda.guilds
      .flatMap { it.members }
      .map { it.user }
      .firstOrNull { it.id == idOrName || it.name == idOrName }
      ?.let { return it }

    val user = envWithTime.environment.createUser(idOrName, false)
    envWithTime.environment.jda.guilds.firstOrNull()?.let { guild ->
      envWithTime.environment.createMember(guild, user)
    }
    return user
  }

  internal fun build(): List<com.fvlaenix.queemporium.testing.scenario.ScenarioStep> = scenarioBuilder.build()
}

@BotTestDsl
class BotTestSetupContext(
  val envWithTime: TestEnvironmentWithTime,
  val answerService: MockAnswerService?,
  val databaseConfig: DatabaseConfiguration,
  val messageDataConnector: MessageDataConnector,
  private val parentContext: BotTestContext
) {
  val emojiDataConnector: EmojiDataConnector by lazy {
    EmojiDataConnector(databaseConfig.toDatabase())
  }

  val messageEmojiDataConnector: MessageEmojiDataConnector by lazy {
    MessageEmojiDataConnector(databaseConfig.toDatabase())
  }

  val messageDuplicateDataConnector: MessageDuplicateDataConnector by lazy {
    MessageDuplicateDataConnector(databaseConfig.toDatabase())
  }

  val hallOfFameConnector: HallOfFameConnector by lazy {
    HallOfFameConnector(databaseConfig.toDatabase())
  }

  val messageDependencyConnector: MessageDependencyConnector by lazy {
    MessageDependencyConnector(databaseConfig.toDatabase())
  }

  private val hallOfFameDsl by lazy { HallOfFameDsl(this) }
  private val duplicatesDsl by lazy { DuplicateDsl(this) }
  val adventDataConnector: AdventDataConnector by lazy {
    AdventDataConnector(databaseConfig.toDatabase())
  }

  private val adventDsl by lazy { AdventDsl(this) }

  val hallOfFame: HallOfFameDsl
    get() = hallOfFameDsl

  val duplicates: DuplicateDsl
    get() = duplicatesDsl

  val advent: AdventDsl
    get() = adventDsl

  val logger: LoggerTestContext by lazy {
    parentContext._loggerContext ?: run {
      val context = LoggerTestContext()
      parentContext._loggerContext = context
      context
    }
  }

  fun getMessage(guildId: String, channelId: String, messageIndex: Int): String {
    val guild = guild(guildId)
    val channel = channel(guild, channelId)
    val message = MessageResolver.resolve(channel, messageIndex, MessageOrder.OLDEST_FIRST)
    return message.id
  }

  fun getMessages(guildId: String, channelId: String): List<String> {
    val guild = guild(guildId)
    val channel = channel(guild, channelId)
    val testChannel = channel as? TestTextChannel
      ?: throw IllegalStateException("Channel ${channel.name} is not a test text channel")
    return testChannel.messages.map { it.id }
  }

  fun guild(nameOrId: String): net.dv8tion.jda.api.entities.Guild {
    return GuildResolver.resolve(envWithTime.environment.jda, nameOrId)
  }

  fun channel(
    guild: net.dv8tion.jda.api.entities.Guild,
    nameOrId: String
  ): net.dv8tion.jda.api.entities.channel.concrete.TextChannel {
    return ChannelResolver.resolve(guild, nameOrId)
  }

  fun channel(
    guildNameOrId: String,
    channelNameOrId: String
  ): net.dv8tion.jda.api.entities.channel.concrete.TextChannel {
    val g = guild(guildNameOrId)
    return channel(g, channelNameOrId)
  }

  fun message(
    channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel,
    index: Int = 0,
    order: MessageOrder = MessageOrder.OLDEST_FIRST
  ): net.dv8tion.jda.api.entities.Message {
    return MessageResolver.resolve(channel, index, order)
  }

  fun message(
    guildName: String,
    channelName: String,
    index: Int = 0,
    order: MessageOrder = MessageOrder.OLDEST_FIRST
  ): net.dv8tion.jda.api.entities.Message {
    val ch = channel(guildName, channelName)
    return message(ch, index, order)
  }

  fun messageById(
    channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel,
    messageId: String
  ): net.dv8tion.jda.api.entities.Message {
    return MessageResolver.resolveById(channel, messageId)
  }

  fun messageById(
    guildName: String,
    channelName: String,
    messageId: String
  ): net.dv8tion.jda.api.entities.Message {
    val ch = channel(guildName, channelName)
    return messageById(ch, messageId)
  }

  fun latestMessage(channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel): net.dv8tion.jda.api.entities.Message {
    return message(channel, 0, MessageOrder.NEWEST_FIRST)
  }

  fun firstMessage(channel: net.dv8tion.jda.api.entities.channel.middleman.MessageChannel): net.dv8tion.jda.api.entities.Message {
    return message(channel, 0, MessageOrder.OLDEST_FIRST)
  }

  fun resolveUser(idOrName: String): net.dv8tion.jda.api.entities.User {
    envWithTime.userMap[idOrName]?.let { return it }

    envWithTime.environment.jda.users.find { it.id == idOrName || it.name == idOrName }?.let { return it }

    envWithTime.environment.jda.guilds
      .flatMap { it.members }
      .map { it.user }
      .firstOrNull { it.id == idOrName || it.name == idOrName }
      ?.let { return it }

    val user = envWithTime.environment.createUser(idOrName, false)
    envWithTime.environment.jda.guilds.firstOrNull()?.let { guild ->
      envWithTime.environment.createMember(guild, user)
    }
    return user
  }

  internal fun hallOfFameWithScenario(
    scenarioBuilder: ScenarioBuilder
  ): HallOfFameDsl {
    return HallOfFameDsl(this, scenarioBuilder)
  }

  internal fun duplicatesWithScenario(
    scenarioBuilder: ScenarioBuilder
  ): DuplicateDsl {
    return DuplicateDsl(this, scenarioBuilder)
  }

  internal fun adventWithScenario(
    scenarioBuilder: ScenarioBuilder
  ): AdventDsl {
    return AdventDsl(this, scenarioBuilder)
  }
}

fun BaseKoinTest.testBot(block: BotTestContext.() -> Unit) = runBlocking {
  val context = BotTestContext()
  context.block()

  if (context.answerService == null) {
    context.withAnswerService()
  }

  val testFixture = context.fixtureBuilder.build()

  val envWithTime = setupWithFixture(testFixture, context.virtualClock) { builder ->
    builder.answerService = context.answerService!!
  }

  context.envWithTime = envWithTime

  val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
  context.databaseConfig = databaseConfig

  val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())
  context.messageDataConnector = messageDataConnector

  autoPopulateMessageData(envWithTime, messageDataConnector)

  // Automatically populate author data into the database
  val authorDataConnector = AuthorDataConnector(databaseConfig.toDatabase())
  autoPopulateAuthorData(envWithTime, authorDataConnector)

  // Automatically populate emoji/reaction data into the database
  val emojiDataConnector = EmojiDataConnector(databaseConfig.toDatabase())
  autoPopulateEmojiData(envWithTime, emojiDataConnector)

  val setupContext = BotTestSetupContext(
    envWithTime = envWithTime,
    answerService = context.answerService,
    databaseConfig = databaseConfig,
    messageDataConnector = messageDataConnector,
    parentContext = context
  )

  context.setupActions.forEach { action ->
    setupContext.action()
  }

  try {
    for (scenarioBlock in context.scenarioSteps) {
      val scenarioContext = BotTestScenarioContext(setupContext)
      scenarioContext.scenarioBlock()
      val steps = scenarioContext.build()
      val runner = com.fvlaenix.queemporium.testing.scenario.ScenarioRunner(
        envWithTime.environment,
        envWithTime.timeController,
        envWithTime,
        context.answerService
      )
      runner.run(steps)
    }
  } finally {
    context._loggerContext?.cleanup()
  }
}

class BotTestFixture(private val context: BotTestContext) {
  private var setupContext: BotTestSetupContext? = null
  var autoStart: Boolean = true

  val setup: BotTestSetupContext
    get() = setupContext ?: throw IllegalStateException("Fixture not initialized. Call initialize() first.")

  suspend fun initialize(koinTest: BaseKoinTest) {
    if (context.answerService == null) {
      context.withAnswerService()
    }

    if (context._loggerContext == null) {
      context._loggerContext = LoggerTestContext()
    }

    val testFixture = context.fixtureBuilder.build()

    val envWithTime = setupWithFixtureAndModules(
      testFixture,
      context.virtualClock,
      autoStart,
      context.beforeFeatureLoadModules
    ) { builder ->
      builder.answerService = context.answerService!!
    }

    context.envWithTime = envWithTime

    val databaseConfig: DatabaseConfiguration = org.koin.core.context.GlobalContext.get().get()
    context.databaseConfig = databaseConfig

    val messageDataConnector = MessageDataConnector(databaseConfig.toDatabase())
    context.messageDataConnector = messageDataConnector

    autoPopulateMessageData(envWithTime, messageDataConnector)

    // Automatically populate author data into the database
    val authorDataConnector = AuthorDataConnector(databaseConfig.toDatabase())
    autoPopulateAuthorData(envWithTime, authorDataConnector)

    // Automatically populate emoji/reaction data into the database
    val emojiDataConnector = EmojiDataConnector(databaseConfig.toDatabase())
    autoPopulateEmojiData(envWithTime, emojiDataConnector)

    val setupCtx = BotTestSetupContext(
      envWithTime = envWithTime,
      answerService = context.answerService,
      databaseConfig = databaseConfig,
      messageDataConnector = messageDataConnector,
      parentContext = context
    )

    context.setupActions.forEach { action ->
      setupCtx.action()
    }

    setupContext = setupCtx
  }

  suspend fun runScenario(block: suspend BotTestScenarioContext.() -> Unit) {
    val setupCtx = setupContext ?: throw IllegalStateException("Fixture not initialized. Call initialize() first.")

    val scenarioContext = BotTestScenarioContext(setupCtx)
    scenarioContext.block()
    val steps = scenarioContext.build()
    val runner = com.fvlaenix.queemporium.testing.scenario.ScenarioRunner(
      setupCtx.envWithTime.environment,
      setupCtx.envWithTime.timeController,
      setupCtx.envWithTime,
      context.answerService
    )
    runner.run(steps)
  }

  fun cleanup() {
    context._loggerContext?.cleanup()
  }
}

fun testBotFixture(block: BotTestContext.() -> Unit): BotTestFixture {
  val context = BotTestContext()
  context.block()
  return BotTestFixture(context)
}

private fun autoPopulateMessageData(
  envWithTime: TestEnvironmentWithTime,
  messageDataConnector: MessageDataConnector
) {
  var firstMsg = true
  envWithTime.environment.jda.guilds.forEach { guild ->
    guild.channels.filterIsInstance<TestTextChannel>().forEach { channel ->
      channel.messages.forEach { msg ->
        val epochMillis = msg.timeCreated.toEpochSecond() * 1000
        if (firstMsg) {
          println("DEBUG autoPopulateMessageData: first message timeCreated=${msg.timeCreated}, toEpochSecond=${msg.timeCreated.toEpochSecond()}, epochMillis=$epochMillis")
          firstMsg = false
        }
        messageDataConnector.add(
          MessageData(
            messageId = msg.id,
            guildId = guild.id,
            channelId = channel.id,
            text = msg.contentRaw,
            url = msg.jumpUrl,
            authorId = msg.author.id,
            epoch = epochMillis
          )
        )
      }
    }
  }
}

private fun autoPopulateAuthorData(
  envWithTime: TestEnvironmentWithTime,
  authorDataConnector: AuthorDataConnector
) {
  envWithTime.environment.jda.guilds.forEach { guild ->
    val authors = mutableSetOf<AuthorData>()

    // Collect authors from all messages in the guild
    guild.channels.filterIsInstance<TestTextChannel>().forEach { channel ->
      channel.messages.forEach { msg ->
        authors.add(
          AuthorData(
            authorId = msg.author.id,
            guildId = guild.id,
            authorName = msg.author.name
          )
        )
      }
    }

    // Use replaceAuthors to populate the database
    if (authors.isNotEmpty()) {
      authorDataConnector.replaceAuthors(authors.toList(), guild.id)
    }
  }
}

private fun autoPopulateEmojiData(
  envWithTime: TestEnvironmentWithTime,
  emojiDataConnector: EmojiDataConnector
) {
  val emojiDataList = mutableListOf<EmojiData>()

  envWithTime.environment.jda.guilds.forEach { guild ->
    guild.channels.filterIsInstance<TestTextChannel>().forEach { channel ->
      channel.messages.forEach { msg ->
        msg.reactions.forEach { reaction ->
          reaction.retrieveUsers().complete().forEach { user ->
            emojiDataList.add(
              EmojiData(
                messageId = msg.id,
                emojiId = reaction.emoji.name,
                authorId = user.id
              )
            )
          }
        }
      }
    }
  }

  if (emojiDataList.isNotEmpty()) {
    emojiDataConnector.insert(emojiDataList)
  }
}
