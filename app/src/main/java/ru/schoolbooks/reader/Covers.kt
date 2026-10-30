package ru.schoolbooks.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/** First-page thumbnails: rendered once, then kept in memory and on disk. */
object Covers {
    private const val WIDTH = 400
    private val memory = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val limit = Semaphore(2)

    fun cached(uri: Uri): Bitmap? = memory.get(uri.toString())

    suspend fun load(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        memory.get(key)?.let { return@withContext it }
        limit.withPermit {
            val file = File(File(context.cacheDir, "covers").apply { mkdirs() }, "${key.hashCode()}.jpg")
            val bitmap = file.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                ?: render(context, uri)?.also { b ->
                    runCatching { file.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) } }
                }
            bitmap?.also { memory.put(key, it) }
        }
    }

    private fun render(context: Context, uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")!!.use { fd ->
            PdfRenderer(fd).use { r ->
                r.openPage(0).use { p ->
                    val h = (WIDTH * p.height.toFloat() / p.width).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(WIDTH, h, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(Color.WHITE)
                        p.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }.getOrNull()
}
