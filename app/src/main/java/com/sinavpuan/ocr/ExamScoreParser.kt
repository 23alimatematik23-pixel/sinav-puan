package com.sinavpuan.ocr

import kotlin.math.abs

/**
 * Maps red-ink numbers on an exam paper to question numbers.
 *
 * Strategies (the one that assigns the most numbers wins):
 *  - TABLE_ROW: a printed row "1 2 3 ..." (score table) with red scores written below it.
 *  - TABLE_COLUMN: a printed column "1 2 3 ..." with red scores written to the right.
 *  - MARKERS: printed question markers ("1)", "2.", "Soru 3:") – each red number belongs to the
 *    nearest question marker above it in the same page column.
 *  - SEQUENTIAL: fallback, red numbers in reading order.
 * Questions without a red number get 0.
 */
object ExamScoreParser {
    enum class Mode { TABLE_ROW, TABLE_COLUMN, MARKERS, SEQUENTIAL, NONE }

    data class QuestionScore(val question: Int, val score: Int, val candidates: List<Int>)

    data class Result(val mode: Mode, val questions: List<QuestionScore>, val ignored: List<Int>)

    private data class Token(val value: Int, val box: TextBox)

    private val markerRe = Regex("""^(?:[Ss](?:oru|ORU)?\s*)?(\d{1,2})[.)\-:](?!\d)""")
    private val bareRe = Regex("""^\d{1,2}$""")

    fun parseScore(raw: String): Int? {
        var t = raw.trim().trimStart('+').trimEnd('.', ',', ')', ':', ';')
        if (t.isEmpty() || t.startsWith("-")) return null
        t = t.substringBefore('/')
        for (suffix in listOf("puan", "Puan", "PUAN", "pn", "P", "p")) {
            if (t.length > suffix.length && t.endsWith(suffix)) {
                t = t.dropLast(suffix.length)
                break
            }
        }
        if (t.isEmpty() || t.length > 3) return null
        if (t.none { it.isDigit() } && t !in setOf("O", "o")) return null
        val mapped = t.map {
            when (it) {
                'O', 'o', 'D', 'Q' -> '0'
                'l', 'I', '|', 'i', '!' -> '1'
                'Z', 'z' -> '2'
                'S', 's' -> '5'
                'B' -> '8'
                'g', 'q' -> '9'
                'G', 'b' -> '6'
                else -> it
            }
        }.joinToString("")
        if (!mapped.all { it.isDigit() }) return null
        val v = mapped.toInt()
        return if (v in 0..100) v else null
    }

    fun parse(printed: List<TextBox>, red: List<TextBox>, questionCount: Int, pageWidth: Int): Result {
        val n = questionCount.coerceIn(1, 100)
        val tokens = removeTotal(red.mapNotNull { b -> parseScore(b.text)?.let { Token(it, b) } })
        if (tokens.isEmpty()) {
            return Result(Mode.NONE, (1..n).map { QuestionScore(it, 0, emptyList()) }, emptyList())
        }
        val options = listOfNotNull(
            tableRow(printed, tokens, n),
            tableColumn(printed, tokens, n, pageWidth),
            markers(printed, tokens, n, pageWidth),
        )
        val best = options.maxByOrNull { o -> o.second.values.sumOf { it.size } }
        val (mode, assign) =
            if (best != null && best.second.isNotEmpty()) best else Mode.SEQUENTIAL to sequential(tokens, n)
        val used = assign.values.flatten().toSet()
        val qs = (1..n).map { q ->
            val c = assign[q].orEmpty().sortedBy { it.box.top }.map { it.value }
            QuestionScore(q, c.maxOrNull() ?: 0, c)
        }
        return Result(mode, qs, tokens.filter { it !in used }.map { it.value })
    }

    /** Drops a number that equals the sum of all the others (the teacher's total). */
    private fun removeTotal(tokens: List<Token>): List<Token> {
        if (tokens.size < 3) return tokens
        val sum = tokens.sumOf { it.value }
        val total = tokens.firstOrNull { it.value > 0 && it.value * 2 == sum } ?: return tokens
        return tokens - total
    }

    private fun longestRun(items: List<Pair<Int, TextBox>>): List<Pair<Int, TextBox>> {
        var best = emptyList<Pair<Int, TextBox>>()
        for (i in items.indices) {
            if (items[i].first != 1) continue
            val run = mutableListOf(items[i])
            var j = i + 1
            while (j < items.size && items[j].first == run.size + 1) {
                run += items[j]
                j++
            }
            if (run.size > best.size) best = run
        }
        return best
    }

