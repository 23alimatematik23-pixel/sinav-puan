package com.sinavpuan.export

import com.sinavpuan.data.ExamResult
import com.sinavpuan.data.SchoolClass
import com.sinavpuan.data.Student
import com.sinavpuan.data.scoreList
import com.sinavpuan.data.title
import com.sinavpuan.data.total
import java.util.Locale

data class ReportTable(val title: String, val header: List<String>, val rows: List<List<String>>) {
    /** Relative column widths: Sıra, No, Adı Soyadı, then equal score columns. */
    fun weights(): List<Float> = header.indices.map {
        when (it) {
            0 -> 0.6f
            1 -> 1.1f
            2 -> 3.6f
            else -> 1f
        }
    }

    fun isSummaryRow(row: List<String>) = row.getOrNull(2)?.contains("Ortalama") == true
}

object ReportBuilder {
    private val tr = Locale("tr", "TR")

    fun fmt(d: Double): String =
        if (d % 1.0 == 0.0) d.toLong().toString() else String.format(tr, "%.1f", d)

    private fun avg(values: List<Int>): String = if (values.isEmpty()) "-" else fmt(values.average())

    fun build(cls: SchoolClass, students: List<Student>, results: List<ExamResult>): Pair<String, List<ReportTable>> {
        val title = "${cls.title()} Sınıfı Sınav Sonuçları"
        val res = results.associateBy { it.studentId to it.examNo }
        val tables = mutableListOf<ReportTable>()

        val t1 = students.map { res[it.id to 1]?.total() }
        val t2 = students.map { res[it.id to 2]?.total() }
        val averages = students.indices.map { i -> listOfNotNull(t1[i], t2[i]).takeIf { it.isNotEmpty() }?.average() }
        val summary = students.mapIndexed { i, s ->
            listOf(
                "${i + 1}", s.number, s.name,
                t1[i]?.toString() ?: "-", t2[i]?.toString() ?: "-",
                averages[i]?.let(::fmt) ?: "-",
            )
        }
        val summaryAvg = listOf(
            "", "", "Sınıf Ortalaması", avg(t1.filterNotNull()), avg(t2.filterNotNull()),
            averages.filterNotNull().takeIf { it.isNotEmpty() }?.average()?.let(::fmt) ?: "-",
        )
        tables += ReportTable(
            "Özet",
            listOf("Sıra", "No", "Adı Soyadı", "1. Sınav", "2. Sınav", "Ortalama"),
            summary + listOf(summaryAvg),
        )

        for (exam in 1..2) {
            val lists = students.map { res[it.id to exam]?.scoreList() }
            if (lists.all { it == null }) continue
            val n = maxOf(lists.maxOf { it?.size ?: 0 }, 1)
            val header = listOf("Sıra", "No", "Adı Soyadı") + (1..n).map { "S$it" } + "Toplam"
            val rows = students.mapIndexed { i, s ->
                val l = lists[i]
                listOf("${i + 1}", s.number, s.name) +
                    (0 until n).map { q -> l?.let { (it.getOrNull(q) ?: 0).toString() } ?: "-" } +
                    (l?.sum()?.toString() ?: "-")
            }
            val avgRow = listOf("", "", "Ortalama") +
                (0 until n).map { q -> avg(lists.filterNotNull().map { it.getOrNull(q) ?: 0 }) } +
                avg(lists.filterNotNull().map { it.sum() })
            tables += ReportTable("$exam. Sınav (Soru Bazlı)", header, rows + listOf(avgRow))
        }
        return title to tables
    }
}

fun xmlEscape(s: String): String = s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
