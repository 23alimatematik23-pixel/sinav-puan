package com.sinavpuan.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sinavpuan.data.ExamResult
import com.sinavpuan.data.SchoolClass
import com.sinavpuan.data.Student
import com.sinavpuan.data.scoreList
import com.sinavpuan.data.title
import java.io.File

enum class ExportFormat(val ext: String, val mime: String, val label: String) {
    PDF("pdf", "application/pdf", "PDF"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word"),
}

object Exporter {
    fun export(
        context: Context,
        format: ExportFormat,
        cls: SchoolClass,
        students: List<Student>,
        results: List<ExamResult>,
    ): File {
        val (title, tables) = ReportBuilder.build(cls, students, results)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val base = "${cls.title()}_sinav_sonuclari".replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "$base.${format.ext}")
        when (format) {
            ExportFormat.PDF -> PdfExporter.export(file, title, tables)
            ExportFormat.XLSX -> XlsxExporter.export(file, title, tables)
            ExportFormat.DOCX -> DocxExporter.export(file, title, tables)
        }
        return file
    }
}

object Share {
    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun shareFile(context: Context, file: File, mime: String, viaWhatsApp: Boolean) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (viaWhatsApp) {
            for (pkg in whatsappPackages) {
                try {
                    context.startActivity(Intent(intent).setPackage(pkg))
                    return
                } catch (_: ActivityNotFoundException) {
                }
            }
            Toast.makeText(context, "WhatsApp bulunamadı", Toast.LENGTH_SHORT).show()
        }
        context.startActivity(Intent.createChooser(intent, "Paylaş / Kaydet"))
    }

    fun normalizePhone(phone: String): String {
        var d = phone.filter { it.isDigit() }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.startsWith("0")) d = "90" + d.drop(1)
        if (d.length == 10 && d.startsWith("5")) d = "90$d"
        return d
    }

    fun whatsappMessage(context: Context, phone: String, text: String) {
        val uri = Uri.parse("https://wa.me/${normalizePhone(phone)}?text=${Uri.encode(text)}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun parentMessage(cls: SchoolClass, s: Student, results: List<ExamResult>): String {
        val sb = StringBuilder()
        sb.append("Sayın Veli,\n")
        sb.append("${s.name} (${cls.title()}")
        if (s.number.isNotBlank()) sb.append(", No: ${s.number}")
        sb.append(") sınav sonuçları:\n")
        for (exam in 1..2) {
            val r = results.firstOrNull { it.studentId == s.id && it.examNo == exam }
            if (r == null) {
                sb.append("\n$exam. Sınav: henüz girilmedi\n")
            } else {
                val l = r.scoreList()
                sb.append("\n$exam. Sınav: ${l.sum()} puan\n")
                sb.append(l.mapIndexed { i, v -> "S${i + 1}: $v" }.joinToString(", ")).append("\n")
            }
        }
        return sb.toString()
    }
}
