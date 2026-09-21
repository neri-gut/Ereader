package org.openreader.feature.reader

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfNativePreview(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pageCount by remember { mutableStateOf(0) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    val mutex = remember { Mutex() }

    DisposableEffect(uri) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        descriptor = pfd
        renderer = pfd?.let { PdfRenderer(it) }
        pageCount = renderer?.pageCount ?: 0
        onDispose {
            renderer?.close()
            descriptor?.close()
            renderer = null
            descriptor = null
        }
    }

    if (pageCount == 0) {
        Text("No se pudo abrir la vista nativa del PDF", modifier = Modifier.padding(16.dp))
        return
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })
    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
        var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(page, renderer) {
            val current = renderer ?: return@LaunchedEffect
            bitmap = withContext(Dispatchers.IO) {
                mutex.withLock {
                current.openPage(page).use { pdfPage ->
                    val width = 1080
                    val height = (pdfPage.height.toFloat() / pdfPage.width * width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                        pdfPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
                }
            }
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Página ${page + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
