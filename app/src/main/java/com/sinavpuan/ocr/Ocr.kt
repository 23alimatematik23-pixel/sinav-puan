package com.sinavpuan.ocr

import android.content.Context
import android.graphics.Bitmap
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
        for (b in Ocr.recognize(bmp)) {
            if (ImageUtils.isRedBox(px, red, w, h, b)) redBoxes += b else printed += b
        }
        for (m in Ocr.recognize(mask)) {
            if (redBoxes.none { it.iou(m) > 0.3f }) redBoxes += m
        }
        ExamAnalysis(bmp, mask, printed, redBoxes)
    }
}
