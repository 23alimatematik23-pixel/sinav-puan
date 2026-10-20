package com.sinavpuan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.sinavpuan.export.Share
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentsScreen(nav: NavController, classId: Long) {
    val ctx = LocalContext.current
    val dao = ctx.dao
    val scope = rememberCoroutineScope()
    val cls by remember { dao.classFlow(classId) }.collectAsState(initial = null)
    val students by remember { dao.students(classId) }.collectAsState(initial = emptyList())
    val results by remember { dao.results(classId) }.collectAsState(initial = emptyList())
    val sent = remember { mutableStateListOf<Long>() }
    var editing by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Velilere Gönder") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp)) {
            item {
                Text(
                    "\"Gönder\"e bastığınızda WhatsApp, velinin numarasıyla ve öğrencinin puanlarını içeren mesajla açılır; " +
                        "WhatsApp'ta gönder tuşuna basmanız yeterli. Telefonu olmayan öğrenciye dokunarak numara ekleyebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(students, key = { it.id }) { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${s.number}  ${s.name}")
                        Text(
                            s.parentPhone.ifBlank { "Veli telefonu yok" },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    val c = cls
                    if (s.parentPhone.isBlank()) {
                        TextButton(onClick = { editing = s }) { Text("Telefon ekle") }
                    } else if (c != null) {
                        val onClick = {
                            sent.add(s.id)
                            Share.whatsappMessage(ctx, s.parentPhone, Share.parentMessage(c, s, results))
                        }
                        if (s.id in sent) {
                            FilledTonalButton(onClick = onClick) { Text("Tekrar") }
                        } else {
                            Button(onClick = onClick) { Text("Gönder") }
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }

    editing?.let { s ->
        StudentDialog(s, onDismiss = { editing = null }, onSave = { no, name, phone ->
            editing = null
            scope.launch { dao.updateStudent(s.copy(number = no, name = name, parentPhone = phone)) }
        })
    }
}
