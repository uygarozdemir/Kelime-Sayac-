package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [TextStats.countWords] için birim testleri.
 * Saf Kotlin olduğu için Android/Robolectric gerektirmez.
 */
class WordCountTest {

    @Test
    fun emptyTextHasNoWords() {
        assertEquals(0, TextStats.countWords(""))
        assertEquals(0, TextStats.countWords("   \n\t  "))
    }

    @Test
    fun countsSimpleWords() {
        assertEquals(3, TextStats.countWords("bir iki üç"))
    }

    @Test
    fun collapsesMultipleWhitespace() {
        assertEquals(2, TextStats.countWords("merhaba\n\n   dünya"))
    }

    @Test
    fun joinsHyphenatedLineBreaks() {
        // "kelime-\nsayacı" tek bir kelime olarak birleşmeli
        assertEquals(1, TextStats.countWords("kelime-\nsayacı"))
    }

    @Test
    fun ignoresPunctuationOnlyTokens() {
        // "!!!" harf/rakam içermediği için sayılmaz
        assertEquals(2, TextStats.countWords("merhaba, dünya !!!"))
    }

    @Test
    fun countsNumbersAsWords() {
        assertEquals(3, TextStats.countWords("toplam 42 adet"))
    }
}
