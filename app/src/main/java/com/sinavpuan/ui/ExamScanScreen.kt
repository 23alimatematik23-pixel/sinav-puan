package com.sinavpuan.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sinavpuan.dao
import com.sinavpuan.data.ExamResult
import com.sinavpuan.data.SchoolClass
import com.sinavpuan.data.Student
import com.sinavpuan.data.questionCount
import com.sinavpuan.data.scoreList
import com.sinavpuan.ocr.ExamAnalysis
import com.sinavpuan.ocr.ExamAnalyzer
import com.sinavpuan.ocr.ExamScoreParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScanScreen(nav: NavController, studentId: Long, examNo: Int) {
    val ctx = LocalContext.current
    val dao = ctx.dao
    val scope = rememberCoroutineScope()
    var student by remember { mutableStateOf<Student?>(null) }
    var cls by remember { mutableStateOf<SchoolClass?>(null) }
    var countText by remember { mutableStateOf("") }
    val scores = remember { mutableStateListOf<String>() }
    val candidates = remember { mutableStateListOf<List<Int>>() }
    var analysis by remember { mutableStateOf<ExamAnalysis?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showMask by remember { mutableStateOf(false) }

    LaunchedEffect(studentId, examNo) {
        val s = dao.getStudent(studentId) ?: return@LaunchedEffect
        student = s
        val c = dao.getClass(s.classId)
        cls = c
        val existing = dao.getResult(studentId, examNo)?.scoreList()
        val n = existing?.size ?: c?.questionCount(examNo) ?: 10
        countText = n.toString()
        scores.clear()
        scores.addAll(existing?.map { it.toString() } ?: List(n) { "0" })
        candidates.clear()
        candidates.addAll(List(n) { emptyList() })
    }

    fun applyParse(n: Int) {
        val a = analysis ?: return
        val r = ExamScoreParser.parse(a.printed, a.red, n, a.bitmap.width)
        scores.clear()
        scores.addAll(r.questions.map { it.score.toString() })
        candidates.clear()
        candidates.addAll(r.questions.map { it.candidates })
        info = when (r.mode) {
            ExamScoreParser.Mode.TABLE_ROW, ExamScoreParser.Mode.TABLE_COLUMN ->
                "Puan tablosu bulundu, puanlar tablodan okundu."
            ExamScoreParser.Mode.MARKERS -> "Soru numaraları bulundu, kırmızı puanlar sorulara eşleştirildi."
            ExamScoreParser.Mode.SEQUENTIAL ->
                "Soru numaraları bulunamadı; kırmızı puanlar yukarıdan aşağıya sırayla yerleştirildi. Lütfen kontrol edin."
            ExamScoreParser.Mode.NONE -> "Kırmızı kalemle yazılmış puan bulunamadı. Puanları elle girebilirsiniz."
        } + if (r.ignored.isNotEmpty()) "\nEşleşmeyen kırmızı sayılar: ${r.ignored.joinToString()}" else ""
    }

    fun resize(n: Int) {
        if (analysis != null) {
            applyParse(n)
            return
        }
        while (scores.size < n) scores.add("0")
        while (scores.size > n) scores.removeAt(scores.size - 1)
        while (candidates.size < n) candidates.add(emptyList())
        while (candidates.size > n) candidates.removeAt(candidates.size - 1)
    }

    val picker = rememberImagePicker { uri ->
        scope.launch {
            busy = true
            try {
                analysis = ExamAnalyzer.analyze(ctx, uri)
                applyParse(countText.toIntOrNull()?.takeIf { it in 1..60 } ?: scores.size.coerceAtLeast(1))
            } catch (e: Exception) {
                Toast.makeText(ctx, "Tarama hatası: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    suspend fun save(): Boolean {
        val s = student ?: return false
        val c = cls ?: return false
        val list = scores.map { it.toIntOrNull() ?: 0 }
        dao.upsertResult(ExamResult(s.id, examNo, list.joinToString(",")))
        val updated = if (examNo == 1) c.copy(q1Count = list.size) else c.copy(q2Count = list.size)
        if (updated != c) dao.updateClass(updated)
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$examNo. Sınav • ${student?.name ?: ""}", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy && student != null,
                    modifier = Modifier.weight(1f),
                    onClick = { scope.launch { if (save()) nav.popBackStack() } },
                ) { Text("Kaydet") }
                Button(
                    enabled = !busy && student != null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            if (!save()) return@launch
                            val list = dao.studentsList(student!!.classId)
                            val idx = list.indexOfFirst { it.id == studentId }
                            val next = list.getOrNull(idx + 1)
                            if (next == null) {
                                Toast.makeText(ctx, "Son öğrenciydi, kaydedildi", Toast.LENGTH_SHORT).show()
                                nav.popBackStack()
                            } else {
                                nav.navigate("exam/${next.id}/$examNo") {
                                    popUpTo("exam/{studentId}/{examNo}") { inclusive = true }
                                }
                            }
                        }
                    },
                ) { Text("Kaydet → Sonraki") }
            }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(enabled = !busy, onClick = picker.camera) { Text("Kamera ile Tara") }
                    OutlinedButton(enabled = !busy, onClick = picker.gallery) { Text("Galeriden") }
                }
                OutlinedTextField(
                    countText,
                    { v ->
                        countText = v.filter(Char::isDigit).take(2)
                        countText.toIntOrNull()?.takeIf { it in 1..60 }?.let { resize(it) }
                    },
                    Modifier.padding(top = 8.dp).width(160.dp),
                    label = { Text("Soru sayısı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                    Text("Sınav kağıdı taranıyor…")
                }
            }
            analysis?.let { a ->
                item {
                    val bmp = remember(a, showMask) { (if (showMask) a.mask else a.bitmap).asImageBitmap() }
                    Image(
                        bmp, null,
                        Modifier.fillMaxWidth().height(320.dp).padding(top = 8.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(showMask, { showMask = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Sadece kırmızı mürekkebi göster")
                    }
                }
            }
            info?.let { item { Text(it, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 4.dp)) } }
            item {
                Text(
                    "Toplam: ${scores.sumOf { it.toIntOrNull() ?: 0 }}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            itemsIndexed(scores) { i, v ->
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Soru ${i + 1}", Modifier.width(72.dp))
                    OutlinedTextField(
                        v, { nv -> scores[i] = nv.filter(Char::isDigit).take(3) },
                        Modifier.width(96.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.width(8.dp))
                    val c = candidates.getOrNull(i).orEmpty()
                    if (analysis != null) {
                        Text(
                            if (c.isEmpty()) "puan yok → 0" else "okunan: ${c.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (c.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
