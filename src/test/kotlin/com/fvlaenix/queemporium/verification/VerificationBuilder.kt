package com.fvlaenix.queemporium.verification

import com.fvlaenix.queemporium.service.MockAnswerService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VerificationBuilder(val mockAnswerService: MockAnswerService) {
    fun messageCount(expected: Int) {
        val actual = mockAnswerService.answers.size
        assertEquals(expected, actual, "Expected $expected messages, but found $actual")
    }

    fun lastMessageContains(text: String) {
        val lastMessage = mockAnswerService.answers.lastOrNull()
        assertNotNull(lastMessage, "No messages found")
        assertTrue(
            lastMessage.text.contains(text),
            "Last message '${lastMessage.text}' does not contain '$text'")
    }

    fun message(index: Int = -1, block: MessageVerificationBuilder.() -> Unit) {
        val messages = mockAnswerService.answers
        val messageToVerify = when {
            index == -1 -> messages.lastOrNull()
            index >= 0 && index < messages.size -> messages[index]
            else -> null
        }

        assertNotNull(messageToVerify, "No message found at index $index")

        MessageVerificationBuilder(messageToVerify).apply(block)
    }

    fun messagesContain(text: String) {
        val containsText = mockAnswerService.answers.any { it.text.contains(text) }
        assertTrue(containsText, "No message contains text: $text")
    }

    fun messageAt(index: Int, text: String) {
        val messages = mockAnswerService.answers
        assertTrue(index < messages.size, "No message at index $index")
        assertTrue(messages[index].text.contains(text),
            "Message at index $index does not contain: $text")
    }

    fun isEmpty() {
        val messages = mockAnswerService.answers
        assertTrue(messages.isEmpty(), "Expected no messages, but found ${messages.size}")
    }
}