package com.taifdigital.adawati

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.exifinterface.media.ExifInterface
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentEngineTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun image(w: Int = 400, h: Int = 200): File {
        val bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val file = File.createTempFile("source", ".jpg", context.cacheDir)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,95,it) }
        bitmap.recycle()
        return file
    }
    @Test fun arabicQrRoundTrip() {
        val text = "أدواتي — مستند عربي 💙 https://example.com/مسار"
        val bitmap = DocumentEngine.qr(text)
        val pixels = IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width,bitmap.height,pixels))))
        assertEquals(text,decoded.text)
        bitmap.recycle()
    }
    @Test fun exifRotationAndBoundedDecode() {
        val file = image(3200,1600)
        ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes() }
        val bitmap = DocumentEngine.decode(file,1000)
        assertTrue(bitmap.height <= 1000)
        assertEquals(bitmap.width*2,bitmap.height)
        bitmap.recycle()
    }
    @Test fun pdfIsA4AndRetainsAllPages() {
        val source = image()
        val progress = mutableListOf<Int>()
        val result = DocumentEngine.pdf(context,listOf(source,source),"natural") { progress.add(it) }
        assertEquals(File(context.filesDir,"exports"),result.parentFile)
        PdfRenderer(ParcelFileDescriptor.open(result,ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
            assertEquals(2,pdf.pageCount)
            pdf.openPage(0).use { page -> assertEquals(595,page.width); assertEquals(842,page.height) }
        }
        assertEquals(listOf(1,2),progress)
    }
    @Test fun invalidImportLeavesNoDraft() {
        val invalid = File(context.cacheDir,"invalid.txt").apply { writeText("not an image") }
        val dir = DocumentEngine.directory(context,"drafts")
        val before = dir.list()!!.toSet()
        assertThrows(IllegalArgumentException::class.java) { DocumentEngine.importImage(context,Uri.fromFile(invalid)) }
        assertEquals(before,dir.list()!!.toSet())
    }
    @Test fun cropRotateDoesNotChangeOriginal() {
        val source = image()
        val original = source.readBytes()
        val edited = DocumentEngine.edit(context,source,90,0f,0f,.5f,1f)
        val bitmap = DocumentEngine.decode(edited)
        assertEquals(200,bitmap.width); assertEquals(200,bitmap.height)
        assertArrayEquals(original,source.readBytes())
        bitmap.recycle()
    }
    @Test fun failedPdfLeavesNoPartialExport() {
        val invalid = File(context.cacheDir,"broken.jpg").apply { writeText("broken") }
        val dir = DocumentEngine.directory(context,"exports")
        val before = dir.list()!!.toSet()
        assertThrows(IllegalArgumentException::class.java) { DocumentEngine.pdf(context,listOf(image(),invalid),"natural") {} }
        assertEquals(before,dir.list()!!.toSet())
    }
    @Test fun draftSurvivesModelRecreation() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        app.getSharedPreferences("draft_state", 0).edit().clear().commit()
        val first = ToolsModel(app)
        val source = image()
        first.pages.add(source)
        first.screen = "scan"
        first.qrText = "نص محفوظ"
        first.persist()
        val second = ToolsModel(app)
        assertEquals(source.path, second.pages.single().path)
        assertEquals("scan", second.screen)
        assertEquals("نص محفوظ", second.qrText)
    }
    @Test fun refusesEmptyAndOversizedPageSets() {
        assertThrows(IllegalArgumentException::class.java) { DocumentEngine.pdf(context,emptyList(),"natural") {} }
        assertThrows(IllegalArgumentException::class.java) { DocumentEngine.pdf(context,List(31) { image() },"natural") {} }
    }
}
