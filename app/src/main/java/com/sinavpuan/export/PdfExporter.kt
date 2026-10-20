package com.sinavpuan.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    private const val W = 842
    private const val H = 595
    private const val M = 28f

    fun export(file: File, title: String, tables: List<ReportTable>) {
        val doc = PdfDocument()
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
        val border = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val fill = Paint().apply { color = Color.rgb(235, 235, 235) }
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            page = p
            bold.textSize = 14f
            p.canvas.drawText(title, M, M + 10f, bold)
            text.textSize = 8f
            p.canvas.drawText("Sayfa $pageNo", W - M - 40f, H - 12f, text)
            y = M + 30f
        }

        newPage()
        for (t in tables) {
            val ws = t.weights()
            val unit = (W - 2 * M) / ws.sum()
            val colW = ws.map { it * unit }
            val fs = when {
                unit < 26 -> 7f
                unit < 34 -> 8.5f
                else -> 10f
            }
            val rowH = fs * 2f

            fun drawRow(cells: List<String>, header: Boolean) {
                val c = page!!.canvas
                if (header) c.drawRect(M, y, W - M, y + rowH, fill)
                val p = if (header || t.isSummaryRow(cells)) bold else text
                p.textSize = fs
                var x = M
                cells.forEachIndexed { i, cell ->
                    val w = colW[i]
                    c.drawRect(x, y, x + w, y + rowH, border)
                    val s = ellipsize(cell, p, w - 4f)
                    val tx = if (i == 2 && !header) x + 3f else x + (w - p.measureText(s)) / 2
                    c.drawText(s, tx, y + rowH / 2 + fs / 2.8f, p)
                    x += w
                }
                y += rowH
            }

            if (y + 20f + rowH * 3 > H - M) newPage()
            bold.textSize = 12f
            page!!.canvas.drawText(t.title, M, y + 12f, bold)
            y += 20f
            drawRow(t.header, true)
            for (r in t.rows) {
                if (y + rowH > H - M) {
                    newPage()
                    drawRow(t.header, true)
                }
                drawRow(r, false)
            }
            y += 18f
        }
        page?.let { doc.finishPage(it) }
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private fun ellipsize(s: String, p: Paint, max: Float): String {
        if (p.measureText(s) <= max) return s
        var end = s.length
        while (end > 0 && p.measureText(s.substring(0, end) + "…") > max) end--
        return s.substring(0, end) + "…"
    }
}
