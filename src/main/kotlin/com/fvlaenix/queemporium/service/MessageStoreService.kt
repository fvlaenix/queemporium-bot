package com.fvlaenix.queemporium.service

import com.fvlaenix.queemporium.coroutine.BotCoroutineProvider
import com.fvlaenix.queemporium.utils.Logging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val LOG = Logging.getLogger(MessageStoreService::class.java)

class MessageStoreService(
  internal val jda: JDA,
  private val coroutineProvider: BotCoroutineProvider
) {
  private val guildCaches = ConcurrentHashMap<String, GuildCache>()
  private val consumerCount = AtomicInteger(0)
  private var initialized = false

  fun initialize() {
    if (initialized) return
    initialized = true

    LOG.info("Initializing MessageStoreService caches for ${jda.guilds.size} guilds")
    jda.guilds.forEach { guild ->
      val channels = guild.channels.filterIsInstance<MessageChannel>()
      val channelCaches = channels.associate { channel ->
        channel.id to ChannelCache(
          channelId = channel.id,
          guildId = guild.id
        )
      }
      guildCaches[guild.id] = GuildCache(
        guildId = guild.id,
        channelCaches = ConcurrentHashMap(channelCaches)
      )
      LOG.debug("Initialized cache for guild ${guild.name} with ${channels.size} channels")
    }
    LOG.info("MessageStoreService initialization complete")
  }

  fun guilds(): Flow<GuildHandle> = flow {
    jda.guilds.forEach { guild ->
      emit(GuildHandle(guild.id, this@MessageStoreService))
    }
  }

  fun registerConsumer() {
    val count = consumerCount.incrementAndGet()
    LOG.debug("Consumer registered, total consumers: $count")
    // Re-initialize if service was previously shut down
    if (!initialized) {
      initialize()
    }
  }

  fun unregisterConsumer() {
    val count = consumerCount.decrementAndGet()
    LOG.debug("Consumer unregistered, remaining consumers: $count")
    if (count == 0) {
      LOG.info("All consumers finished, cache can be dropped")
      shutdown()
    }
  }

  fun shutdown() {
    LOG.info("Shutting down MessageStoreService")
    guildCaches.clear()
    initialized = false
  }

  internal fun getGuildCache(guildId: String): GuildCache? = guildCaches[guildId]

  @TestOnly
  fun getCachedMessageCount(guildId: String, channelId: String): Int {
    return guildCaches[guildId]?.channelCaches?.get(channelId)?.messages?.size ?: 0
  }

  internal suspend fun fetchMessages(
    channel: MessageChannel,
    channelCache: ChannelCache
  ): List<Message> {
    channelCache.fetchMutex.withLock {
      if (channelCache.isExhausted) {
        return emptyList()
      }

      if (channelCache.historyIterator == null) {
        channelCache.historyIterator = channel.iterableHistory.iterator()
      }

      var attempts = 5
      while (attempts > 0) {
        attempts--
        try {
          val newMessages = mutableListOf<Message>()
          val batchSize = 100
          var fetched = 0

          while (fetched < batchSize && channelCache.historyIterator!!.hasNext()) {
            val message = channelCache.historyIterator!!.next()
            newMessages.add(message)
            fetched++
          }

          channelCache.readMutex.withLock {
            newMessages.forEach { message ->
              channelCache.messages.addLast(message)
            }
          }

          if (!channelCache.historyIterator!!.hasNext()) {
            channelCache.isExhausted = true
            LOG.debug("Channel ${channel.name} exhausted")
          }

          LOG.debug("Fetched ${newMessages.size} messages for channel ${channel.name}")
          return newMessages
        } catch (e: InsufficientPermissionException) {
          LOG.info("Insufficient permissions for channel ${channel.name}")
          channelCache.isExhausted = true
          return emptyList()
        } catch (e: InterruptedException) {
          LOG.warn("Interrupted exception while fetching messages from ${channel.name}", e)
        } catch (e: Exception) {
          LOG.error("Error fetching messages from ${channel.name}", e)
          throw e
        }
      }

      LOG.error("Failed to fetch messages from ${channel.name} after multiple attempts")
      throw RuntimeException("Failed to fetch messages after multiple attempts")
    }
  }
}

class GuildHandle(
  val id: String,
  private val service: MessageStoreService
) {
  fun channels(): Flow<ChannelHandle> = flow {
    val guildCache = service.getGuildCache(id) ?: return@flow
    val guild = service.jda.getGuildById(id) ?: return@flow

    guild.channels.filterIsInstance<MessageChannel>().forEach { channel ->
      val channelCache = guildCache.channelCaches[channel.id] ?: return@forEach
      emit(ChannelHandle(channel.id, id, service, channel, channelCache))
    }
  }
}

class ChannelHandle internal constructor(
  val id: String,
  val guildId: String,
  private val service: MessageStoreService,
  private val channel: MessageChannel,
  private val channelCache: ChannelCache
) {
  fun messages(): Flow<Message> = flow {
    LOG.debug("Starting message iteration for channel $id")
    var currentIndex = 0

    while (true) {
      val message = channelCache.readMutex.withLock {
        if (currentIndex < channelCache.messages.size) {
          channelCache.messages[currentIndex]
        } else {
          null
        }
      }

      if (message != null) {
        LOG.debug("Emitting message at index $currentIndex for channel $id")
        emit(message)
        currentIndex++
      } else {
        if (channelCache.isExhausted) {
          val cacheSize = channelCache.readMutex.withLock { channelCache.messages.size }
          if (currentIndex >= cacheSize) {
            LOG.debug("Channel $id exhausted, stopping iteration")
            break
          }
          LOG.debug("Channel $id exhausted but cache has $cacheSize messages, continuing from index $currentIndex")
          continue
        }

        LOG.debug("Fetching more messages for channel $id (current cache size: ${channelCache.messages.size})")
        val newMessages = service.fetchMessages(channel, channelCache)
        if (newMessages.isEmpty()) {
          val cacheSize = channelCache.readMutex.withLock { channelCache.messages.size }
          if (currentIndex >= cacheSize) {
            LOG.debug("No more messages fetched for channel $id, stopping iteration")
            break
          }
          LOG.debug("No messages fetched but cache has $cacheSize messages, continuing from index $currentIndex")
          continue
        }
        LOG.debug("Fetched ${newMessages.size} messages for channel $id, continuing iteration")
      }
    }
  }

  suspend fun takeNext(n: Int): List<Message> {
    return messages().take(n).toList()
  }
}

internal data class ChannelCache(
  val channelId: String,
  val guildId: String,
  val messages: ArrayDeque<Message> = ArrayDeque(),
  var isExhausted: Boolean = false,
  val fetchMutex: Mutex = Mutex(),
  val readMutex: Mutex = Mutex(),
  var historyIterator: Iterator<Message>? = null
)

internal data class GuildCache(
  val guildId: String,
  val channelCaches: ConcurrentHashMap<String, ChannelCache>
)
