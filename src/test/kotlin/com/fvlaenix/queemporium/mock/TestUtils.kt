package com.fvlaenix.queemporium.mock

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import java.util.*

/**
 * Utility class with common methods for creating mock objects in tests.
 */
object TestUtils {

  /**
   * Creates a mock Member with specified admin permissions.
   *
   * @param user The User object for this member
   * @param guild The Guild this member belongs to
   * @param isAdmin Whether this member should have admin permissions
   * @param isRoleAdmin Whether this member should have a role named "admin"
   * @return A mocked Member object
   */
  fun createMockMember(
    user: User,
    guild: Guild,
    isAdmin: Boolean = false,
    isRoleAdmin: Boolean = false
  ): Member {
    val member = mockk<Member>()

    // Set up basic properties
    every { member.user } returns user
    every { member.guild } returns guild
    every { member.idLong } returns user.idLong
    every { member.id } returns user.id
    every { member.effectiveName } returns user.name

    // Set up admin permissions
    every { member.hasPermission(Permission.ADMINISTRATOR) } returns isAdmin
    every { member.hasPermission(any<Collection<Permission>>()) } returns isAdmin
    every { member.hasPermission(any<Permission>(), any<Permission>()) } returns isAdmin
    every { member.hasPermission(any<GuildChannel>(), any<Permission>()) } returns isAdmin

    // Set up roles
    val roles = if (isRoleAdmin) {
      val adminRole = mockk<Role>()
      every { adminRole.name } returns "admin"
      Collections.singletonList(adminRole)
    } else {
      emptyList()
    }
    every { member.roles } returns roles

    return member
  }
}