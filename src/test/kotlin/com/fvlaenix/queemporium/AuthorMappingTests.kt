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
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
      )
    )
    val text = "Hello World"
    val mapping = authorMapper.findMapping(text)
    assertNull(mapping)
  }

  @Test
  fun `test single element exists`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
      )
    )
    val text = "Hello World: incorrect-2"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    mapping
    assertEquals("incorrect-2", mapping.from)
    assertEquals("correct-1 / correct-2", mapping.to)
  }

  @Test
  fun `test correct and incorrect in single element`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
      )
    )
    val text = "Hello World: hhttpsss://x.com/incorrect-2: correct-1"
    val mapping = authorMapper.findMapping(text)
    assertNull(mapping)
  }

  @Test
  fun `test incorrect in single element`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2")
      )
    )
    val text = "Hello World: hhttpsss://x.com/incorrect-2: incorrect-1"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("incorrect-1", mapping.from)
    assertEquals("correct-1 / correct-2", mapping.to)
  }

  @Test
  fun `test incorrect in two elements`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2"),
        listOf("abracadabra-1") to listOf("abracadabra-2")
      )
    )
    val text = "Hello World: hhttpsss://x.com/incorrect-2: incorrect-1"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("incorrect-1", mapping.from)
    assertEquals("correct-1 / correct-2", mapping.to)
  }

  @Test
  fun `test incorrect in two elements in second`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("incorrect-1", "incorrect-2") to listOf("correct-1", "correct-2"),
        listOf("abracadabra-1") to listOf("abracadabra-2")
      )
    )
    val text = "Hello World: hhttpsss://x.com/aaaa abracadabra-1"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("abracadabra-1", mapping.from)
    assertEquals("abracadabra-2", mapping.to)
  }

  @Test
  fun `test when correct name is substring of incorrect name`() {
    // In this test, the correct name "artist" is a substring of the incorrect "artist-x"
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("artist-x") to listOf("artist")
      )
    )

    // Text contains an incorrect author name
    val text = "Check out this artwork by artist-x!"
    val mapping = authorMapper.findMapping(text)

    // Should find a match despite "artist" being a substring of "artist-x"
    assertNotNull(mapping)
    assertEquals("artist-x", mapping.from)
    assertEquals("artist", mapping.to)
  }

  @Test
  fun `test when both incorrect and correct names are present`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("artist-x") to listOf("artist")
      )
    )

    // Text contains both incorrect and correct author names
    val text = "Check out artworks by artist-x and artist!"
    val mapping = authorMapper.findMapping(text)

    // Should not find a match because both names are present
    assertNull(mapping)
  }

  @Test
  fun `test with different variants of substring relations`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("super-artist") to listOf("artist"),
        listOf("artiste") to listOf("artist-proper"),
        listOf("name-one") to listOf("name")
      )
    )

    // Incorrect name "super-artist" contains correct "artist" as a suffix
    val text1 = "This is by super-artist."
    val mapping1 = authorMapper.findMapping(text1)
    assertNotNull(mapping1)
    assertEquals("super-artist", mapping1.from)
    assertEquals("artist", mapping1.to)

    // Incorrect name "artiste" contains part of the correct "artist-proper"
    val text2 = "This is by artiste."
    val mapping2 = authorMapper.findMapping(text2)
    assertNotNull(mapping2)
    assertEquals("artiste", mapping2.from)
    assertEquals("artist-proper", mapping2.to)

    // Correct name "name" is a prefix of incorrect "name-one"
    val text3 = "This is by name-one."
    val mapping3 = authorMapper.findMapping(text3)
    assertNotNull(mapping3)
    assertEquals("name-one", mapping3.from)
    assertEquals("name", mapping3.to)
  }

  @Test
  fun `test case insensitivity`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("Artist-X") to listOf("artist")
      )
    )

    // Incorrect name with different case
    val text = "This is by ARTIST-X."
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("Artist-X", mapping.from)
    assertEquals("artist", mapping.to)
  }

  @Test
  fun `test with punctuation near names`() {
    val authorMapper = MockAuthorMapper(
      mapOf(
        listOf("artist-x") to listOf("artist")
      )
    )

    // Text with punctuation near the name
    val text = "This is by artist-x, and it's amazing!"
    val mapping = authorMapper.findMapping(text)
    assertNotNull(mapping)
    assertEquals("artist-x", mapping.from)
    assertEquals("artist", mapping.to)
  }
}