package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Use 33 to match common local configurations for Robolectric
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kelime Sayacı", appName)
  }

  @Test
  fun testPdfBoxLoadingInRobolectric() {
    println("--- ROBOLECTRIC PDFBOX TEST ---")
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
      println("SUCCESS: PDFBoxResourceLoader initialized in Robolectric!")
      
      // Attempt to load PDFTextStripper to verify GlyphList resources etc are solved.
      val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
      println("SUCCESS: PDFTextStripper instantiated! Stripper: $stripper")
    } catch (e: Throwable) {
      println("FAILED: Robolectric PDFBox resolution or initialization failed!")
      e.printStackTrace()
    }
  }
}
