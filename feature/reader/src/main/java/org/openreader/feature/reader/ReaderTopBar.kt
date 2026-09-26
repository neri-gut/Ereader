package org.openreader.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    textReady: Boolean,
    showTtsBar: Boolean,
    pdfPageMode: PdfPageMode,
    onOpenMenu: () -> Unit,
    onToggleNative: () -> Unit,
    onToggleTts: () -> Unit,
    onPdfPageMode: (PdfPageMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var optionsOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onOpenMenu) { Text("Menú") }
        Text(
            text = fileName.removeSuffix(".pdf").ifBlank { "Lectura" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Box {
            TextButton(onClick = { optionsOpen = true }) { Text("Vista") }
            DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            when {
                                showNativePdf && !textReady -> "Texto (extrayendo)"
                                showNativePdf -> "Cambiar a texto"
                                else -> "Cambiar a PDF"
                            }
                        )
                    },
                    onClick = {
                        optionsOpen = false
                        onToggleNative()
                    }
                )
                if (showNativePdf) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (pdfPageMode == PdfPageMode.CONTINUOUS) {
                                    "Modo páginas (deslizar)"
                                } else {
                                    "Modo continuo (scroll)"
                                }
                            )
                        },
                        onClick = {
                            optionsOpen = false
                            onPdfPageMode(
                                if (pdfPageMode == PdfPageMode.CONTINUOUS) {
                                    PdfPageMode.PAGED
                                } else {
                                    PdfPageMode.CONTINUOUS
                                }
                            )
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (showTtsBar) "Ocultar audio" else "Mostrar audio") },
                    onClick = {
                        optionsOpen = false
                        onToggleTts()
                    }
                )
            }
        }
    }
}
