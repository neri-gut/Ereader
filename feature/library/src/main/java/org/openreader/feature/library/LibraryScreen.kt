package org.openreader.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openreader.core.model.LibraryDocument

private enum class LibraryFilter { ALL, RECENT, FAVORITES }

@Composable
fun LibraryScreen(
    documents: List<LibraryDocument>,
    onOpenDocument: (LibraryDocument) -> Unit,
    onImportUri: (Uri, String) -> Unit,
    onImportTree: (Uri) -> Unit,
    onToggleFavorite: (LibraryDocument) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }
    var addMenu by remember { mutableStateOf(false) }
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

    val filtered = remember(documents, query, filter) {
        val now = System.currentTimeMillis()
        val week = 7L * 24 * 60 * 60 * 1000
        documents.filter { doc ->
            val matchesQuery = query.isBlank() ||
                doc.fileName.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> doc.isFavorite
                LibraryFilter.RECENT -> now - doc.lastOpenedTimestamp <= week
            }
            matchesQuery && matchesFilter
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "Tu biblioteca",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)
            )
            Text(
                text = if (documents.isEmpty()) "Añade PDFs para empezar" else "${documents.size} documentos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("Buscar por título") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == LibraryFilter.ALL,
                    onClick = { filter = LibraryFilter.ALL },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = filter == LibraryFilter.RECENT,
                    onClick = { filter = LibraryFilter.RECENT },
                    label = { Text("Recientes") }
                )
                FilterChip(
                    selected = filter == LibraryFilter.FAVORITES,
                    onClick = { filter = LibraryFilter.FAVORITES },
                    label = { Text("Favoritos") }
                )
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (documents.isEmpty()) {
                            "Pulsa + para abrir un PDF o una carpeta."
                        } else {
                            "Nada coincide con este filtro."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.fileHash.ifBlank { it.contentUri } }) { doc ->
                        BookCard(
                            document = doc,
                            onOpen = { onOpenDocument(doc) },
                            onFavorite = { onToggleFavorite(doc) },
                            onRemove = { onRemove(doc.fileHash) }
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Abrir PDF") },
                        onClick = {
                            addMenu = false
                            openDocument.launch(arrayOf("application/pdf"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Abrir carpeta") },
                        onClick = {
                            addMenu = false
                            openTree.launch(null)
                        }
                    )
                }
                FloatingActionButton(onClick = { addMenu = true }) {
                    Text("+", fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    document: LibraryDocument,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onRemove: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.clickable(onClick = onOpen)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            CoverImage(document)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onFavorite),
                contentAlignment = Alignment.Center
            ) {
                Text(if (document.isFavorite) "★" else "☆", color = Color.White, fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { menu = true },
                contentAlignment = Alignment.Center
            ) {
                Text("···", color = Color.White)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Quitar de la biblioteca") },
                        onClick = {
                            menu = false
                            onRemove()
                        }
                    )
                }
            }
            if (document.totalParagraphs > 0) {
                LinearProgressIndicator(
                    progress = {
                        ((document.paragraphIndex + 1).toFloat() / document.totalParagraphs)
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = document.fileName.removeSuffix(".pdf"),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = progressLabel(document),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun CoverImage(document: LibraryDocument) {
    val context = LocalContext.current
    var bitmap by remember(document.fileHash, document.contentUri) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(document.fileHash, document.contentUri) {
        if (document.fileHash.isNotBlank()) {
            bitmap = PdfCoverLoader.load(context, document.contentUri, document.fileHash)
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = document.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val hue = (document.fileName.hashCode().toUInt() % 360u).toFloat()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.hsl(hue, 0.38f, 0.42f),
                            Color.hsl(hue, 0.32f, 0.22f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = document.fileName.take(2).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

private fun progressLabel(document: LibraryDocument): String {
    if (document.totalParagraphs <= 0) return "Sin empezar"
    val percent = ((document.paragraphIndex + 1) * 100 / document.totalParagraphs).coerceIn(1, 100)
    return "$percent% leído"
}
