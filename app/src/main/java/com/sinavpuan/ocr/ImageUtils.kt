package com.sinavpuan.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

object ImageUtils {
    fun load(context: Context, uri: Uri, maxSide: Int = 2200): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bmp = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Görüntü okunamadı")
        val rotation = resolver.openInputStream(uri).use { stream ->
            if (stream == null) 0 else ExifInterface(stream).rotationDegrees
        }
        val scale = maxSide.toFloat() / max(bmp.width, bmp.height)
        if (rotation != 0 || scale < 1f) {
            val m = Matrix()
            if (scale < 1f) m.postScale(scale, scale)
            if (rotation != 0) m.postRotate(rotation.toFloat())
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }

    fun isRed(c: Int): Boolean {
        val r = (c shr 16) and 0xff
        val g = (c shr 8) and 0xff
        val b = c and 0xff
        return r > 100 && r - max(g, b) > 50 && g < r * 0.75f && b < r * 0.85f
    }

    private fun isDark(c: Int): Boolean {
        val r = (c shr 16) and 0xff
        val g = (c shr 8) and 0xff
        val b = c and 0xff
        return (r * 299 + g * 587 + b * 114) / 1000 < 120
    }

    /** Black-on-white image containing only red ink (slightly thickened for OCR). */
    fun redMaskBitmap(red: BooleanArray, w: Int, h: Int): Bitmap {
        val out = IntArray(w * h) { -0x1 }
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (!red[y * w + x]) continue
                for (dy in -1..1) {
                    val yy = y + dy
                    if (yy < 0 || yy >= h) continue
                    for (dx in -1..1) {
                        val xx = x + dx
                        if (xx in 0 until w) out[yy * w + xx] = -0x1000000
                    }
                }
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /** True when most of the ink inside the box is red. */
    fun isRedBox(pixels: IntArray, red: BooleanArray, w: Int, h: Int, b: TextBox): Boolean {
        var redCount = 0
        var darkCount = 0
        for (y in b.top.coerceAtLeast(0) until b.bottom.coerceAtMost(h)) {
            for (x in b.left.coerceAtLeast(0) until b.right.coerceAtMost(w)) {
                val i = y * w + x
                if (red[i]) redCount++ else if (isDark(pixels[i])) darkCount++
            }
        }
        return redCount >= 8 && redCount > (redCount + darkCount) * 0.6f
    }
}
