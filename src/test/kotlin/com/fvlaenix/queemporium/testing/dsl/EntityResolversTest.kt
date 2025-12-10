package com.fvlaenix.queemporium.testing.dsl

import com.fvlaenix.queemporium.mock.TestTextChannel
import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.requests.RestAction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EntityResolversTest {

  // ========================================
  // GuildResolver Tests
  // ========================================

  @Test
  fun `GuildResolver should resolve by ID`() {
    val jda = mockk<JDA>()
    val guild = mockk<Guild>()
    every { guild.id } returns "123"
    every { jda.getGuildById("123") } returns guild

    val result = GuildResolver.resolve(jda, "123")
    assertEquals(guild, result)
  }

  @Test
  fun `GuildResolver should resolve by exact name`() {
    val jda = mockk<JDA>()
    val guild = mockk<Guild>()
    every { guild.id } returns "123"
    every { jda.getGuildsByName("TestGuild", false) } returns listOf(guild)

    val result = GuildResolver.resolve(jda, "TestGuild")
    assertEquals(guild, result)
  }

  @Test
  fun `GuildResolver should resolve by loose name`() {
    val jda = mockk<JDA>()
    val guild = mockk<Guild>()
    every { guild.id } returns "123"
    every { jda.getGuildsByName("testguild", false) } returns emptyList()
    every { jda.getGuildsByName("testguild", true) } returns listOf(guild)

    val result = GuildResolver.resolve(jda, "testguild")
    assertEquals(guild, result)
  }

  @Test
  fun `GuildResolver should fail when ambiguous`() {
    val jda = mockk<JDA>()
    val guild1 = mockk<Guild> {
      every { name } returns "TestGuild"
      every { id } returns "1"
    }
    val guild2 = mockk<Guild> {
      every { name } returns "TestGuild"
      every { id } returns "2"
    }
    every { jda.getGuildsByName("TestGuild", false) } returns listOf(guild1, guild2)

    val exception = assertFailsWith<IllegalStateException> {
      GuildResolver.resolve(jda, "TestGuild")
    }
    assertTrue(exception.message!!.contains("Ambiguous Guild"))
  }

  @Test
  fun `GuildResolver should fail when not found`() {
    val jda = mockk<JDA>()
    every { jda.getGuildsByName("Unknown", false) } returns emptyList()
    every { jda.getGuildsByName("Unknown", true) } returns emptyList()
    every { jda.guilds } returns emptyList()

    assertFailsWith<IllegalStateException> {
      GuildResolver.resolve(jda, "Unknown")
    }
  }

  // ========================================
  // ChannelResolver Tests
  // ========================================

  @Test
  fun `ChannelResolver should resolve by ID`() {
    val guild = mockk<Guild>()
    val channel = mockk<TextChannel>()
    every { channel.id } returns "456"
    every { guild.getTextChannelById("456") } returns channel

    val result = ChannelResolver.resolve(guild, "456")
    assertEquals(channel, result)
  }

  @Test
  fun `ChannelResolver should resolve by exact name`() {
    val guild = mockk<Guild>()
    val channel = mockk<TextChannel>()
    every { channel.id } returns "456"
    every { guild.getTextChannelsByName("general", false) } returns listOf(channel)

    val result = ChannelResolver.resolve(guild, "general")
    assertEquals(channel, result)
  }

  @Test
  fun `ChannelResolver should resolve by loose name`() {
    val guild = mockk<Guild>()
    val channel = mockk<TextChannel>()
    every { channel.id } returns "456"
    every { guild.getTextChannelsByName("GENERAL", false) } returns emptyList()
    every { guild.getTextChannelsByName("GENERAL", true) } returns listOf(channel)

    val result = ChannelResolver.resolve(guild, "GENERAL")
    assertEquals(channel, result)
  }

  @Test
  fun `ChannelResolver should fail when ambiguous`() {
    val guild = mockk<Guild>()
    val ch1 = mockk<TextChannel> {
      every { name } returns "general"
      every { id } returns "1"
    }
    val ch2 = mockk<TextChannel> {
      every { name } returns "general"
      every { id } returns "2"
    }
    every { guild.getTextChannelsByName("general", false) } returns listOf(ch1, ch2)

    val exception = assertFailsWith<IllegalStateException> {
      ChannelResolver.resolve(guild, "general")
    }
    assertTrue(exception.message!!.contains("Ambiguous TextChannel"))
  }

  // ========================================
  // MessageResolver Tests
  // ========================================

  @Test
  fun `MessageResolver should resolve by index (TestTextChannel)`() {
    val channel = mockk<TestTextChannel>(relaxed = true)
    val msg1 = mockk<Message> { every { id } returns "msg1" }
    val msg2 = mockk<Message> { every { id } returns "msg2" }

    // Direct field access isn't easy with mockk if it's not a property backing field or if we don't own the class
    // But TestTextChannel has 'messages' as an internal property.
    // However, since we mock the class, we need to mock the property getter if it has one or generic usage.
    // 'messages' is a field in TestTextChannel. Since TestTextChannel is a class we are mocking, 
    // we should use a real instance or spy if we want to access the real 'messages' list.
    // Let's rely on 'every { channel.messages }' if it was accessible, but it's a property.
    // Since 'messages' is internal mutable list, mocking get() might work if Kotlin compiles it to getter.
    // Actually TestTextChannel is in another package. 'messages' is internal.
    // If I cannot access 'messages' directly on a mock, I might need to use a real TestTextChannel or spy.
    // But TestTextChannel constructor is complex.

    // Workaround: In the actual code, we cast to TestTextChannel and access .messages.
    // If I mock TestTextChannel, I must ensure .messages returns what I want.
    // Since 'messages' is a val with a backing field, every { channel.messages } might not work if it is final.
    // Let's try mocking the property.

    every { channel.messages } returns mutableListOf(msg1, msg2)
    every { channel.name } returns "test-channel"

    val resultOldest = MessageResolver.resolve(channel, 0, MessageOrder.OLDEST_FIRST)
    assertEquals(msg1, resultOldest)

    val resultNewest = MessageResolver.resolve(channel, 0, MessageOrder.NEWEST_FIRST)
    assertEquals(msg2, resultNewest)
  }

  @Test
  fun `MessageResolver should fail for non-TestTextChannel`() {
    val channel = mockk<MessageChannel>() // Not TestTextChannel
    assertFailsWith<UnsupportedOperationException> {
      MessageResolver.resolve(channel, 0, MessageOrder.OLDEST_FIRST)
    }
  }

  @Test
  fun `MessageResolver resolveById should use retrieveMessageById`() {
    val channel = mockk<MessageChannel>()
    val message = mockk<Message> { every { id } returns "msg1" }
    val action = mockk<RestAction<Message>>()

    every { channel.retrieveMessageById("msg1") } returns action
    every { action.complete() } returns message
    every { channel.name } returns "test-channel"

    val result = MessageResolver.resolveById(channel, "msg1")
    assertEquals(message, result)
  }
}
