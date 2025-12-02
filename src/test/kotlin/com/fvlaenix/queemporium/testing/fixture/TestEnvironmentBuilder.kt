package com.fvlaenix.queemporium.testing.fixture

import com.fvlaenix.queemporium.mock.*
import com.fvlaenix.queemporium.testing.time.TimeController
import com.fvlaenix.queemporium.testing.time.VirtualClock
import com.fvlaenix.queemporium.testing.time.VirtualTimeController
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import org.koin.core.context.GlobalContext

class TestEnvironmentBuilder(
  private val fixture: TestFixture,
  private val virtualClock: VirtualClock? = null,
  private val autoStart: Boolean = true
) {
  private val environment = TestEnvironment()
  private val userMap = mutableMapOf<String, TestUser>()
  private val guildMap = mutableMapOf<String, Guild>()
  private val channelMap = mutableMapOf<Pair<String, String>, MessageChannelUnion>()

  val timeController: TimeController? = virtualClock?.let { VirtualTimeController(it) }

  fun build(): TestEnvironment {
    setupUsers()
    setupGuilds()
    setupListeners()

    if (autoStart) {
      environment.start()
    }

    return environment
  }

  private fun setupUsers() {
    fixture.users.forEach { (id, userFixture) ->
      val user = environment.createUser(
        name = userFixture.name,
        isBot = userFixture.isBot,
        discriminator = userFixture.discriminator
      )
      userMap[id] = user
    }
  }

  private fun setupGuilds() {
    fixture.guilds.forEach { guildFixture ->
      val guild = environment.createGuild(guildFixture.name)
      guildMap[guildFixture.id] = guild

      guildFixture.channels.forEach { channelFixture ->
        val channel = environment.createTextChannel(guild, channelFixture.name)
        channelMap[guildFixture.id to channelFixture.id] = channel

        channelFixture.messages.forEach { messageFixture ->
          val author = userMap[messageFixture.author]
            ?: throw IllegalStateException("User ${messageFixture.author} not found. Define users before guilds.")

          val member = environment.createMember(guild, author)

          val message = TestMessage(
            testJda = environment.jda,
            testGuild = guild,
            testChannel = channel,
            idLong = environment.nextId(),
            content = messageFixture.text,
            author = author,
            attachments = emptyList(),
            reactions = mutableListOf(),
            timeCreated = messageFixture.timeCreated ?: java.time.OffsetDateTime.now()
          )

          (channel as TestTextChannel).addMessage(message)

          messageFixture.reactions.forEach { reactionFixture ->
            val emoji = TestEmoji(reactionFixture.emoji)
            reactionFixture.users.forEach { userId ->
              val reactionUser = userMap[userId]
                ?: throw IllegalStateException("User $userId not found for reaction")
              message.addReaction(emoji, reactionUser)
            }
          }
        }
      }
    }
  }

  private fun setupListeners() {
    val koinContext = GlobalContext.getOrNull() ?: return

    koinContext.getAll<net.dv8tion.jda.api.hooks.ListenerAdapter>().forEach { listener ->
      environment.addListener(listener)
    }
  }

  fun getUser(id: String): User = userMap[id]
    ?: throw IllegalStateException("User $id not found in fixture")

  fun getGuild(id: String): Guild = guildMap[id]
    ?: throw IllegalStateException("Guild $id not found in fixture")

  fun getChannel(guildId: String, channelId: String): MessageChannelUnion =
    channelMap[guildId to channelId]
      ?: throw IllegalStateException("Channel $channelId in guild $guildId not found in fixture")
}

fun TestFixture.buildEnvironment(
  virtualClock: VirtualClock? = null,
  autoStart: Boolean = true
): TestEnvironmentBuilder {
  return TestEnvironmentBuilder(this, virtualClock, autoStart)
}

class TestEnvironmentScope(
  val environment: TestEnvironment,
  val timeController: TimeController?,
  private val builder: TestEnvironmentBuilder
) {
  fun user(id: String): User = builder.getUser(id)
  fun guild(id: String): Guild = builder.getGuild(id)
  fun channel(guildId: String, channelId: String): MessageChannelUnion =
    builder.getChannel(guildId, channelId)
}

fun withEnv(
  fixture: TestFixture,
  virtualClock: VirtualClock? = null,
  autoStart: Boolean = true,
  block: TestEnvironmentScope.() -> Unit
): TestEnvironment {
  val builder = fixture.buildEnvironment(virtualClock, autoStart)
  val environment = builder.build()
  val scope = TestEnvironmentScope(environment, builder.timeController, builder)
  scope.block()
  return environment
}
