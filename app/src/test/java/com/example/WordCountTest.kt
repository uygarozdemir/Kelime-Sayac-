package com.example

import org.junit.Test
import java.text.BreakIterator
import java.io.File

class WordCountTest {
    @Test
    fun testWordCount() {
        val text = File("src/test/java/com/example/ocr.txt").readText()
        
        // Old way
        val oldList = text.trim().split(Regex("[\\s\\p{Z}]+")).filter { token -> token.any { it.isLetterOrDigit() } }
        println("Old way count: ${oldList.size}")

        // BreakIterator way
        val breaker = BreakIterator.getWordInstance()
        breaker.setText(text)
        var count = 0
        var start = breaker.first()
        var end = breaker.next()
        while (end != BreakIterator.DONE) {
            val word = text.substring(start, end)
            if (word.any { it.isLetterOrDigit() }) {
                count++
            }
            start = end
            end = breaker.next()
        }
        println("BreakIterator count: $count")
        
        // MS Word roughly splits by spaces and common separators. But wait, `\s+` is 73 words.
    }
}
