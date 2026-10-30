package ru.schoolbooks.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Thin wrapper over PdfRenderer, which only allows one open page at a time. */
class PdfDoc(context: Context, uri: Uri) : AutoCloseable {
    private val descriptor: ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(uri, "r") ?: error("Не удалось открыть файл")
    private val renderer = PdfRenderer(descriptor)
    private val lock = Mutex()
    private val cache = object : LruCache<Pair<Int, Int>, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
    ) {
        override fun sizeOf(key: Pair<Int, Int>, value: Bitmap) = value.byteCount
    }

    val pageCount: Int = renderer.pageCount

    /** Height / width of every page, so the list can be laid out before rendering. */
    suspend fun pageRatios(): List<Float> = lock.withLock {
        List(pageCount) { i ->
            renderer.openPage(i).use { it.height.toFloat() / it.width }
        }
    }

    fun cached(page: Int, width: Int): Bitmap? = cache.get(page to width)

    suspend fun render(page: Int, width: Int): Bitmap = lock.withLock {
        cache.get(page to width)?.let { return it }
        renderer.openPage(page).use { p ->
            val height = (width * p.height.toFloat() / p.width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            cache.put(page to width, bitmap)
            bitmap
        }
    }

    override fun close() {
        cache.evictAll()
        renderer.close()
        descriptor.close()
    }
}
