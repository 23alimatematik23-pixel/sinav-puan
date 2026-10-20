package com.sinavpuan.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExamScoreParserTest {
    private fun box(text: String, x: Int, y: Int, w: Int = 30, h: Int = 30) = TextBox(text, x, y, x + w, y + h)

    @Test
    fun parseScoreHandlesCommonOcrNoise() {
        assertEquals(10, ExamScoreParser.parseScore("10"))
        assertEquals(5, ExamScoreParser.parseScore("+5"))
        assertEquals(8, ExamScoreParser.parseScore("8/10"))
        assertEquals(10, ExamScoreParser.parseScore("1O"))
        assertEquals(0, ExamScoreParser.parseScore("O"))
        assertEquals(7, ExamScoreParser.parseScore("7p"))
        assertNull(ExamScoreParser.parseScore("-2"))
        assertNull(ExamScoreParser.parseScore("V"))
        assertNull(ExamScoreParser.parseScore("150"))
    }

    @Test
    fun markersAssignScoresAndMissingIsZero() {
        val printed = listOf(
            box("1)", 50, 100), box("Aşağıdakilerden", 90, 100, 200),
            box("2)", 50, 400), box("Hesaplayınız", 90, 400, 200),
            box("3)", 50, 700), box("Bulunuz", 90, 700, 200),
            box("4)", 50, 1000),
        )
        val red = listOf(box("10", 700, 150), box("5", 700, 760), box("8", 700, 1050))
        val r = ExamScoreParser.parse(printed, red, 4, 1000)
        assertEquals(ExamScoreParser.Mode.MARKERS, r.mode)
        assertEquals(listOf(10, 0, 5, 8), r.questions.map { it.score })
    }

    @Test
    fun totalIsIgnored() {
        val printed = listOf(box("1.", 50, 100), box("2.", 50, 400), box("3.", 50, 700))
        val red = listOf(box("50", 800, 20), box("20", 700, 150), box("10", 700, 450), box("20", 700, 750))
        val r = ExamScoreParser.parse(printed, red, 3, 1000)
        assertEquals(listOf(20, 10, 20), r.questions.map { it.score })
    }

    @Test
    fun twoColumnLayout() {
        val printed = listOf(
            box("1.", 40, 100), box("2.", 40, 600),
            box("3.", 540, 100), box("4.", 540, 600),
        )
        val red = listOf(box("7", 400, 200), box("6", 900, 650), box("9", 900, 150))
        val r = ExamScoreParser.parse(printed, red, 4, 1000)
        assertEquals(listOf(7, 0, 9, 6), r.questions.map { it.score })
    }

    @Test
    fun scoreTableRow() {
        val printed = listOf(box("Soru", 50, 100, 60)) +
            (1..5).map { box("$it", 150 + it * 80, 100) } + box("Toplam", 600, 100, 80)
        val red = listOf(box("10", 230, 150), box("4", 310, 150), box("20", 470, 150), box("34", 610, 150))
        val r = ExamScoreParser.parse(printed, red, 5, 1000)
        assertEquals(ExamScoreParser.Mode.TABLE_ROW, r.mode)
        assertEquals(listOf(10, 4, 0, 20, 0), r.questions.map { it.score })
    }

    @Test
    fun sequentialFallback() {
        val red = listOf(box("3", 100, 500), box("5", 100, 100))
        val r = ExamScoreParser.parse(emptyList(), red, 3, 1000)
        assertEquals(ExamScoreParser.Mode.SEQUENTIAL, r.mode)
        assertEquals(listOf(5, 3, 0), r.questions.map { it.score })
    }
}
