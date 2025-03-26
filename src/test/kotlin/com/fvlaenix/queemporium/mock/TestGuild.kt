package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Region
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.automod.AutoModRule
import net.dv8tion.jda.api.entities.automod.build.AutoModRuleData
import net.dv8tion.jda.api.entities.channel.concrete.*
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.entities.sticker.GuildSticker
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import net.dv8tion.jda.api.entities.templates.Template
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.PrivilegeConfig
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege
import net.dv8tion.jda.api.managers.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.*
import net.dv8tion.jda.api.requests.restaction.order.CategoryOrderAction
import net.dv8tion.jda.api.requests.restaction.order.ChannelOrderAction
import net.dv8tion.jda.api.requests.restaction.order.RoleOrderAction
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction
import net.dv8tion.jda.api.requests.restaction.pagination.BanPaginationAction
import net.dv8tion.jda.api.utils.ClosableIterator
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.cache.MemberCacheView
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
import net.dv8tion.jda.api.utils.cache.SortedChannelCacheView
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView
import net.dv8tion.jda.api.utils.concurrent.Task
import org.jetbrains.annotations.Unmodifiable
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Stream

class TestGuild(
  private val testJda: TestJDA,
  private val idLong: Long,
  private val name: String
) : Guild {
  private val textChannelsMap = mutableMapOf<Long, TextChannel>()
  private val bot = createMockBotMember(this, testJda.selfUser, "Queemporium Bot")
  private val membersMap = mutableMapOf<String, Member>()

  fun addMember(member: Member) {
    membersMap[member.user.id] = member
  }

  override fun getJDA(): JDA = testJda

  override fun retrieveInvites(): RestAction<@Unmodifiable List<Invite?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveTemplates(): RestAction<@Unmodifiable List<Template?>?> {
    TODO("Not yet implemented")
  }

  override fun createTemplate(
    name: String,
    description: String?
  ): RestAction<Template?> {
    TODO("Not yet implemented")
  }

  override fun retrieveWebhooks(): RestAction<@Unmodifiable List<Webhook?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveWelcomeScreen(): RestAction<GuildWelcomeScreen?> {
    TODO("Not yet implemented")
  }

  override fun getVoiceStates(): List<GuildVoiceState?> {
    TODO("Not yet implemented")
  }

  override fun getVerificationLevel(): Guild.VerificationLevel {
    TODO("Not yet implemented")
  }

  override fun getDefaultNotificationLevel(): Guild.NotificationLevel {
    TODO("Not yet implemented")
  }

  override fun getRequiredMFALevel(): Guild.MFALevel {
    TODO("Not yet implemented")
  }

  override fun getExplicitContentLevel(): Guild.ExplicitContentLevel {
    TODO("Not yet implemented")
  }

  override fun loadMembers(callback: Consumer<Member?>): Task<Void?> {
    TODO("Not yet implemented")
  }

  override fun retrieveMemberById(id: Long): CacheRestAction<Member?> {
    TODO("Not yet implemented")
  }

  override fun retrieveMembersByIds(
    includePresence: Boolean,
    vararg ids: Long
  ): Task<List<Member?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveMembersByPrefix(
    prefix: String,
    limit: Int
  ): Task<List<Member?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveActiveThreads(): RestAction<@Unmodifiable List<ThreadChannel?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveScheduledEventById(id: String): CacheRestAction<ScheduledEvent?> {
    TODO("Not yet implemented")
  }

  override fun moveVoiceMember(
    member: Member,
    audioChannel: AudioChannel?
  ): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun modifyNickname(
    member: Member,
    nickname: String?
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun prune(
    days: Int,
    wait: Boolean,
    vararg roles: Role?
  ): AuditableRestAction<Int?> {
    TODO("Not yet implemented")
  }

  override fun kick(user: UserSnowflake): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun ban(
    user: UserSnowflake,
    deletionTimeframe: Int,
    unit: TimeUnit
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun ban(
    users: Collection<UserSnowflake?>,
    deletionTime: Duration?
  ): AuditableRestAction<BulkBanResponse?> {
    TODO("Not yet implemented")
  }

  override fun unban(user: UserSnowflake): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun timeoutUntil(
    user: UserSnowflake,
    temporal: TemporalAccessor
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun removeTimeout(user: UserSnowflake): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun deafen(
    user: UserSnowflake,
    deafen: Boolean
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun mute(
    user: UserSnowflake,
    mute: Boolean
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun addRoleToMember(
    user: UserSnowflake,
    role: Role
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun removeRoleFromMember(
    user: UserSnowflake,
    role: Role
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun modifyMemberRoles(
    member: Member,
    rolesToAdd: Collection<Role?>?,
    rolesToRemove: Collection<Role?>?
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun modifyMemberRoles(
    member: Member,
    roles: Collection<Role?>
  ): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun transferOwnership(newOwner: Member): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun createTextChannel(
    name: String,
    parent: Category?
  ): ChannelAction<TextChannel?> {
    TODO("Not yet implemented")
  }

  override fun createNewsChannel(
    name: String,
    parent: Category?
  ): ChannelAction<NewsChannel?> {
    TODO("Not yet implemented")
  }

  override fun createVoiceChannel(
    name: String,
    parent: Category?
  ): ChannelAction<VoiceChannel?> {
    TODO("Not yet implemented")
  }

  override fun createStageChannel(
    name: String,
    parent: Category?
  ): ChannelAction<StageChannel?> {
    TODO("Not yet implemented")
  }

  override fun createForumChannel(
    name: String,
    parent: Category?
  ): ChannelAction<ForumChannel?> {
    TODO("Not yet implemented")
  }

  override fun createMediaChannel(
    name: String,
    parent: Category?
  ): ChannelAction<MediaChannel?> {
    TODO("Not yet implemented")
  }

  override fun createCategory(name: String): ChannelAction<Category?> {
    TODO("Not yet implemented")
  }

  override fun createRole(): RoleAction {
    TODO("Not yet implemented")
  }

  override fun createEmoji(
    name: String,
    icon: Icon,
    vararg roles: Role?
  ): AuditableRestAction<RichCustomEmoji?> {
    TODO("Not yet implemented")
  }

  override fun createSticker(
    name: String,
    description: String,
    file: FileUpload,
    tags: Collection<String?>
  ): AuditableRestAction<GuildSticker?> {
    TODO("Not yet implemented")
  }

  override fun deleteSticker(id: StickerSnowflake): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun createScheduledEvent(
    name: String,
    location: String,
    startTime: OffsetDateTime,
    endTime: OffsetDateTime
  ): ScheduledEventAction {
    TODO("Not yet implemented")
  }

  override fun createScheduledEvent(
    name: String,
    channel: GuildChannel,
    startTime: OffsetDateTime
  ): ScheduledEventAction {
    TODO("Not yet implemented")
  }

  override fun modifyCategoryPositions(): ChannelOrderAction {
    TODO("Not yet implemented")
  }

  override fun modifyTextChannelPositions(): ChannelOrderAction {
    TODO("Not yet implemented")
  }

  override fun modifyVoiceChannelPositions(): ChannelOrderAction {
    TODO("Not yet implemented")
  }

  override fun modifyTextChannelPositions(category: Category): CategoryOrderAction {
    TODO("Not yet implemented")
  }

  override fun modifyVoiceChannelPositions(category: Category): CategoryOrderAction {
    TODO("Not yet implemented")
  }

  override fun modifyRolePositions(useAscendingOrder: Boolean): RoleOrderAction {
    TODO("Not yet implemented")
  }

  override fun modifyWelcomeScreen(): GuildWelcomeScreenManager {
    TODO("Not yet implemented")
  }

  override fun getTextChannels(): List<TextChannel> = textChannelsMap.values.toList()
  override fun getMediaChannelCache(): SnowflakeCacheView<MediaChannel?> {
    TODO("Not yet implemented")
  }

  override fun getTextChannelById(id: Long): TextChannel? = textChannelsMap[id]

  internal fun addTextChannel(channel: TextChannel) {
    textChannelsMap[channel.idLong] = channel
  }

  override fun retrieveCommands(withLocalizations: Boolean): RestAction<List<Command?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveCommandById(id: String): RestAction<Command?> {
    TODO("Not yet implemented")
  }

  override fun upsertCommand(command: CommandData): RestAction<Command?> {
    TODO("Not yet implemented")
  }

  override fun updateCommands(): CommandListUpdateAction {
    TODO("Not yet implemented")
  }

  override fun editCommandById(id: String): CommandEditAction {
    TODO("Not yet implemented")
  }

  override fun deleteCommandById(commandId: String): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun retrieveIntegrationPrivilegesById(targetId: String): RestAction<List<IntegrationPrivilege?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveCommandPrivileges(): RestAction<PrivilegeConfig?> {
    TODO("Not yet implemented")
  }

  override fun retrieveRegions(includeDeprecated: Boolean): RestAction<EnumSet<Region?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveAutoModRules(): RestAction<@Unmodifiable List<AutoModRule?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveAutoModRuleById(id: String): RestAction<AutoModRule?> {
    TODO("Not yet implemented")
  }

  override fun createAutoModRule(data: AutoModRuleData): AuditableRestAction<AutoModRule?> {
    TODO("Not yet implemented")
  }

  override fun modifyAutoModRuleById(id: String): AutoModRuleManager {
    TODO("Not yet implemented")
  }

  override fun deleteAutoModRuleById(id: String): AuditableRestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun addMember(
    accessToken: String,
    user: UserSnowflake
  ): MemberAction {
    TODO("Not yet implemented")
  }

  override fun isLoaded(): Boolean {
    TODO("Not yet implemented")
  }

  override fun pruneMemberCache() {
    TODO("Not yet implemented")
  }

  override fun unloadMember(userId: Long): Boolean {
    TODO("Not yet implemented")
  }

  override fun getMemberCount(): Int {
    TODO("Not yet implemented")
  }

  // Базовые методы с простой реализацией
  override fun getName(): String = name
  override fun getIconId(): String? {
    TODO("Not yet implemented")
  }

  override fun getFeatures(): @Unmodifiable Set<String?> {
    TODO("Not yet implemented")
  }

  override fun getSplashId(): String? {
    TODO("Not yet implemented")
  }

  override fun getVanityCode(): String? {
    TODO("Not yet implemented")
  }

  override fun retrieveVanityInvite(): RestAction<VanityInvite?> {
    TODO("Not yet implemented")
  }

  override fun getDescription(): String? {
    TODO("Not yet implemented")
  }

  override fun getLocale(): DiscordLocale {
    TODO("Not yet implemented")
  }

  override fun getBannerId(): String? {
    TODO("Not yet implemented")
  }

  override fun getBoostTier(): Guild.BoostTier {
    TODO("Not yet implemented")
  }

  override fun getBoostCount(): Int {
    TODO("Not yet implemented")
  }

  override fun getBoosters(): @Unmodifiable List<Member?> {
    TODO("Not yet implemented")
  }

  override fun getMaxMembers(): Int {
    TODO("Not yet implemented")
  }

  override fun getMaxPresences(): Int {
    TODO("Not yet implemented")
  }

  override fun retrieveMetaData(): RestAction<Guild.MetaData?> {
    TODO("Not yet implemented")
  }

  override fun getAfkChannel(): VoiceChannel? {
    TODO("Not yet implemented")
  }

  override fun getSystemChannel(): TextChannel? {
    TODO("Not yet implemented")
  }

  override fun getRulesChannel(): TextChannel? {
    TODO("Not yet implemented")
  }

  override fun getCommunityUpdatesChannel(): TextChannel? {
    TODO("Not yet implemented")
  }

  override fun getSafetyAlertsChannel(): TextChannel? {
    TODO("Not yet implemented")
  }

  override fun getOwner(): Member? {
    TODO("Not yet implemented")
  }

  override fun getOwnerIdLong(): Long {
    TODO("Not yet implemented")
  }

  override fun getAfkTimeout(): Guild.Timeout {
    TODO("Not yet implemented")
  }

  override fun isMember(user: UserSnowflake): Boolean {
    TODO("Not yet implemented")
  }

  override fun getSelfMember(): Member = bot

  override fun getNSFWLevel(): Guild.NSFWLevel {
    TODO("Not yet implemented")
  }

  override fun getMember(user: UserSnowflake): Member? {
    return membersMap[user.id]
  }

  override fun getMemberCache(): MemberCacheView {
    return object : MemberCacheView {
      override fun getElementById(id: Long): Member? = membersMap[id.toString()]
      override fun getElementsByUsername(p0: String, p1: Boolean): @Unmodifiable List<Member?> = TODO("Not yet implemented")
      override fun getElementsByNickname(p0: String?, p1: Boolean): @Unmodifiable List<Member?> = TODO("Not yet implemented")
      override fun getElementsWithRoles(vararg p0: Role?): @Unmodifiable List<Member?> = TODO("Not yet implemented")
      override fun getElementsWithRoles(p0: Collection<Role?>): @Unmodifiable List<Member?> = TODO("Not yet implemented")
      override fun asList(): @Unmodifiable List<Member?> = membersMap.values.toList()
      override fun asSet(): @Unmodifiable Set<Member?> = membersMap.values.toSet()
      override fun lockedIterator(): ClosableIterator<Member?> =
        object : ClosableIterator<Member?> {
          private val iterator = membersMap.values.iterator()
          override fun hasNext(): Boolean = iterator.hasNext()
          override fun next(): Member = iterator.next()
          override fun close() {}
          override fun remove() = throw UnsupportedOperationException("Cannot remove from MemberCacheView")
        }
      override fun size(): Long = membersMap.size.toLong()
      override fun isEmpty(): Boolean = membersMap.isEmpty()
      override fun getElementsByName(name: String, ignoreCase: Boolean): @Unmodifiable List<Member?> =
        membersMap.values.filter { it.effectiveName.equals(name, ignoreCase) }
      override fun stream(): Stream<Member?> = membersMap.values.map { it as Member? }.stream()
      override fun parallelStream(): Stream<Member?> = membersMap.values.map { it as Member? }.parallelStream()
      override fun iterator(): MutableIterator<Member?> = membersMap.values.toMutableList().iterator()
    }
  }

  override fun getScheduledEventCache(): SortedSnowflakeCacheView<ScheduledEvent?> {
    TODO("Not yet implemented")
  }

  override fun getStageChannelCache(): SortedSnowflakeCacheView<StageChannel?> {
    TODO("Not yet implemented")
  }

  override fun getThreadChannelCache(): SortedSnowflakeCacheView<ThreadChannel?> {
    TODO("Not yet implemented")
  }

  override fun getCategoryCache(): SortedSnowflakeCacheView<Category?> {
    TODO("Not yet implemented")
  }

  override fun getTextChannelCache(): SortedSnowflakeCacheView<TextChannel> {
    return SortedSnowflakeCacheViewTextChannelCollection(TreeSet(channels.filterIsInstance<TextChannel>()))
  }

  override fun getNewsChannelCache(): SortedSnowflakeCacheView<NewsChannel?> {
    TODO("Not yet implemented")
  }

  override fun getVoiceChannelCache(): SortedSnowflakeCacheView<VoiceChannel?> {
    TODO("Not yet implemented")
  }

  override fun getForumChannelCache(): SortedSnowflakeCacheView<ForumChannel?> {
    TODO("Not yet implemented")
  }

  override fun getChannelCache(): SortedChannelCacheView<GuildChannel?> =
    SortedChannelCacheViewGuildChannel(channels.toList())

  override fun getChannels(includeHidden: Boolean): @Unmodifiable List<GuildChannel?> =
    textChannelsMap.values.map { it as GuildChannel? }

  override fun getRoleCache(): SortedSnowflakeCacheView<Role?> {
    TODO("Not yet implemented")
  }

  override fun getEmojiCache(): SnowflakeCacheView<RichCustomEmoji?> {
    TODO("Not yet implemented")
  }

  override fun getStickerCache(): SnowflakeCacheView<GuildSticker?> {
    TODO("Not yet implemented")
  }

  override fun retrieveEmojis(): RestAction<@Unmodifiable List<RichCustomEmoji?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveEmojiById(id: String): RestAction<RichCustomEmoji?> {
    TODO("Not yet implemented")
  }

  override fun retrieveStickers(): RestAction<@Unmodifiable List<GuildSticker?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveSticker(sticker: StickerSnowflake): RestAction<GuildSticker?> {
    TODO("Not yet implemented")
  }

  override fun editSticker(sticker: StickerSnowflake): GuildStickerManager {
    TODO("Not yet implemented")
  }

  override fun retrieveBanList(): BanPaginationAction {
    TODO("Not yet implemented")
  }

  override fun retrieveBan(user: UserSnowflake): RestAction<Guild.Ban?> {
    TODO("Not yet implemented")
  }

  override fun retrievePrunableMemberCount(days: Int): RestAction<Int?> {
    TODO("Not yet implemented")
  }

  override fun getPublicRole(): Role {
    TODO("Not yet implemented")
  }

  override fun getDefaultChannel(): DefaultGuildChannelUnion? {
    TODO("Not yet implemented")
  }

  override fun getManager(): GuildManager {
    TODO("Not yet implemented")
  }

  override fun isBoostProgressBarEnabled(): Boolean {
    TODO("Not yet implemented")
  }

  override fun retrieveAuditLogs(): AuditLogPaginationAction {
    TODO("Not yet implemented")
  }

  override fun leave(): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun delete(): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun delete(mfaCode: String?): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun getAudioManager(): AudioManager {
    TODO("Not yet implemented")
  }

  override fun requestToSpeak(): Task<Void?> {
    TODO("Not yet implemented")
  }

  override fun cancelRequestToSpeak(): Task<Void?> {
    TODO("Not yet implemented")
  }

  override fun getId(): String = idLong.toString()

  override fun getIdLong(): Long = idLong
}