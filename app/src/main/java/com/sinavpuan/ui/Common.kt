package com.sinavpuan.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sinavpuan.data.SchoolClass
import com.sinavpuan.data.Student
import java.io.File
import java.util.Locale

class ImagePicker(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): ImagePicker {
    val ctx = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pending?.let { onPicked(Uri.parse(it)) }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    return ImagePicker(
        camera = {
            val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
            pending = uri.toString()
            cam.launch(uri)
        },
        gallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}

private val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
private val tr = Locale("tr", "TR")

@Composable
fun ClassDialog(initial: SchoolClass?, onDismiss: () -> Unit, onSave: (SchoolClass) -> Unit) {
    var grade by remember { mutableStateOf(initial?.grade ?: "") }
    var section by remember { mutableStateOf(initial?.section ?: "") }
    var size by remember { mutableStateOf(initial?.size?.toString() ?: "") }
    var q1 by remember { mutableStateOf((initial?.q1Count ?: 10).toString()) }
    var q2 by remember { mutableStateOf((initial?.q2Count ?: 10).toString()) }
    val valid = grade.isNotBlank() && section.isNotBlank() && (size.toIntOrNull() ?: 0) > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Yeni Sınıf" else "Sınıf Bilgileri") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(grade, { grade = it }, label = { Text("Sınıf (örn. 9)") }, singleLine = true)
                OutlinedTextField(section, { section = it }, label = { Text("Şube (örn. A)") }, singleLine = true)
                OutlinedTextField(
                    size, { size = it.filter(Char::isDigit).take(3) },
                    label = { Text("Sınıf mevcudu") }, singleLine = true, keyboardOptions = numberKeyboard,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        q1, { q1 = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f),
                        label = { Text("1. sınav soru") }, singleLine = true, keyboardOptions = numberKeyboard,
                    )
                    OutlinedTextField(
                        q2, { q2 = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f),
                        label = { Text("2. sınav soru") }, singleLine = true, keyboardOptions = numberKeyboard,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val base = initial ?: SchoolClass(grade = "", section = "", size = 0)
                onSave(
                    base.copy(
                        grade = grade.trim(),
                        section = section.trim().uppercase(tr),
                        size = size.toInt(),
                        q1Count = (q1.toIntOrNull() ?: 10).coerceIn(1, 60),
                        q2Count = (q2.toIntOrNull() ?: 10).coerceIn(1, 60),
                    ),
                )
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

@Composable
fun StudentDialog(
    initial: Student?,
    onDismiss: () -> Unit,
    onSave: (number: String, name: String, phone: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var number by remember { mutableStateOf(initial?.number ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.parentPhone ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Öğrenci Ekle" else "Öğrenci Bilgileri") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    number, { number = it.trim() }, label = { Text("Okul No") },
                    singleLine = true, keyboardOptions = numberKeyboard,
                )
                OutlinedTextField(name, { name = it }, label = { Text("Adı Soyadı") }, singleLine = true)
                OutlinedTextField(
                    phone, { phone = it }, label = { Text("Veli telefonu (WhatsApp)") }, singleLine = true,
                    placeholder = { Text("05xx xxx xx xx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Öğrenciyi sil", color = Color(0xFFC62828)) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(number.trim(), name.trim(), phone.trim()) }) {
                Text("Kaydet")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}
