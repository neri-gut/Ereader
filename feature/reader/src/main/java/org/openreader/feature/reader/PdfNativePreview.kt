package org.openreader.feature.reader

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedHashMap
import java.util.concurrent.Executors

private const val TAG = "OpenReaderPdf"

enum class PdfPageMode { CONTINUOUS, PAGED }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfNativePreview(
    uri: Uri,
    localPath: String?,
    pageMode: PdfPageMode,
    modifier: Modifier = Modifier
) {
    var pageCount by remember(localPath) { mutableIntStateOf(0) }
    var error by remember(localPath) { mutableStateOf<String?>(null) }
    var viewWidth by remember { mutableIntStateOf(0) }
    var session by remember { mutableStateOf<PdfRenderSession?>(null) }
    var aspect by remember { mutableStateOf(1f / 1.414f) }

    DisposableEffect(localPath) {
        var created: PdfRenderSession? = null
        if (localPath.isNullOrBlank()) {
            error = null
            pageCount = 0
            session = null
        } else {
            try {
                created = PdfRenderSession(localPath)
                session = created
                pageCount = created.pageCount
                error = null
                Log.i(TAG, "Opened native PDF pages=${created.pageCount} path=$localPath")
            } catch (throwable: Throwable) {
                Log.e(TAG, "Failed to open native PDF path=$localPath uri=$uri", throwable)
                error = throwable.message ?: "No se pudo abrir la vista nativa del PDF"
                runCatching { created?.close() }
                session = null
            }
        }
        onDispose {
            runCatching { created?.close() }
            session = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B))
            .onSizeChanged { viewWidth = it.width }
    ) {
        when {
            localPath.isNullOrBlank() -> Text(
                "Copiando PDF al dispositivo…",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            error != null -> Text(
                error ?: "",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            pageCount == 0 -> Text(
                "Abriendo PDF…",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            pageMode == PdfPageMode.CONTINUOUS -> ContinuousPages(
                session = session,
                pageCount = pageCount,
                viewWidth = viewWidth,
                aspect = aspect
            )
            else -> PagedPages(
                session = session,
                pageCount = pageCount,
                viewWidth = viewWidth,
                aspect = aspect,
                chrome = true
            )
        }
        LaunchedEffect(session) {
            val current = session ?: return@LaunchedEffect
            aspect = current.pageAspect(0) ?: aspect
        }
    }
}

@Composable
private fun ContinuousPages(
    session: PdfRenderSession?,
    pageCount: Int,
    viewWidth: Int,
    aspect: Float
) {
    val listState = rememberLazyListState()
    val pages = remember(pageCount) { (0 until pageCount).toList() }
    val visiblePage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(pages, key = { it }) { page ->
                PdfPageImage(
                    session = session,
                    pageIndex = page,
                    viewWidth = viewWidth,
                    aspect = aspect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
        Text(
            "$visiblePage / $pageCount",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedPages(
    session: PdfRenderSession?,
    pageCount: Int,
    viewWidth: Int,
    aspect: Float,
    chrome: Boolean
) {
    val pagerState = rememberPagerState(pageCount = { pageCount })
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PdfPageImage(
                    session = session,
                    pageIndex = page,
                    viewWidth = viewWidth,
                    aspect = aspect,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (chrome) {
            Text(
                "${pagerState.currentPage + 1} / $pageCount",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PdfPageImage(
    session: PdfRenderSession?,
    pageIndex: Int,
    viewWidth: Int,
    aspect: Float,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex, session, viewWidth) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pageIndex, session, viewWidth) {
        val current = session ?: return@LaunchedEffect
        val width = viewWidth.coerceAtLeast(1)
        bitmap = current.render(pageIndex, width)
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = "Página ${pageIndex + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .background(Color.White)
        )
    }
}

private class PdfRenderSession(path: String) {
    private val descriptor: ParcelFileDescriptor = run {
        val file = File(path)
        require(file.exists() && file.length() > 0) { "El PDF aún no está en caché" }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    private val renderer = PdfRenderer(descriptor)
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "openreader-pdf").apply { isDaemon = true }
    }
    private val dispatcher = worker.asCoroutineDispatcher()
    private val mutex = Mutex()
    private val cache = object : LinkedHashMap<Int, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>?): Boolean {
            val evict = size > 8
            if (evict) eldest?.value?.recycle()
            return evict
        }
    }
    val pageCount: Int = renderer.pageCount

    suspend fun pageAspect(pageIndex: Int): Float? = withContext(dispatcher) {
        mutex.withLock {
            if (pageIndex !in 0 until pageCount) return@withLock null
            renderer.openPage(pageIndex).use { page ->
                page.width.toFloat() / page.height.toFloat().coerceAtLeast(1f)
            }
        }
    }

    suspend fun render(pageIndex: Int, widthPx: Int): Bitmap? = withContext(dispatcher) {
        mutex.withLock {
            cache[pageIndex]?.let { return@withLock it }
            if (pageIndex !in 0 until pageCount || widthPx <= 1) return@withLock null
            renderer.openPage(pageIndex).use { page ->
                val height = (page.height.toFloat() / page.width * widthPx).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888).also { bmp ->
                    bmp.eraseColor(AndroidColor.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache[pageIndex] = bmp
                }
            }
        }
    }

    fun close() {
        cache.values.forEach { it.recycle() }
        cache.clear()
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
        dispatcher.close()
        worker.shutdown()
    }
}
