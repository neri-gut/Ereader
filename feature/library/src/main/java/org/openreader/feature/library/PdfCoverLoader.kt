package org.openreader.feature.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfCoverLoader {
    suspend fun load(context: Context, contentUri: String, fileHash: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val cache = File(context.cacheDir, "covers").apply { mkdirs() }
            val cached = File(cache, "${fileHash.take(24)}.jpg")
            if (cached.exists() && cached.length() > 0) {
                return@withContext android.graphics.BitmapFactory.decodeFile(cached.absolutePath)
            }
            val uri = Uri.parse(contentUri)
            val pfd = runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")
            }.getOrNull() ?: return@withContext null
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount <= 0) return@withContext null
                    renderer.openPage(0).use { page ->
                        val width = 360
                        val height = (page.height.toFloat() / page.width * width).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cached.outputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
                        }
                        bmp
                    }
                }
            }
        }
}
