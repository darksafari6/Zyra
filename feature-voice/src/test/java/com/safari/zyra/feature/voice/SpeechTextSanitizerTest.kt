package com.safari.zyra.feature.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextSanitizerTest {
    @Test
    fun removesMarkdownUrlsAndEmoji() {
        assertEquals("Hello world link", SpeechTextSanitizer.sanitize("**Hello** *world* https://example.com 😀"))
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("one two", SpeechTextSanitizer.sanitize(" one\n  two "))
    }
}
