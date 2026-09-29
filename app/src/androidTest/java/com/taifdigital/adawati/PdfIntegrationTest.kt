package com.taifdigital.adawati

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun image(): File {
        val bitmap = Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val file = File.createTempFile("pdf-source", ".png", context.cacheDir)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
        return file
    }
    @Test fun pdfIsReadableA4AndRetainsAllThirtyPages() {
        val source = image()
        for (mode in listOf("natural", "text", "bw")) {
            val progress = mutableListOf<Int>()
            val count = if (mode == "natural") 30 else 2
            val result = DocumentEngine.pdf(context,List(count) { source },mode) { progress.add(it) }
            try {
                assertEquals(File(context.filesDir,"exports"),result.parentFile)
                PdfRenderer(ParcelFileDescriptor.open(result,ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
                    assertEquals(count,pdf.pageCount)
                    pdf.openPage(0).use { page ->
                        assertEquals(595,page.width); assertEquals(842,page.height)
                        val preview = Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888)
                        page.render(preview,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        assertNotEquals("The page must contain image content", Color.WHITE, preview.getPixel(297,421))
                        preview.recycle()
                    }
                }
                assertEquals((1..count).toList(),progress)
            } finally { result.delete() }
        }
        source.delete()
    }
    @Test fun failedPdfLeavesNoPartialExport() {
        val source = image()
        val invalid = File(context.cacheDir,"invalid-pdf-image.txt").apply { writeText("broken") }
        val dir = DocumentEngine.directory(context,"exports")
        val before = dir.list()!!.toSet()
        try {
            assertThrows(IllegalArgumentException::class.java) { DocumentEngine.pdf(context,listOf(source,invalid),"natural") {} }
            assertEquals(before,dir.list()!!.toSet())
        } finally { source.delete(); invalid.delete() }
    }
}
