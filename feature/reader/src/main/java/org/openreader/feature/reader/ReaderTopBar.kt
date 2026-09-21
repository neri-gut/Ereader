package org.openreader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    pdfPageMode: PdfPageMode,
    onToggleNative: () -> Unit,
    onToggleTts: () -> Unit,
    onPdfPageMode: (PdfPageMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = fileName.removeSuffix(".pdf").ifBlank { "Lectura" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = !showNativePdf,
                onClick = { if (showNativePdf) onToggleNative() },
                label = { Text("Texto") }
            )
            FilterChip(
                selected = showNativePdf,
                onClick = { if (!showNativePdf) onToggleNative() },
                label = { Text("PDF") }
            )
            FilterChip(
                selected = showTtsBar,
                onClick = onToggleTts,
                label = { Text("Audio") }
            )
            if (showNativePdf) {
                FilterChip(
                    selected = pdfPageMode == PdfPageMode.CONTINUOUS,
                    onClick = { onPdfPageMode(PdfPageMode.CONTINUOUS) },
                    label = { Text("Continuo") }
                )
                FilterChip(
                    selected = pdfPageMode == PdfPageMode.PAGED,
                    onClick = { onPdfPageMode(PdfPageMode.PAGED) },
                    label = { Text("Páginas") }
                )
            }
        }
    }
}
