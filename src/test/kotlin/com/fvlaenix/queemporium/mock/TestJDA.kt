package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.*
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji
import net.dv8tion.jda.api.entities.sticker.StickerPack
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake
import net.dv8tion.jda.api.entities.sticker.StickerUnion
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.managers.AudioManager
import net.dv8tion.jda.api.managers.DirectAudioController
import net.dv8tion.jda.api.managers.Presence
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.*
import net.dv8tion.jda.api.requests.restaction.pagination.EntitlementPaginationAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ClosableIterator
import net.dv8tion.jda.api.utils.Once
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.dv8tion.jda.api.utils.cache.CacheView
import net.dv8tion.jda.api.utils.cache.ChannelCacheView
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView
import okhttp3.OkHttpClient
import org.jetbrains.annotations.Unmodifiable
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class TestJDA : JDA {
  private val bot = createMockBot(this, 0, "queemporium")
  private val guildsMap = mutableMapOf<Long, Guild>()
  private val listeners = mutableListOf<ListenerAdapter>()
  private val privateChannelsMap = mutableMapOf<Long, PrivateChannel>()

  override fun getGuildById(id: Long): Guild? = guildsMap[id]

  override fun getGuilds(): List<Guild> = guildsMap.values.toList()

  override fun getGuildCache(): SnowflakeCacheView<Guild> =
    object : SnowflakeCacheView<Guild> {
      override fun getElementById(id: Long): Guild? = guildsMap[id]
      override fun asList(): List<Guild> = guilds
      override fun asSet(): @Unmodifiable Set<Guild?> = guilds.toMutableSet()
      override fun lockedIterator(): ClosableIterator<Guild?> = TODO("Not yet implemented")
      override fun size(): Long = guilds.size.toLong()
      override fun isEmpty(): Boolean = guilds.isEmpty()
      override fun getElementsByName(name: String, ignoreCase: Boolean): @Unmodifiable List<Guild?> =
        guilds.filter { guild -> guild.name.equals(name, ignoreCase) }

      override fun stream(): Stream<Guild> = guilds.stream()
      override fun parallelStream(): Stream<Guild> = guilds.parallelStream()
      override fun iterator(): MutableIterator<Guild> = TODO("Not yet implemented")
    }

  internal fun addGuild(guild: Guild) {
    guildsMap[guild.idLong] = guild
  }

  fun notifyMessageSend(messageReceivedEvent: MessageReceivedEvent) {
    listeners.forEach { listenerAdapter -> listenerAdapter.onMessageReceived(messageReceivedEvent) }
  }

  fun notifyMessageDeleted(messageDeleteEvent: MessageDeleteEvent) {
    listeners.forEach { listenersAdapter -> listenersAdapter.onMessageDelete(messageDeleteEvent) }
  }

  override fun getStatus(): JDA.Status = JDA.Status.CONNECTED

  override fun getGatewayIntents(): EnumSet<GatewayIntent?> {
    TODO("Not yet implemented")
  }

  override fun getCacheFlags(): EnumSet<CacheFlag?> {
    TODO("Not yet implemented")
  }

  override fun unloadUser(p0: Long): Boolean {
    TODO("Not yet implemented")
  }

  override fun getGatewayPing(): Long {
    TODO("Not yet implemented")
  }

  override fun awaitStatus(
    p0: JDA.Status,
    vararg p1: JDA.Status?
  ): JDA {
    TODO("Not yet implemented")
  }

  override fun awaitShutdown(p0: Long, p1: TimeUnit): Boolean {
    TODO("Not yet implemented")
  }

  override fun cancelRequests(): Int {
    TODO("Not yet implemented")
  }

  override fun getRateLimitPool(): ScheduledExecutorService {
    TODO("Not yet implemented")
  }

  override fun getGatewayPool(): ScheduledExecutorService {
    TODO("Not yet implemented")
  }

  override fun getCallbackPool(): ExecutorService {
    TODO("Not yet implemented")
  }

  override fun getHttpClient(): OkHttpClient {
    TODO("Not yet implemented")
  }

  override fun getDirectAudioController(): DirectAudioController {
    TODO("Not yet implemented")
  }

  override fun setEventManager(p0: IEventManager?) {
    TODO("Not yet implemented")
  }

  override fun addEventListener(vararg p0: Any?) {
    listeners.addAll(p0.map { it as ListenerAdapter })
  }

  override fun removeEventListener(vararg p0: Any?) {
    TODO("Not yet implemented")
  }

  override fun getRegisteredListeners(): List<Any?> =
    listeners.toList()

  override fun <E : GenericEvent?> listenOnce(p0: Class<E?>): Once.Builder<E?> {
    TODO("Not yet implemented")
  }

  override fun retrieveCommands(p0: Boolean): RestAction<List<Command?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveCommandById(p0: String): RestAction<Command?> {
    TODO("Not yet implemented")
  }

  override fun upsertCommand(p0: CommandData): RestAction<Command?> {
    TODO("Not yet implemented")
  }

  override fun updateCommands(): CommandListUpdateAction {
    TODO("Not yet implemented")
  }

  override fun editCommandById(p0: String): CommandEditAction {
    TODO("Not yet implemented")
  }

  override fun deleteCommandById(p0: String): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun retrieveRoleConnectionMetadata(): RestAction<List<RoleConnectionMetadata?>?> {
    TODO("Not yet implemented")
  }

  override fun updateRoleConnectionMetadata(p0: Collection<RoleConnectionMetadata?>): RestAction<List<RoleConnectionMetadata?>?> {
    TODO("Not yet implemented")
  }

  override fun createGuild(p0: String): GuildAction {
    TODO("Not yet implemented")
  }

  override fun createGuildFromTemplate(
    p0: String,
    p1: String,
    p2: Icon?
  ): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun getAudioManagerCache(): CacheView<AudioManager?> {
    TODO("Not yet implemented")
  }

  override fun getUserCache(): SnowflakeCacheView<User?> {
    TODO("Not yet implemented")
  }

  override fun getMutualGuilds(vararg p0: User?): @Unmodifiable List<Guild?> {
    TODO("Not yet implemented")
  }

  override fun getMutualGuilds(p0: Collection<User?>): @Unmodifiable List<Guild?> {
    TODO("Not yet implemented")
  }

  override fun retrieveUserById(p0: Long): CacheRestAction<User?> {
    TODO("Not yet implemented")
  }

  override fun getUnavailableGuilds(): Set<String?> {
    TODO("Not yet implemented")
  }

  override fun isUnavailable(p0: Long): Boolean {
    TODO("Not yet implemented")
  }

  override fun getRoleCache(): SnowflakeCacheView<Role?> {
    TODO("Not yet implemented")
  }

  override fun getScheduledEventCache(): SnowflakeCacheView<ScheduledEvent?> {
    TODO("Not yet implemented")
  }

  override fun getPrivateChannelCache(): SnowflakeCacheView<PrivateChannel?> {
    return object : SnowflakeCacheView<PrivateChannel?> {
      override fun getElementById(id: Long): PrivateChannel? = privateChannelsMap[id]
      override fun asList(): List<PrivateChannel?> = privateChannelsMap.values.toList()
      override fun asSet(): @Unmodifiable Set<PrivateChannel?> = privateChannelsMap.values.toSet()
      override fun lockedIterator(): ClosableIterator<PrivateChannel?> = TODO("Not yet implemented")
      override fun size(): Long = privateChannelsMap.size.toLong()
      override fun isEmpty(): Boolean = privateChannelsMap.isEmpty()
      override fun getElementsByName(name: String, ignoreCase: Boolean): @Unmodifiable List<PrivateChannel?> =
        privateChannelsMap.values.filter { it.name.equals(name, ignoreCase) }

      override fun stream(): Stream<PrivateChannel?> = privateChannelsMap.values.map { it as PrivateChannel? }.stream()
      override fun parallelStream(): Stream<PrivateChannel?> =
        privateChannelsMap.values.map { it as PrivateChannel? }.parallelStream()

      override fun iterator(): MutableIterator<PrivateChannel?> = privateChannelsMap.values.toMutableList().iterator()
    }
  }

  internal fun addPrivateChannel(channel: PrivateChannel) {
    privateChannelsMap[channel.idLong] = channel
  }

  override fun openPrivateChannelById(p0: Long): CacheRestAction<PrivateChannel?> {
    TODO("Not yet implemented")
  }

  override fun getEmojiCache(): SnowflakeCacheView<RichCustomEmoji?> {
    TODO("Not yet implemented")
  }

  override fun createApplicationEmoji(
    p0: String,
    p1: Icon
  ): RestAction<ApplicationEmoji?> {
    TODO("Not yet implemented")
  }

  override fun retrieveApplicationEmojis(): RestAction<List<ApplicationEmoji?>?> {
    TODO("Not yet implemented")
  }

  override fun retrieveApplicationEmojiById(p0: String): RestAction<ApplicationEmoji?> {
    TODO("Not yet implemented")
  }

  override fun retrieveSticker(p0: StickerSnowflake): RestAction<StickerUnion?> {
    TODO("Not yet implemented")
  }

  override fun retrieveNitroStickerPacks(): RestAction<@Unmodifiable List<StickerPack?>?> {
    TODO("Not yet implemented")
  }

  override fun getEventManager(): IEventManager {
    TODO("Not yet implemented")
  }

  override fun getSelfUser(): SelfUser = bot

  override fun getPresence(): Presence {
    TODO("Not yet implemented")
  }

  override fun getShardInfo(): JDA.ShardInfo {
    TODO("Not yet implemented")
  }

  override fun getToken(): String {
    TODO("Not yet implemented")
  }

  override fun getResponseTotal(): Long = 0L

  override fun getMaxReconnectDelay(): Int {
    TODO("Not yet implemented")
  }

  override fun setAutoReconnect(p0: Boolean) {
    TODO("Not yet implemented")
  }

  override fun setRequestTimeoutRetry(p0: Boolean) {
    TODO("Not yet implemented")
  }

  override fun isAutoReconnect(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isBulkDeleteSplittingEnabled(): Boolean {
    TODO("Not yet implemented")
  }

  override fun shutdown() {
    TODO("Not yet implemented")
  }

  override fun shutdownNow() {
    TODO("Not yet implemented")
  }

  override fun retrieveApplicationInfo(): RestAction<ApplicationInfo?> {
    TODO("Not yet implemented")
  }

  override fun retrieveEntitlements(): EntitlementPaginationAction {
    TODO("Not yet implemented")
  }

  override fun retrieveEntitlementById(p0: Long): RestAction<Entitlement?> {
    TODO("Not yet implemented")
  }

  override fun createTestEntitlement(
    p0: Long,
    p1: Long,
    p2: TestEntitlementCreateAction.OwnerType
  ): TestEntitlementCreateAction {
    TODO("Not yet implemented")
  }

  override fun deleteTestEntitlement(p0: Long): RestAction<Void?> {
    TODO("Not yet implemented")
  }

  override fun setRequiredScopes(p0: Collection<String?>): JDA {
    TODO("Not yet implemented")
  }

  override fun getInviteUrl(vararg p0: Permission?): String {
    TODO("Not yet implemented")
  }

  override fun getInviteUrl(p0: Collection<Permission?>?): String {
    TODO("Not yet implemented")
  }

  override fun getShardManager(): ShardManager? {
    TODO("Not yet implemented")
  }

  override fun retrieveWebhookById(p0: String): RestAction<Webhook?> {
    TODO("Not yet implemented")
  }

  override fun getChannelCache(): ChannelCacheView<Channel?> {
    TODO("Not yet implemented")
  }

  override fun getStageChannelCache(): SnowflakeCacheView<StageChannel?> {
    TODO("Not yet implemented")
  }

  override fun getThreadChannelCache(): SnowflakeCacheView<ThreadChannel?> {
    TODO("Not yet implemented")
  }

  override fun getCategoryCache(): SnowflakeCacheView<Category?> {
    TODO("Not yet implemented")
  }

  override fun getTextChannelCache(): SnowflakeCacheView<TextChannel?> {
    TODO("Not yet implemented")
  }

  override fun getNewsChannelCache(): SnowflakeCacheView<NewsChannel?> {
    TODO("Not yet implemented")
  }

  override fun getVoiceChannelCache(): SnowflakeCacheView<VoiceChannel?> {
    TODO("Not yet implemented")
  }

  override fun getForumChannelCache(): SnowflakeCacheView<ForumChannel?> {
    TODO("Not yet implemented")
  }

  override fun getMediaChannelCache(): SnowflakeCacheView<MediaChannel?> {
    TODO("Not yet implemented")
  }
}