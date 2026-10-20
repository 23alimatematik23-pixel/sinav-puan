package com.sinavpuan.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sinavpuan.dao
import com.sinavpuan.data.Student
import com.sinavpuan.data.title
import com.sinavpuan.data.total
import com.sinavpuan.export.ExportFormat
import com.sinavpuan.export.Exporter
import com.sinavpuan.export.Share
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(nav: NavController, classId: Long) {
    val ctx = LocalContext.current
    val dao = ctx.dao
    val scope = rememberCoroutineScope()
    val cls by remember { dao.classFlow(classId) }.collectAsState(initial = null)
    val students by remember { dao.students(classId) }.collectAsState(initial = emptyList())
    val results by remember { dao.results(classId) }.collectAsState(initial = emptyList())
    val resMap = remember(results) { results.associateBy { it.studentId to it.examNo } }

    var menu by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Student?>(null) }
    var editClass by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var exported by remember { mutableStateOf<Pair<File, ExportFormat>?>(null) }

    fun export(format: ExportFormat) {
        val c = cls ?: return
        scope.launch {
            try {
                val f = withContext(Dispatchers.IO) {
                    Exporter.export(ctx, format, c, dao.studentsList(c.id), dao.resultsList(c.id))
                }
                exported = f to format
            } catch (e: Exception) {
                Toast.makeText(ctx, "Çıktı oluşturulamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cls?.let { "${it.title()} Sınıfı" } ?: "") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.Share, "Çıktı / Paylaş") }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        ExportFormat.entries.forEach { f ->
                            DropdownMenuItem(
                                text = { Text("${f.label} çıktısı al / paylaş") },
                                onClick = { menu = false; export(f) },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Velilere WhatsApp ile gönder") },
                            onClick = { menu = false; nav.navigate("parents/$classId") },
                        )
                        DropdownMenuItem(text = { Text("Sınıf bilgilerini düzenle") }, onClick = { menu = false; editClass = true })
                        DropdownMenuItem(text = { Text("Sınıfı sil") }, onClick = { menu = false; confirmDelete = true })
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val size = cls?.size ?: 0
                    Text("Mevcut: $size   •   Listede: ${students.size} öğrenci", fontWeight = FontWeight.Bold)
                    if (students.size != size && size > 0) {
                        Text(
                            if (students.size < size) "${size - students.size} öğrenci eksik" else "Listede mevcuttan fazla öğrenci var",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("roster/$classId") }) {
                            Icon(Icons.Default.Search, null)
                            Text(" Listeyi Tara")
                        }
                        OutlinedButton(onClick = { adding = true }) {
                            Icon(Icons.Default.Add, null)
                            Text(" Manuel Ekle")
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("#", Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                Text("No", Modifier.width(52.dp), fontWeight = FontWeight.Bold)
                Text("Adı Soyadı", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Sınavlar", fontWeight = FontWeight.Bold)
            }
            LazyColumn {
                itemsIndexed(students, key = { _, s -> s.id }) { i, s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = s }.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 1}", Modifier.width(28.dp))
                        Text(s.number, Modifier.width(52.dp))
                        Text(s.name, Modifier.weight(1f), maxLines = 2)
                        for (exam in 1..2) {
                            val r = resMap[s.id to exam]
                            val pad = PaddingValues(horizontal = 8.dp)
                            val mod = Modifier.padding(start = 4.dp)
                            val onClick = { nav.navigate("exam/${s.id}/$exam") }
                            if (r != null) {
                                FilledTonalButton(onClick = onClick, modifier = mod, contentPadding = pad) {
                                    Text("$exam.S: ${r.total()}", fontSize = 12.sp)
                                }
                            } else {
                                OutlinedButton(onClick = onClick, modifier = mod, contentPadding = pad) {
                                    Text("$exam. Sınav", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (adding) {
        StudentDialog(null, onDismiss = { adding = false }, onSave = { no, name, phone ->
            if (no.isNotEmpty() && students.any { it.number == no }) {
                Toast.makeText(ctx, "$no numaralı öğrenci zaten listede", Toast.LENGTH_SHORT).show()
            } else {
                adding = false
                scope.launch { dao.insertStudent(Student(classId = classId, number = no, name = name, parentPhone = phone)) }
            }
        })
    }
    editing?.let { s ->
        StudentDialog(
            s,
            onDismiss = { editing = null },
            onSave = { no, name, phone ->
                if (no.isNotEmpty() && students.any { it.id != s.id && it.number == no }) {
                    Toast.makeText(ctx, "$no numaralı öğrenci zaten listede", Toast.LENGTH_SHORT).show()
                } else {
                    editing = null
                    scope.launch { dao.updateStudent(s.copy(number = no, name = name, parentPhone = phone)) }
                }
            },
            onDelete = {
                editing = null
                scope.launch { dao.deleteStudent(s) }
            },
        )
    }
    if (editClass) {
        cls?.let { c ->
            ClassDialog(c, onDismiss = { editClass = false }) { updated ->
                editClass = false
                scope.launch { dao.updateClass(updated) }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Sınıf silinsin mi?") },
            text = { Text("Sınıftaki tüm öğrenciler ve sınav puanları silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    val c = cls ?: return@TextButton
                    scope.launch {
                        dao.deleteClass(c)
                        nav.popBackStack()
                    }
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("İptal") } },
        )
    }
    exported?.let { (file, format) ->
        AlertDialog(
            onDismissRequest = { exported = null },
            title = { Text("${format.label} dosyası hazır") },
            text = { Text("${file.name}\n\nWhatsApp ile gönderebilir ya da başka bir uygulamayla paylaşıp kaydedebilirsiniz.") },
            confirmButton = {
                TextButton(onClick = { exported = null; Share.shareFile(ctx, file, format.mime, true) }) { Text("WhatsApp") }
            },
            dismissButton = {
                TextButton(onClick = { exported = null; Share.shareFile(ctx, file, format.mime, false) }) { Text("Diğer / Kaydet") }
            },
        )
    }
}
