package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.utils.ClosableIterator
import net.dv8tion.jda.api.utils.cache.SortedChannelCacheView
import org.jetbrains.annotations.Unmodifiable
import java.util.NavigableSet
import java.util.TreeSet
import java.util.function.Consumer
import java.util.stream.Stream

class SortedChannelCacheViewGuildChannel(source: List<GuildChannel>) : SortedChannelCacheView<GuildChannel?> {

  // Convert source list to a TreeSet for sorted access
  private val collection: NavigableSet<GuildChannel> = TreeSet<GuildChannel>(source)

  fun add(element: GuildChannel) {
    collection.add(element)
  }

  override fun <C : GuildChannel?> ofType(type: Class<C?>): SortedChannelCacheView<C?> {
    TODO("Not yet implemented")
  }

  override fun getElementById(type: ChannelType, id: Long): GuildChannel? {
    return collection.find { it.type == type && it.idLong == id }
  }

  override fun getElementById(id: Long): GuildChannel? {
    return collection.find { it.idLong == id }
  }

  override fun asList(): @Unmodifiable List<GuildChannel?> {
    return collection.toList()
  }

  override fun lockedIterator(): ClosableIterator<GuildChannel?> {
    val iterator = collection.iterator()
    return object : ClosableIterator<GuildChannel?> {
      override fun hasNext(): Boolean = iterator.hasNext()
      override fun next(): GuildChannel = iterator.next()
      override fun close() {} // No-op for test implementation
      override fun remove() = iterator.remove()
    }
  }

  override fun forEachUnordered(action: Consumer<in GuildChannel?>) {
    collection.forEach { action.accept(it) }
  }

  override fun size(): Long = collection.size.toLong()

  override fun isEmpty(): Boolean = collection.isEmpty()

  override fun getElementsByName(name: String, ignoreCase: Boolean): @Unmodifiable List<GuildChannel?> {
    val result = collection.filter { element ->
      try {
        if (ignoreCase) {
          element.name.equals(name, ignoreCase = true)
        } else {
          element.name == name
        }
      } catch (e: Exception) {
        false
      }
    }
    return result.toList()
  }

  override fun stream(): Stream<GuildChannel?> = collection.stream()

  override fun parallelStream(): Stream<GuildChannel?> = collection.parallelStream()

  override fun asSet(): NavigableSet<GuildChannel?> {
    return TreeSet(collection)
  }

  override fun streamUnordered(): Stream<GuildChannel?> {
    return collection.stream()
  }

  override fun parallelStreamUnordered(): Stream<GuildChannel?> {
    return collection.parallelStream()
  }

  override fun iterator(): MutableIterator<GuildChannel?> = collection.iterator()
}