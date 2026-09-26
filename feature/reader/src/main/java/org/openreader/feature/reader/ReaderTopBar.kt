package org.openreader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderTopBar(
    showNativePdf: Boolean,
    showTtsBar: Boolean,
    pdfPageMode: PdfPageMode,
    palette: ThemeColors,
    onOpenMenu: () -> Unit,
    onToggleNative: () -> Unit,
    onToggleTts: () -> Unit,
    onOpenPage: () -> Unit,
    onPdfPageMode: (PdfPageMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = palette.text
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background.copy(alpha = 0.28f))
            .statusBarsPadding()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(Icons.Filled.Menu, contentDescription = "Menú", tint = tint)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleNative) {
            Icon(
                if (showNativePdf) Icons.AutoMirrored.Filled.MenuBook else Icons.Filled.PictureAsPdf,
                contentDescription = if (showNativePdf) "Texto" else "PDF",
                tint = tint
            )
        }
        if (showNativePdf) {
            IconButton(
                onClick = {
                    onPdfPageMode(
                        if (pdfPageMode == PdfPageMode.CONTINUOUS) {
                            PdfPageMode.PAGED
                        } else {
                            PdfPageMode.CONTINUOUS
                        }
                    )
                }
            ) {
                Icon(
                    if (pdfPageMode == PdfPageMode.CONTINUOUS) Icons.Filled.SwapVert else Icons.Filled.ViewDay,
                    contentDescription = if (pdfPageMode == PdfPageMode.CONTINUOUS) {
                        "Páginas"
                    } else {
                        "Continuo"
                    },
                    tint = tint
                )
            }
        } else {
            IconButton(onClick = onOpenPage) {
                Icon(Icons.Filled.Settings, contentDescription = "Página", tint = tint)
            }
        }
        IconButton(onClick = onToggleTts) {
            Icon(
                if (showTtsBar) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (showTtsBar) "Ocultar audio" else "Oír",
                tint = tint
            )
        }
    }
}
