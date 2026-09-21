package org.openreader.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.openreader.core.model.LibraryDocument
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(
    documents: List<LibraryDocument>,
    onOpenDocument: (LibraryDocument) -> Unit,
    onImportUri: (Uri, String) -> Unit,
    onImportTree: (Uri) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = queryDisplayName(context.contentResolver, uri)
            onImportUri(uri, name)
            onOpenDocument(
                LibraryDocument(
                    fileHash = "",
                    fileName = name,
                    contentUri = uri.toString(),
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }
    val openTree = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onImportTree(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Biblioteca",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    openDocument.launch(arrayOf("application/pdf"))
                }
            ) { Text("Abrir PDF") }
            OutlinedButton(
                onClick = {
                    openTree.launch(null)
                }
            ) { Text("Abrir carpeta") }
        }
        Spacer(Modifier.height(16.dp))
        if (documents.isEmpty()) {
            Text(
                "Abre un PDF desde el almacenamiento interno, una tarjeta SD o un USB OTG.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(documents, key = { it.fileHash }) { doc ->
                    LibraryRow(
                        document = doc,
                        onOpen = { onOpenDocument(doc) },
                        onRemove = { onRemove(doc.fileHash) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    document: LibraryDocument,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(document.lastOpenedTimestamp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp)
    ) {
        Text(document.fileName, style = MaterialTheme.typography.titleMedium)
        val progressLabel = if (document.totalParagraphs > 0) {
            "Párrafo ${document.paragraphIndex + 1} de ${document.totalParagraphs} · $date"
        } else {
            date
        }
        Text(progressLabel, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onRemove) { Text("Quitar") }
    }
}
