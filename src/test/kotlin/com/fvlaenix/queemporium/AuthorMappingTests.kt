package com.fvlaenix.queemporium

import com.fvlaenix.queemporium.author.AuthorMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthorMappingTests {
  class MockAuthorMapper(val authorMapping: Map<List<String>, List<String>>) : AuthorMapper() {
    override fun authorMapping(): Map<List<String>, List<String>> = authorMapping
  }

  @Test
  fun `test no elements`() {
    val authorMapper = MockAuthorMapper(mapOf())
    val text = "Hello World"
    val mapping = authorMapper.findMapping(text)
    assertNull(mapping)
  }

  @Test
  fun `test single element not exists`() {
    val authorMapper = MockAuthorMapper(mapOf(
      listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
    ))
    val text = "Hello World"
    val mapping = authorMapper.findMapping(text)
    assertNull(mapping)
  }

  @Test
  fun `test single element exists`() {
    val authorMapper = MockAuthorMapper(mapOf(
      listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
    ))
    val text = "Hello World: incorrect-2"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    mapping
    assertEquals("incorrect-2", mapping.from)
    assertEquals("correct-1 / correct-2", mapping.to)
  }

  @Test
  fun `test correct and incorrect in single element`() {
    val authorMapper = MockAuthorMapper(mapOf(
      listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
    ))
    val text = "Hello World: hhttpsss://x.com/incorrect-2: correct-1"
    val mapping = authorMapper.findMapping(text)
    assertNull(mapping)
  }

  @Test
  fun `test incorrect in single element`() {
    val authorMapper = MockAuthorMapper(mapOf(
      listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
    ))
    val text = "Hello World: hhttpsss://x.com/incorrect-2: incorrect-1"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("incorrect-1", mapping.from)
    assertEquals("correct-1 / correct-2", mapping.to)
  }

  @Test
  fun `test incorrect in two elements`() {
    val authorMapper = MockAuthorMapper(mapOf(
      listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2"),
      listOf("abracadabra-1") to listOf("abracadabra-2")
    ))
    val text = "Hello World: hhttpsss://x.com/incorrect-2: incorrect-1"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("incorrect-1", mapping.from)
    assertEquals("correct-1 / correct-2", mapping.to)
  }

  @Test
  fun `test incorrect in two elements in second`() {
    val authorMapper = MockAuthorMapper(mapOf(
      listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2"),
      listOf("abracadabra-1") to listOf("abracadabra-2")
    ))
    val text = "Hello World: hhttpsss://x.com/aaaa abracadabra-1"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("abracadabra-1", mapping.from)
    assertEquals("abracadabra-2", mapping.to)
  }
}