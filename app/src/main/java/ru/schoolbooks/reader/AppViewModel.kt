package ru.schoolbooks.reader

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("settings", 0)

    var rootUri by mutableStateOf<Uri?>(prefs.getString(KEY_ROOT, null)?.let(Uri::parse))
        private set
    var theme by mutableStateOf(
        prefs.getString(KEY_THEME, null)?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.LIBRARY,
    )
        private set

    fun chooseTheme(value: AppTheme) {
        theme = value
        prefs.edit().putString(KEY_THEME, value.name).apply()
    }

    var immersive by mutableStateOf(prefs.getBoolean(KEY_IMMERSIVE, true))
        private set

    fun chooseImmersive(value: Boolean) {
        immersive = value
        prefs.edit().putBoolean(KEY_IMMERSIVE, value).apply()
    }

    /** Entries are "documentUri<TAB>pageIndex<TAB>label". */
    var bookmarks by mutableStateOf(prefs.getStringSet(KEY_BOOKMARKS, emptySet())!!.toSet())
        private set

    class Bookmark(val page: Int, val label: String)

    class LibraryBookmark(val book: Book, val page: Int, val label: String)

    private fun parse(entry: String): Pair<String, Bookmark>? {
        val parts = entry.split('\t', limit = 3)
        val page = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return parts[0] to Bookmark(page, parts.getOrElse(2) { "" })
    }

    fun bookmarksOf(book: Book): List<Bookmark> {
        val uri = book.uri.toString()
        return bookmarks.mapNotNull(::parse).filter { it.first == uri }.map { it.second }.sortedBy { it.page }
    }

    /** Adds a bookmark on [page] or replaces its label if one is already there. */
    fun putBookmark(book: Book, page: Int, label: String) {
        removeBookmark(book, page)
        val clean = label.replace(Regex("[\\t\\n\\r]"), " ").trim()
        bookmarks = bookmarks + "${book.uri}\t$page\t$clean"
        prefs.edit().putStringSet(KEY_BOOKMARKS, bookmarks).apply()
    }

    fun removeBookmark(book: Book, page: Int) {
        val uri = book.uri.toString()
        bookmarks = bookmarks.filterNot { e -> parse(e)?.let { it.first == uri && it.second.page == page } == true }.toSet()
        prefs.edit().putStringSet(KEY_BOOKMARKS, bookmarks).apply()
    }

    /** Bookmarks of books still present in the library, ordered by title then page. */
    fun allBookmarks(): List<LibraryBookmark> {
        val lib = library ?: return emptyList()
        return bookmarks.mapNotNull { e ->
            val (uri, b) = parse(e) ?: return@mapNotNull null
            find(lib, uri)?.let { LibraryBookmark(it, b.page, b.label) }
        }.sortedWith(compareBy({ it.book.name }, { it.page }))
    }

    var thumbStrip by mutableStateOf(prefs.getBoolean(KEY_THUMBS, true))
        private set

    fun chooseThumbStrip(value: Boolean) {
        thumbStrip = value
        prefs.edit().putBoolean(KEY_THUMBS, value).apply()
    }

    var library by mutableStateOf<Folder?>(null)
        private set
    var scanning by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var current by mutableStateOf<Book?>(null)
        private set

    /** Folder names from the library root down to the folder shown on the home screen. */
    var homePath by mutableStateOf<List<String>>(emptyList())
        private set

    fun homeFolder(root: Folder): Folder =
        homePath.fold(root) { f, name -> f.folders.firstOrNull { it.name == name } ?: return f }

    fun openFolder(name: String) { homePath = homePath + name }
    fun goHome(depth: Int) { homePath = homePath.take(depth) }
    fun close() { current = null }

    private var restoreBook: String? = null

    init {
        rescan()
    }

    fun setRoot(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        rootUri?.takeIf { it != uri }?.let {
            runCatching { resolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        rootUri = uri
        prefs.edit().putString(KEY_ROOT, uri.toString()).apply()
        current = null
        restoreBook = null
        rescan()
    }

    fun rescan() {
        val root = rootUri ?: return
        scanning = true
        error = null
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { Library.scan(getApplication(), root) }
            }.onSuccess { lib ->
                library = lib
                if (lib.bookCount == 0) error = "В выбранной папке нет PDF-файлов"
                restoreBook?.let { saved -> current = find(lib, saved) }
                restoreBook = null
            }.onFailure {
                library = null
                error = "Не удалось прочитать папку. Выберите её заново."
            }
            scanning = false
        }
    }

    fun open(book: Book) {
        current = book
        prefs.edit().putString(KEY_LAST_BOOK, book.uri.toString()).apply()
    }

    fun savedPage(book: Book): Int = prefs.getInt(PAGE_PREFIX + book.uri, 0)

    /** Zoom is kept per book and per orientation; 1.0 means the page fills the screen width. */
    fun savedZoom(book: Book, landscape: Boolean): Float = prefs.getFloat(zoomKey(book, landscape), 1f)

    fun saveZoom(book: Book, landscape: Boolean, zoom: Float) {
        prefs.edit().putFloat(zoomKey(book, landscape), zoom).apply()
    }

    private fun zoomKey(book: Book, landscape: Boolean) =
        "$ZOOM_PREFIX${if (landscape) "land" else "port"}:${book.uri}"

    fun savePage(book: Book, page: Int) {
        prefs.edit().putInt(PAGE_PREFIX + book.uri, page).apply()
    }

    private fun find(folder: Folder, uri: String): Book? =
        folder.books.firstOrNull { it.uri.toString() == uri }
            ?: folder.folders.firstNotNullOfOrNull { find(it, uri) }

    private companion object {
        const val KEY_ROOT = "root"
        const val KEY_THEME = "theme"
        const val KEY_THUMBS = "thumb_strip"
        const val KEY_BOOKMARKS = "bookmarks"
        const val KEY_IMMERSIVE = "immersive"
        const val KEY_LAST_BOOK = "last_book"
        const val PAGE_PREFIX = "page:"
        const val ZOOM_PREFIX = "zoom:"
    }
}
