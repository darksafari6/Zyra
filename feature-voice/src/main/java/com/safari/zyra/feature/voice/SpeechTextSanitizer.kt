package com.safari.zyra.feature.voice

object SpeechTextSanitizer {
    fun sanitize(source: String): String = source
        .replace(Regex("""https?://\S+|www\.\S+""", RegexOption.IGNORE_CASE), "link")
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("[*_~`]"), "")
        .replace(Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]"""), "")
        .replace(Regex("""[\uFE0E\uFE0F\u200D]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
}
