package com.taifdigital.adawati
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

fun enhanceDocument(source: Bitmap): Bitmap {
    val target = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(target)

    val saturation = ColorMatrix().apply { setSaturation(0f) }
    val contrast = 1.28f
    val translate = (-0.5f * contrast + 0.5f) * 255f
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    saturation.postConcat(contrastMatrix)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(saturation)
    }
    canvas.drawBitmap(source, 0f, 0f, paint)
    return target
}

fun blackAndWhiteDocument(source: Bitmap): Bitmap {
    val gray = enhanceDocument(source)
    val target = Bitmap.createBitmap(gray.width, gray.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(gray.width * gray.height)
    gray.getPixels(pixels, 0, gray.width, 0, 0, gray.width, gray.height)
    for (i in pixels.indices) {
        val c = pixels[i]
        val luminance = (AndroidColor.red(c) + AndroidColor.green(c) + AndroidColor.blue(c)) / 3
        pixels[i] = if (luminance >= 150) AndroidColor.WHITE else AndroidColor.BLACK
    }
    target.setPixels(pixels, 0, gray.width, 0, 0, gray.width, gray.height)
    gray.recycle()
    return target
}

