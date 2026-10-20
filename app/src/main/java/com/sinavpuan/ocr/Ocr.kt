package com.sinavpuan.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object Ocr {
    private val client by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(bmp: Bitmap): List<TextBox> {
        val result = client.process(InputImage.fromBitmap(bmp, 0)).await()
        return result.textBlocks.flatMap { it.lines }.flatMap { it.elements }.mapNotNull { e ->
            e.boundingBox?.let { TextBox(e.text, it.left, it.top, it.right, it.bottom) }
        }
    }
}

class ExamAnalysis(
    val bitmap: Bitmap,
    val mask: Bitmap,
    val printed: List<TextBox>,
    val red: List<TextBox>,
)

object ExamAnalyzer {
    private const val TARGET_SIDE = 2400

    suspend fun analyze(context: Context, uri: Uri): ExamAnalysis = withContext(Dispatchers.Default) {
        val bmp = ImageUtils.load(context, uri)
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val red = BooleanArray(w * h) { ImageUtils.isRed(px[it]) }
        val mask = ImageUtils.redMaskBitmap(red, w, h)

        val printed = mutableListOf<TextBox>()
        val redBoxes = mutableListOf<TextBox>()
        split(Ocr.recognize(bmp), px, red, w, h, printed, redBoxes)
        mergeRed(redBoxes, Ocr.recognize(mask))

        // Small handwriting / score tables: OCR again on an enlarged crop around the red ink.
        val area = redArea(red, w, h)
        if (area != null) {
            val scale = (TARGET_SIDE.toFloat() / maxOf(area.width(), area.height())).coerceAtMost(3f)
            if (scale >= 1.4f) {
                val crop = ImageUtils.scaledCrop(bmp, area, scale)
                val cropMask = ImageUtils.scaledCrop(mask, area, scale)
                val back = { b: TextBox ->
                    TextBox(
                        b.text,
                        area.left + (b.left / scale).toInt(), area.top + (b.top / scale).toInt(),
                        area.left + (b.right / scale).toInt(), area.top + (b.bottom / scale).toInt(),
                    )
                }
                val cPrinted = mutableListOf<TextBox>()
                val cRed = mutableListOf<TextBox>()
                split(Ocr.recognize(crop).map(back), px, red, w, h, cPrinted, cRed)
                mergeRed(cRed, Ocr.recognize(cropMask).map(back))
                val inside = { b: TextBox -> area.contains(b.cx.toInt(), b.cy.toInt()) }
                printed.removeAll(inside)
                redBoxes.removeAll(inside)
                printed += cPrinted
                redBoxes += cRed
            }
        }
        ExamAnalysis(bmp, mask, printed, redBoxes)
    }

    private fun split(
        boxes: List<TextBox>, px: IntArray, red: BooleanArray, w: Int, h: Int,
        printed: MutableList<TextBox>, redBoxes: MutableList<TextBox>,
    ) {
        for (b in boxes) {
            if (ImageUtils.isRedBox(px, red, w, h, b)) redBoxes += b else printed += b
        }
    }

    private fun mergeRed(redBoxes: MutableList<TextBox>, fromMask: List<TextBox>) {
        for (m in fromMask) {
            val i = redBoxes.indexOfFirst { it.iou(m) > 0.3f }
            if (i < 0) {
                redBoxes += m
            } else if (ExamScoreParser.parseScore(redBoxes[i].text) == null && ExamScoreParser.parseScore(m.text) != null) {
                redBoxes[i] = m
            }
        }
    }

    /** Bounding box of the red ink, widened to include printed question numbers around it. */
    private fun redArea(red: BooleanArray, w: Int, h: Int): Rect? {
        val cell = 8
        val cw = (w + cell - 1) / cell
        val ch = (h + cell - 1) / cell
        val counts = IntArray(cw * ch)
        for (y in 0 until h) for (x in 0 until w) if (red[y * w + x]) counts[(y / cell) * cw + x / cell]++
        var l = cw; var t = ch; var r = -1; var b = -1
        for (cy in 0 until ch) for (cx in 0 until cw) {
            if (counts[cy * cw + cx] < 3) continue
            if (cx < l) l = cx
            if (cx > r) r = cx
            if (cy < t) t = cy
            if (cy > b) b = cy
        }
        if (r < 0) return null
        val left = l * cell
        val top = t * cell
        val right = minOf(w, (r + 1) * cell)
        val bottom = minOf(h, (b + 1) * cell)
        val padX = maxOf((right - left) / 4, w / 12)
        val padY = maxOf((bottom - top) * 2, h / 25)
        return Rect(
            maxOf(0, left - padX), maxOf(0, top - padY),
            minOf(w, right + padX), minOf(h, bottom + padY / 2),
        )
    }
}
