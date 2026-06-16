package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPdfBoxClasses() {
    println("--- DIAGNOSING PDFBOX CLASSPATH ---")
    val classesToTest = listOf(
      "com.tom_roush.pdfbox.android.PDFBoxResourceLoader",
      "com.tom_roush.pdfbox.pdmodel.PDDocument",
      "com.tom_roush.pdfbox.text.PDFTextStripper"
    )
    for (clsName in classesToTest) {
      try {
        println("Testing loading of class: $clsName")
        val clazz = Class.forName(clsName)
        println("SUCCESS: $clsName loaded! Class: $clazz")
        if (clsName.contains("PDFBoxResourceLoader")) {
          val methods = clazz.methods
          for (m in methods) {
            if (m.name == "init") {
              println("Found init method: $m")
            }
          }
        }
      } catch (e: Throwable) {
        println("FAILED: $clsName could not be loaded!")
        e.printStackTrace()
      }
    }
  }
}
