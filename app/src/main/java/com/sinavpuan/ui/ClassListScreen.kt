package com.sinavpuan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sinavpuan.dao
import com.sinavpuan.data.title
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassListScreen(nav: NavController) {
    val ctx = LocalContext.current
    val dao = ctx.dao
    val scope = rememberCoroutineScope()
    val classes by remember { dao.classes() }.collectAsState(initial = emptyList())
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sınav Puan Tarayıcı") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Yeni Sınıf") },
            )
        },
    ) { pad ->
        if (classes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Henüz sınıf yok.\n\"Yeni Sınıf\" ile sınıf, şube ve mevcudu girerek başlayın.",
                    textAlign = TextAlign.Center,
                )
            }
        }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp)) {
            items(classes, key = { it.id }) { c ->
                Card(
                    onClick = { nav.navigate("class/${c.id}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${c.title()} Sınıfı", style = MaterialTheme.typography.titleLarge)
                        Text("Mevcut: ${c.size}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (adding) {
        ClassDialog(null, onDismiss = { adding = false }) { c ->
            adding = false
            scope.launch {
                val id = dao.insertClass(c)
                nav.navigate("class/$id")
            }
        }
    }
}
