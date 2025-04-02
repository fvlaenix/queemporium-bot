package com.fvlaenix.queemporium.mock

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.utils.ClosableIterator
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView
import org.jetbrains.annotations.Unmodifiable
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

class SortedSnowflakeCacheViewTextChannelCollection(
  private val collection: NavigableSet<TextChannel>
) : SortedSnowflakeCacheView<TextChannel> {

  fun add(element: TextChannel) {
    collection.add(element)
  }

  fun addAll(elements: Collection<TextChannel>) {
    collection.addAll(elements)
  }

  override fun forEachUnordered(action: Consumer<in TextChannel>) {
    collection.forEach { action.accept(it) }
  }

  override fun asSet(): NavigableSet<TextChannel?> {
    return TreeSet(collection)
  }

  override fun streamUnordered(): Stream<TextChannel?> {
    return collection.stream()
  }

  override fun parallelStreamUnordered(): Stream<TextChannel?> {
    return collection.parallelStream()
  }

  override fun getElementById(id: Long): TextChannel? {
    return collection.find { it.idLong == id }
  }

  override fun asList(): @Unmodifiable List<TextChannel?> {
    return Collections.unmodifiableList(ArrayList(collection))
  }

  override fun lockedIterator(): ClosableIterator<TextChannel?> {
    val iterator = collection.iterator()
    return object : ClosableIterator<TextChannel?> {
      override fun hasNext(): Boolean = iterator.hasNext()
      override fun next(): TextChannel = iterator.next()
      override fun close() {} // No-op for test implementation
      override fun remove() = iterator.remove()
    }
  }

  override fun size(): Long = collection.size.toLong()

  override fun isEmpty(): Boolean = collection.isEmpty()

  override fun getElementsByName(name: String, ignoreCase: Boolean): @Unmodifiable List<TextChannel?> {
    val result = collection.filter { element ->
      try {
        val getName = element::class.java.getMethod("getName")
        val elementName = getName.invoke(element) as String
        if (ignoreCase) {
          elementName.equals(name, ignoreCase = true)
        } else {
          elementName == name
        }
      } catch (e: Exception) {
        false
      }
    }
    return Collections.unmodifiableList(result)
  }

  override fun stream(): Stream<TextChannel?> = collection.stream()

  override fun parallelStream(): Stream<TextChannel?> = collection.parallelStream()

  override fun iterator(): MutableIterator<TextChannel?> = collection.iterator()
}