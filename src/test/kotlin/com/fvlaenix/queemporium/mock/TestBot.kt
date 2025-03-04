package com.fvlaenix.queemporium.mock

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.SelfUser
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

/**
 * Creates and configures a mock SelfUser for testing
 *
 * @param id Bot ID (defaults to 1L)
 * @param name Bot username (defaults to "TestBot")
 * @param discriminator Bot discriminator (defaults to "0000")
 * @return A mocked SelfUser object
 */
fun createMockBot(
  jda: JDA,
  id: Long = 1L,
  name: String = "TestBot",
  discriminator: String = "0000"
): SelfUser {
  // Create a relaxed mock of SelfUser
  val selfUser = mockk<SelfUser>(relaxed = true)

  // Configure basic properties
  every { selfUser.idLong } returns id
  every { selfUser.id } returns id.toString()
  every { selfUser.name } returns name
  every { selfUser.discriminator } returns discriminator
  every { selfUser.isBot } returns true
  every { selfUser.jda } returns jda

  return selfUser
}

/**
 * Creates and configures a mock Member for the bot in a specific guild
 *
 * @param guild The guild where the bot is a member
 * @param selfUser The bot's SelfUser object
 * @param nickname Optional nickname for the bot in this guild
 * @return A mocked Member object representing the bot in the guild
 */
fun createMockBotMember(
  guild: Guild,
  selfUser: SelfUser,
  nickname: String? = null
): Member {
  val member = mockk<Member>(relaxed = true)

  // Configure basic properties
  every { member.user } returns selfUser
  every { member.idLong } returns selfUser.idLong
  every { member.id } returns selfUser.id
  every { member.guild } returns guild
  every { member.jda } returns selfUser.jda

  // Configure nickname and effective name
  every { member.nickname } returns nickname
  every { member.effectiveName } returns (nickname ?: selfUser.name)

  // Configure permissions (grant all for simplicity in testing)
  every { member.hasPermission(any<Permission>()) } returns true
  every { member.hasPermission(any<Collection<Permission>>()) } returns true
  every { member.hasPermission(any<Permission>(), any<Permission>()) } returns true
  every { member.hasPermission(any<GuildChannel>(), any<Permission>()) } returns true

  every { member.hasAccess(any<GuildChannel>()) } returns true

  return member
}