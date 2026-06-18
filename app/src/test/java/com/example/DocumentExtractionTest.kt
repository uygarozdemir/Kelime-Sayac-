package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Gerçek PDF ve DOCX dosyaları üreterek WordCounterViewModel'in metin çıkarım
 * mantığını (extractTextFromPdf / extractTextFromDocx) uçtan uca test eder.
 *
 * Çıkarım metodları private olduğu için, üretim kodunu değiştirmeden
 * yansıma (reflection) ile çağrılırlar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DocumentExtractionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = WordCounterViewModel(context as Application)

    private fun extractPdf(uri: Uri): String {
        val m = WordCounterViewModel::class.java
            .getDeclaredMethod("extractTextFromPdf", Context::class.java, Uri::class.java)
        m.isAccessible = true
        return m.invoke(viewModel, context, uri) as String
    }

    private fun extractDocx(uri: Uri): String {
        val m = WordCounterViewModel::class.java
            .getDeclaredMethod("extractTextFromDocx", Context::class.java, Uri::class.java)
        m.isAccessible = true
        return m.invoke(viewModel, context, uri) as String
    }

    /** İçinde verilen satırlar bulunan gerçek bir PDF üretir. */
    private fun createPdf(lines: List<String>): Uri {
        PDFBoxResourceLoader.init(context)
        val doc = PDDocument()
        val page = PDPage()
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, 12f)
            cs.newLineAtOffset(50f, 700f)
            for (line in lines) {
                cs.showText(line)
                cs.newLineAtOffset(0f, -16f)
            }
            cs.endText()
        }
        val file = File.createTempFile("test", ".pdf")
        doc.save(file)
        doc.close()
        return Uri.fromFile(file)
    }

    /** word/document.xml içinde verilen paragrafları bulunan gerçek bir DOCX (zip) üretir. */
    private fun createDocx(paragraphs: List<String>): Uri {
        val ns = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
        val body = paragraphs.joinToString("") { p ->
            "<w:p><w:r><w:t>${p.replace("&", "&amp;").replace("<", "&lt;")}</w:t></w:r></w:p>"
        }
        val documentXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<w:document xmlns:w="$ns"><w:body>$body</w:body></w:document>"""

        val bytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("[Content_Types].xml"))
                zip.write("<?xml version=\"1.0\"?><Types/>".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("word/document.xml"))
                zip.write(documentXml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            baos.toByteArray()
        }
        val file = File.createTempFile("test", ".docx")
        file.writeBytes(bytes)
        return Uri.fromFile(file)
    }

    // ---- PDF ----

    @Test
    fun pdfWordCountIsCorrect() {
        val uri = createPdf(listOf("bir iki uc dort", "bes alti"))
        val text = extractPdf(uri)
        assertEquals(6, TextStats.countWords(text))
    }

    @Test
    fun pdfHyphenatedLineBreakJoinsIntoOneWord() {
        // "kelime-" satır sonu, sonraki satırda "sayaci" -> tek kelime
        val uri = createPdf(listOf("kelime-", "sayaci"))
        val text = extractPdf(uri)
        assertEquals(1, TextStats.countWords(text))
    }

    // ---- DOCX ----

    @Test
    fun docxWordCountIsCorrect() {
        val uri = createDocx(listOf("bir iki üç", "dört beş"))
        val text = extractDocx(uri)
        assertEquals(5, TextStats.countWords(text))
    }

    @Test
    fun docxIgnoresPunctuationOnlyTokens() {
        val uri = createDocx(listOf("merhaba, dünya", "!!!"))
        val text = extractDocx(uri)
        assertEquals(2, TextStats.countWords(text))
    }

    @Test
    fun docxEmptyDocumentHasNoWords() {
        val uri = createDocx(listOf(""))
        val text = extractDocx(uri)
        assertEquals(0, TextStats.countWords(text))
    }
}
