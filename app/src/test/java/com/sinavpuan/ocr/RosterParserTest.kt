package com.sinavpuan.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class RosterParserTest {
    private fun box(text: String, x: Int, y: Int) = TextBox(text, x, y, x + 20 * text.length, y + 30)

    @Test
    fun parsesNumberedRowsAndSkipsHeaders() {
        val boxes = listOf(
            box("9-A", 100, 10), box("SINIFI", 200, 10), box("ÖĞRENCİ", 350, 10), box("LİSTESİ", 520, 10),
            box("Sıra", 20, 60), box("No", 120, 60), box("Adı", 250, 60), box("Soyadı", 400, 60),
            box("1", 20, 110), box("123", 120, 110), box("Ahmet", 250, 110), box("YILMAZ", 400, 112), box("Erkek", 600, 110),
            box("2", 20, 160), box("456", 120, 158), box("Ayşe", 250, 160), box("Nur", 340, 160), box("KAYA", 400, 160),
            box("Okul", 20, 300), box("Müdürü", 120, 300),
        )
        val r = RosterParser.parse(boxes)
        assertEquals(
            listOf(RosterParser.Entry("123", "Ahmet YILMAZ"), RosterParser.Entry("456", "Ayşe Nur KAYA")),
            r,
        )
    }
}
