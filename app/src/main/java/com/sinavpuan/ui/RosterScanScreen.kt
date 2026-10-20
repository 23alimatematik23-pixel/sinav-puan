package com.sinavpuan.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sinavpuan.dao
import com.sinavpuan.data.Student
import com.sinavpuan.ocr.ImageUtils
import com.sinavpuan.ocr.Ocr
import com.sinavpuan.ocr.RosterParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RosterRow(val number: String, val name: String, val selected: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScanScreen(nav: NavController, classId: Long) {
    val ctx = LocalContext.current
    val dao = ctx.dao
    val scope = rememberCoroutineScope()
    val cls by remember { dao.classFlow(classId) }.collectAsState(initial = null)
    val existing by remember { dao.students(classId) }.collectAsState(initial = emptyList())
    val rows = remember { mutableStateListOf<RosterRow>() }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberImagePicker { uri ->
        scope.launch {
            busy = true
            try {
                val bmp = withContext(Dispatchers.Default) { ImageUtils.load(ctx, uri) }
                val parsed = RosterParser.parse(Ocr.recognize(bmp))
                if (parsed.isEmpty()) Toast.makeText(ctx, "Fotoğrafta öğrenci bulunamadı", Toast.LENGTH_LONG).show()
                rows.addAll(parsed.map { RosterRow(it.number, it.name) })
            } catch (e: Exception) {
                Toast.makeText(ctx, "Tarama hatası: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }
    val selectedCount = rows.count { it.selected && it.name.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sınıf Listesi Tara") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
            )
        },
        bottomBar = {
            Button(
                enabled = selectedCount > 0 && !busy,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                onClick = {
                    scope.launch {
                        val existingNos = existing.map { it.number }.filter { it.isNotBlank() }.toSet()
                        val toAdd = rows.filter {
                            it.selected && it.name.isNotBlank() && (it.number.isBlank() || it.number.trim() !in existingNos)
                        }
                        dao.insertStudents(toAdd.map { Student(classId = classId, number = it.number.trim(), name = it.name.trim()) })
                        val skipped = selectedCount - toAdd.size
                        Toast.makeText(
                            ctx,
                            "${toAdd.size} öğrenci eklendi" + if (skipped > 0) " ($skipped kayıt zaten vardı)" else "",
                            Toast.LENGTH_LONG,
                        ).show()
                        nav.popBackStack()
                    }
                },
            ) { Text("Seçilenleri Listeye Ekle ($selectedCount)") }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sınıf listesinin fotoğrafını çekin veya galeriden seçin. Liste birden fazla sayfaysa " +
                            "her sayfayı ayrı ayrı ekleyebilirsiniz. Okunan bilgileri kaydetmeden önce kontrol edin.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !busy, onClick = picker.camera) { Text("Kamera") }
                        OutlinedButton(enabled = !busy, onClick = picker.gallery) { Text("Galeriden Seç") }
                    }
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Liste okunuyor…")
                    }
                    if (rows.isNotEmpty()) {
                        Text(
                            "Okunan: ${rows.size} satır  •  Sınıf mevcudu: ${cls?.size ?: "-"}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
            itemsIndexed(rows) { i, r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(r.selected, { rows[i] = r.copy(selected = it) })
                    OutlinedTextField(
                        r.number, { rows[i] = r.copy(number = it) }, Modifier.width(90.dp),
                        label = { Text("No") }, singleLine = true,
                    )
                    OutlinedTextField(
                        r.name, { rows[i] = r.copy(name = it) }, Modifier.weight(1f).padding(start = 6.dp),
                        label = { Text("Adı Soyadı") }, singleLine = true,
                    )
                }
            }
            item {
                TextButton(onClick = { rows.add(RosterRow("", "")) }) { Text("+ Satır ekle") }
            }
        }
    }
}
