package com.sinavpuan.ocr

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TextBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val cx: Float get() = (left + right) / 2f
    val cy: Float get() = (top + bottom) / 2f

    fun iou(o: TextBox): Float {
        val iw = min(right, o.right) - max(left, o.left)
        val ih = min(bottom, o.bottom) - max(top, o.top)
        if (iw <= 0 || ih <= 0) return 0f
        val inter = iw.toFloat() * ih
        val union = width.toFloat() * height + o.width.toFloat() * o.height - inter
        return if (union <= 0f) 0f else inter / union
    }
}

object Layout {
    /** Groups boxes into visual rows (top to bottom), each row sorted left to right. */
    fun groupRows(boxes: List<TextBox>): List<List<TextBox>> {
        if (boxes.isEmpty()) return emptyList()
        val medH = boxes.map { it.height }.sorted()[boxes.size / 2].coerceAtLeast(1)
        val rows = mutableListOf<MutableList<TextBox>>()
        var rowCy = 0f
        for (b in boxes.sortedBy { it.cy }) {
            val cur = rows.lastOrNull()
            if (cur != null && abs(b.cy - rowCy) <= medH * 0.5f) {
                cur += b
                rowCy = cur.map { it.cy }.average().toFloat()
            } else {
                rows += mutableListOf(b)
                rowCy = b.cy
            }
        }
        return rows.map { r -> r.sortedBy { it.left } }
    }

    /** Groups boxes into vertical columns (left to right), each column sorted top to bottom. */
    fun groupColumns(boxes: List<TextBox>, tolerance: Float): List<List<TextBox>> {
        val cols = mutableListOf<MutableList<TextBox>>()
        var colCx = 0f
        for (b in boxes.sortedBy { it.cx }) {
            val cur = cols.lastOrNull()
            if (cur != null && abs(b.cx - colCx) <= tolerance) {
                cur += b
                colCx = cur.map { it.cx }.average().toFloat()
            } else {
                cols += mutableListOf(b)
                colCx = b.cx
            }
        }
        return cols.map { c -> c.sortedBy { it.top } }
    }

    fun readingOrder(boxes: List<TextBox>): List<TextBox> = groupRows(boxes).flatten()
}
