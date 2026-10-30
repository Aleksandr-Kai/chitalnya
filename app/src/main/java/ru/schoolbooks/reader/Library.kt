package ru.schoolbooks.reader

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

class Book(val name: String, val uri: Uri)

class Folder(val name: String, val folders: List<Folder>, val books: List<Book>) {
    val bookCount: Int get() = books.size + folders.sumOf { it.bookCount }
}

object Library {
    private const val PDF_MIME = "application/pdf"

    /** Walks the SAF tree and mirrors its structure; folders without PDFs are dropped. */
    fun scan(context: Context, treeUri: Uri): Folder {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        return walk(context, treeUri, rootId, displayName(rootId))
    }

    fun displayName(treeUri: Uri): String =
        displayName(DocumentsContract.getTreeDocumentId(treeUri))

    private fun displayName(documentId: String): String =
        documentId.substringAfterLast(':').ifEmpty { "primary" }.substringAfterLast('/')
            .ifEmpty { documentId }

    private fun walk(context: Context, treeUri: Uri, documentId: String, name: String): Folder {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val folders = ArrayList<Folder>()
        val books = ArrayList<Book>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        context.contentResolver.query(children, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val childName = c.getString(1)?.takeUnless { it.startsWith(".") } ?: continue
                val mime = c.getString(2)
                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR ->
                        walk(context, treeUri, id, childName).takeIf { it.bookCount > 0 }
                            ?.let(folders::add)
                    mime == PDF_MIME || childName.endsWith(".pdf", ignoreCase = true) ->
                        books += Book(
                            childName.removeSuffix(".pdf").removeSuffix(".PDF"),
                            DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                        )
                }
            }
        }
        folders.sortWith { a, b -> naturalCompare(a.name, b.name) }
        books.sortWith { a, b -> naturalCompare(a.name, b.name) }
        return Folder(name, folders, books)
    }
}

/** Case-insensitive comparison where digit runs compare numerically ("2 класс" < "10 класс"). */
fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        if (a[i].isDigit() && b[j].isDigit()) {
            var ie = i
            while (ie < a.length && a[ie].isDigit()) ie++
            var je = j
            while (je < b.length && b[je].isDigit()) je++
            val na = a.substring(i, ie).trimStart('0')
            val nb = b.substring(j, je).trimStart('0')
            if (na.length != nb.length) return na.length - nb.length
            val cmp = na.compareTo(nb)
            if (cmp != 0) return cmp
            i = ie
            j = je
        } else {
            val cmp = a[i].lowercaseChar().compareTo(b[j].lowercaseChar())
            if (cmp != 0) return cmp
            i++
            j++
        }
    }
    return (a.length - i) - (b.length - j)
}
