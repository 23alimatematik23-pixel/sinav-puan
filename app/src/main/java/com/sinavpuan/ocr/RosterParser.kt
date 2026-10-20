package com.sinavpuan.ocr

import java.util.Locale

/** Extracts (school number, full name) pairs from OCR output of a printed class list. */
object RosterParser {
    data class Entry(val number: String, val name: String)

    private val tr = Locale("tr", "TR")
    private val skipWords = setOf(
        "AD", "ADI", "SOYAD", "SOYADI", "ADI-SOYADI", "ADISOYADI", "NO", "NUMARA", "NUMARASI", "NUMARASİ",
        "SIRA", "S.NO", "SNO", "ÖĞRENCİ", "ÖĞRENCİNİN", "OGRENCI", "CİNSİYET", "CİNSİYETİ", "CINSIYET",
        "ERKEK", "KIZ", "SINIF", "SINIFI", "ŞUBE", "ŞUBESİ", "LİSTESİ", "LISTESI", "LİSTE", "OKUL", "OKULU",
        "İMZA", "IMZA", "AÇIKLAMA", "TOPLAM", "SAYFA",
    )

    fun parse(boxes: List<TextBox>): List<Entry> {
        val entries = mutableListOf<Pair<Entry, Boolean>>()
        for (row in Layout.groupRows(boxes)) {
            val nums = mutableListOf<String>()
            val words = mutableListOf<String>()
            for (b in row) {
                val t = b.text.trim().trim('.', ',', ':', ';', '|', '-', '_', '(', ')')
                if (t.isEmpty()) continue
                if (t.all { it.isDigit() }) {
                    if (words.isEmpty()) nums += t
                    continue
                }
                val letters = t.count { it.isLetter() }
                if (letters < 2 || letters < t.length * 0.6) continue
                if (t.uppercase(tr) in skipWords) continue
                words += t.filter { it.isLetter() || it == '\'' || it == '-' }
            }
            if (words.isEmpty()) continue
            val number = if (nums.size >= 2) nums[1] else nums.firstOrNull() ?: ""
            entries += Entry(number, words.joinToString(" ")) to nums.isNotEmpty()
        }
        val anyNumbered = entries.any { it.second }
        return entries.filter { !anyNumbered || it.second }.map { it.first }
    }
}
