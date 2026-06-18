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
  fun pdfBoxInitializesAndStripperWorks() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // PDFBox kaynak yükleyicisinin başlatılması ve metin çıkarıcının
    // örneklenmesi hata fırlatmadan tamamlanmalı.
    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
    val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
    assertEquals(1, stripper.startPage)
  }
}