    private fun bareNumbers(printed: List<TextBox>) =
        printed.filter { bareRe.matches(it.text.trim()) }

    private fun tableRow(printed: List<TextBox>, tokens: List<Token>, n: Int): Pair<Mode, Map<Int, List<Token>>>? {
        var header = emptyList<Pair<Int, TextBox>>()
        for (row in Layout.groupRows(bareNumbers(printed))) {
            val run = longestRun(row.map { it.text.trim().toInt() to it })
            if (run.size > header.size) header = run
        }
        if (header.size < 3) return null
        val rowH = header.map { it.second.height }.average().toFloat()
        val spacing = (header.last().second.cx - header.first().second.cx) / (header.size - 1)
        val bottom = header.maxOf { it.second.bottom }
        val map = mutableMapOf<Int, MutableList<Token>>()
        for (t in tokens) {
            if (t.box.cy <= bottom || t.box.cy > bottom + rowH * 6) continue
            val near = header.minBy { abs(it.second.cx - t.box.cx) }
            if (abs(near.second.cx - t.box.cx) > spacing * 0.6f || near.first > n) continue
            map.getOrPut(near.first) { mutableListOf() } += t
        }
        return Mode.TABLE_ROW to map
    }

    private fun tableColumn(
        printed: List<TextBox>,
        tokens: List<Token>,
        n: Int,
        pageWidth: Int,
    ): Pair<Mode, Map<Int, List<Token>>>? {
        var header = emptyList<Pair<Int, TextBox>>()
        for (col in Layout.groupColumns(bareNumbers(printed), pageWidth * 0.02f)) {
            val run = longestRun(col.map { it.text.trim().toInt() to it })
            if (run.size > header.size) header = run
        }
        if (header.size < 3) return null
        val spacing = (header.last().second.cy - header.first().second.cy) / (header.size - 1)
        val right = header.maxOf { it.second.right }
        val map = mutableMapOf<Int, MutableList<Token>>()
        for (t in tokens) {
            if (t.box.cx <= right || t.box.cx > right + pageWidth * 0.35f) continue
            val near = header.minBy { abs(it.second.cy - t.box.cy) }
            if (abs(near.second.cy - t.box.cy) > spacing * 0.6f || near.first > n) continue
            map.getOrPut(near.first) { mutableListOf() } += t
        }
        return Mode.TABLE_COLUMN to map
    }

    private fun clusterByLeft(items: List<Pair<Int, TextBox>>, tol: Float): List<List<Pair<Int, TextBox>>> {
        val clusters = mutableListOf<MutableList<Pair<Int, TextBox>>>()
        for (it in items.sortedBy { p -> p.second.left }) {
            val cur = clusters.lastOrNull()
            if (cur != null && it.second.left - cur.first().second.left <= tol) cur += it else clusters += mutableListOf(it)
        }
        return clusters
    }

    private fun markers(
        printed: List<TextBox>,
        tokens: List<Token>,
        n: Int,
        pageWidth: Int,
    ): Pair<Mode, Map<Int, List<Token>>>? {
        val cands = printed.mapNotNull { b ->
            markerRe.find(b.text.trim())?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..n }?.let { it to b }
        }
        if (cands.isEmpty()) return null
        val tol = pageWidth * 0.08f
        val clusters = clusterByLeft(cands, tol)
        val weight = clusters.associateWith { c -> c.map { it.first }.distinct().size }
        val selected = cands.map { it.first }.distinct().map { q ->
            val mine = clusters.filter { c -> c.any { it.first == q } }
            val bestCluster = mine.maxBy { weight.getValue(it) }
            bestCluster.filter { it.first == q }.minBy { it.second.top }
        }
        val columns = clusterByLeft(selected, tol).map { c -> c.minOf { it.second.left } to c.sortedBy { it.second.top } }
        val map = mutableMapOf<Int, MutableList<Token>>()
        for (t in tokens) {
            val col = columns.filter { it.first <= t.box.cx }.maxByOrNull { it.first } ?: columns.minBy { it.first }
            val m = col.second.lastOrNull { it.second.top <= t.box.cy + it.second.height * 0.5f } ?: continue
            map.getOrPut(m.first) { mutableListOf() } += t
        }
        return Mode.MARKERS to map
    }

    private fun sequential(tokens: List<Token>, n: Int): Map<Int, List<Token>> {
        val byBox = tokens.associateBy { it.box }
        return Layout.readingOrder(tokens.map { it.box }).take(n)
            .mapIndexed { i, b -> (i + 1) to listOf(byBox.getValue(b)) }.toMap()
    }
}
