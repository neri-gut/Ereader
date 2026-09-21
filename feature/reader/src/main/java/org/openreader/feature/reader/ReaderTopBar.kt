package org.openreader.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderTopBar(
    fileName: String,
    showNativePdf: Boolean,
    showTtsBar: Boolean,
    onBack: () -> Unit,
    onToggleNative: () -> Unit,
    onSettings: () -> Unit,
    onVoices: () -> Unit,
    onToggleTts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("Atrás") }
        Text(
            text = fileName.ifBlank { "Lectura" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Box {
            TextButton(onClick = { menuOpen = true }) { Text("Menú") }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (showNativePdf) "Leer texto" else "Ver PDF original") },
                    onClick = {
                        menuOpen = false
                        onToggleNative()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Ajustes de lectura") },
                    onClick = {
                        menuOpen = false
                        onSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (showTtsBar) "Ocultar audio" else "Audio y voz") },
                    onClick = {
                        menuOpen = false
                        onToggleTts()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Voces neuronales") },
                    onClick = {
                        menuOpen = false
                        onVoices()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Biblioteca") },
                    onClick = {
                        menuOpen = false
                        onBack()
                    }
                )
            }
        }
    }
}
