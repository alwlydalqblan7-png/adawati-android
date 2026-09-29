package com.taifdigital.adawati

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.util.UUID

object DocumentEngine {
    const val MAX_PAGES = 30
    const val MAX_INPUT_BYTES = 40L * 1024 * 1024
    fun directory(context: Context, name: String) = File(context.filesDir, name).apply { mkdirs() }
    fun newFile(context: Context, dir: String, extension: String) =
        File(directory(context, dir), "Adawati-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6)}.$extension")

    fun importImage(context: Context, uri: Uri): File {
        val out = newFile(context, "drafts", "jpg")
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "تعذر فتح الصورة" }
                out.outputStream().use { stream ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_INPUT_BYTES) { "الصورة أكبر من 40 ميغابايت" }
                        stream.write(buffer, 0, read)
                    }
                }
            }
            // Verify before accepting it as a draft. Keep original bytes for size comparison.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(out.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "صيغة الصورة غير مدعومة" }
            return out
        } catch (e: Throwable) { out.delete(); throw e }
    }

    fun decode(file: File, maxEdge: Int = 2000): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "تعذر قراءة الصورة" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })) { "تعذر قراءة الصورة" }
        val exif = runCatching { ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
        val matrix = Matrix().apply {
            when (exif) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(-90f); postScale(-1f, 1f) }
                8 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    fun pdf(context: Context, files: List<File>, mode: String, progress: (Int) -> Unit): File {
        require(files.size in 1..MAX_PAGES) { "اختر من صفحة إلى 30 صفحة" }
        val out = newFile(context, "exports", "pdf")
        val pdf = PdfDocument()
        try {
            files.forEachIndexed { index, file ->
                val source = decode(file)
                var processed = source
                try {
                    processed = when (mode) {
                        "text" -> enhanceDocument(source)
                        "bw" -> blackAndWhiteDocument(source)
                        else -> source
                    }
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
                    page.canvas.drawColor(Color.WHITE)
                    val scale = minOf(571f / processed.width, 818f / processed.height)
                    val w = processed.width * scale
                    val h = processed.height * scale
                    page.canvas.drawBitmap(
                        processed,
                        null,
                        RectF((595 - w) / 2, (842 - h) / 2, (595 + w) / 2, (842 + h) / 2),
                        Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                    pdf.finishPage(page)
                } finally {
                    if (processed !== source) processed.recycle()
                    source.recycle()
                }
                progress(index + 1)
            }
            out.outputStream().use { pdf.writeTo(it) }
            return out
        } catch (e: Throwable) {
            out.delete()
            throw e
        } finally {
            pdf.close()
        }
    }

    fun compress(context: Context, source: File, quality: Int): File {
        val bitmap = decode(source)
        val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val out = newFile(context, "exports", "jpg")
        try {
            Canvas(flat).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
            out.outputStream().use { check(flat.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 95), it)) }
            require(out.length() < source.length()) { "لم نحصل على حجم أصغر. خفّض الجودة أو احتفظ بالصورة الأصلية." }
            return out
        } catch (e: Throwable) { out.delete(); throw e }
        finally { flat.recycle(); bitmap.recycle() }
    }

    fun qr(content: String): Bitmap {
        require(content.isNotBlank()) { "اكتب رابطًا أو نصًا" }
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 720, 720,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4))
        val pixels = IntArray(matrix.width * matrix.height) { i -> if (matrix[i % matrix.width, i / matrix.width]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    }

    fun saveQr(context: Context, content: String): File {
        val bitmap = qr(content)
        val out = newFile(context, "exports", "png")
        try { out.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }; return out }
        catch (e: Throwable) { out.delete(); throw e }
        finally { bitmap.recycle() }
    }

    fun edit(context: Context, file: File, rotation: Int, left: Float, top: Float, right: Float, bottom: Float): File {
        val source = decode(file)
        val x = (source.width * left).toInt().coerceIn(0, source.width-1)
        val y = (source.height * top).toInt().coerceIn(0, source.height-1)
        val w = (source.width * (right-left)).toInt().coerceIn(1, source.width-x)
        val h = (source.height * (bottom-top)).toInt().coerceIn(1, source.height-y)
        val edited = Bitmap.createBitmap(source, x, y, w, h, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        val out = newFile(context, "drafts", "png")
        try { out.outputStream().use { check(edited.compress(Bitmap.CompressFormat.PNG, 100, it)) }; return out }
        catch (e: Throwable) { out.delete(); throw e }
        finally { if (edited !== source) edited.recycle(); source.recycle() }
    }
}
