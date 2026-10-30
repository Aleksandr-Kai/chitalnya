package ru.schoolbooks.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Home(model: AppViewModel, library: Folder) {
    val folder = model.homeFolder(library)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { model.goHome(0) }) { Text("Все учебники", fontSize = 18.sp) }
            model.homePath.forEachIndexed { i, name ->
                Text("›", fontSize = 18.sp)
                TextButton(onClick = { model.goHome(i + 1) }) { Text(name, fontSize = 18.sp) }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(190.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(folder.folders) { sub -> FolderTile(sub) { model.openFolder(sub.name) } }
            items(folder.books) { book -> BookTile(book, model.savedPage(book)) { model.open(book) } }
        }
    }
}

@Composable
private fun FolderTile(folder: Folder, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (LocalSciFi.current) {
            Galaxy(folder.name, Modifier.fillMaxWidth().aspectRatio(1f))
        } else if (LocalLibrary.current) {
            CatalogDrawer(folder.bookCount, Modifier.fillMaxWidth().aspectRatio(1f))
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).padding(12.dp).clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Text("📁", fontSize = 64.sp) }
        }
        Caption(folder.name, "${folder.bookCount} учебн.", center = true)
    }
}

/** A card-catalogue drawer: oak front, brass label holder and pull. */
@Composable
private fun CatalogDrawer(count: Int, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val front = androidx.compose.ui.geometry.Rect(w * 0.06f, h * 0.18f, w * 0.94f, h * 0.84f)
        val r = androidx.compose.ui.geometry.CornerRadius(w * 0.05f)
        drawRoundRect(Color(0x66000000), front.topLeft + Offset(0f, h * 0.025f), front.size, r)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9A6338), Color(0xFF6E4126)), front.top, front.bottom), front.topLeft, front.size, r)
        val inset = w * 0.045f
        drawRoundRect(
            Color(0x55000000), front.topLeft + Offset(inset, inset), androidx.compose.ui.geometry.Size(front.width - 2 * inset, front.height - 2 * inset),
            androidx.compose.ui.geometry.CornerRadius(w * 0.03f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        val plate = androidx.compose.ui.geometry.Rect(w * 0.3f, h * 0.3f, w * 0.7f, h * 0.45f)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE9C57A), Brass), plate.top, plate.bottom), plate.topLeft, plate.size, androidx.compose.ui.geometry.CornerRadius(4f))
        drawRoundRect(Color(0xFFF6EBD0), plate.topLeft + Offset(6f, 6f), androidx.compose.ui.geometry.Size(plate.width - 12f, plate.height - 12f), androidx.compose.ui.geometry.CornerRadius(2f))
        val knob = Offset(w * 0.5f, h * 0.62f)
        drawCircle(Color(0x66000000), w * 0.055f, knob + Offset(0f, 4f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF2D58F), Brass, Color(0xFF8A6422)), knob - Offset(5f, 5f), w * 0.07f), w * 0.055f, knob)
    }
}

@Composable
private fun BookTile(book: Book, savedPage: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    var cover by remember(book.uri) { mutableStateOf<Bitmap?>(Covers.cached(book.uri)) }
    LaunchedEffect(book.uri) { if (cover == null) cover = Covers.load(context, book.uri) }
    val shape = RoundedCornerShape(14.dp)
    val scheme = MaterialTheme.colorScheme
    val library = LocalLibrary.current
    val (fill, edge) = if (library) {
        Brush.verticalGradient(listOf(Color(0xFF4A3024), Leather)) to
            Brush.linearGradient(listOf(Brass.copy(alpha = 0.9f), Color(0xFF8A6422)))
    } else if (LocalSciFi.current) {
        Brush.verticalGradient(listOf(Color(0xFF16204A), SpacePanel)) to
            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.8f), NeonViolet.copy(alpha = 0.5f)))
    } else {
        Brush.verticalGradient(listOf(scheme.surface, scheme.surface)) to
            Brush.linearGradient(listOf(scheme.outlineVariant, scheme.outlineVariant))
    }
    val sciFi = LocalSciFi.current
    val frame = if (sciFi) {
        Modifier.hudFrame(14.dp, alpha = 0.9f).clickable(onClick = onClick).padding(10.dp)
    } else {
        Modifier.clip(shape).background(fill).border(1.dp, edge, shape).clickable(onClick = onClick).padding(8.dp)
    }
    CompositionLocalProvider(
        LocalTextStyle provides if (sciFi) LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace) else LocalTextStyle.current,
    ) {
    Column(frame) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.72f)
                .clip(if (sciFi) RectangleShape else RoundedCornerShape(8.dp))
                .then(if (sciFi) Modifier.border(1.dp, HudCyan.copy(alpha = 0.45f)) else Modifier)
                .background(Color(0xFF0A0F2A)),
            contentAlignment = Alignment.Center,
        ) {
            val c = cover
            if (c != null) {
                Image(c.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            } else {
                Text("📄", fontSize = 48.sp)
            }
        }
        Caption(book.name, if (sciFi) (if (savedPage > 0) "стр. ${savedPage + 1}" else "новая") else null)
    }
    }
}

@Composable
private fun Caption(title: String, subtitle: String?, center: Boolean = false) {
    val align = if (center) TextAlign.Center else TextAlign.Start
    Text(
        title, fontSize = 16.sp, fontWeight = FontWeight.Medium, minLines = 2, maxLines = 2, textAlign = align,
        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
    )
    if (subtitle != null) Text(subtitle, fontSize = 13.sp, textAlign = align, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
}
