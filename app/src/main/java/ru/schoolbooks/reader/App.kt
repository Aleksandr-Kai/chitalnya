package ru.schoolbooks.reader

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(model: AppViewModel) {
    BooksTheme(model.theme) {
      Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (LocalSciFi.current) SpaceBackground() else if (LocalLibrary.current) LibraryBackground()
        var showSettings by rememberSaveable { mutableStateOf(model.rootUri == null) }
        var showAllBookmarks by remember { mutableStateOf(false) }
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        if (showSettings) {
            SettingsScreen(model, onClose = { showSettings = false })
            return@Box
        }

        if (showAllBookmarks) {
            val all = model.allBookmarks()
            AppDialog(
                onDismissRequest = { showAllBookmarks = false },
                title = { Text("Все закладки") },
                text = {
                    LazyColumn {
                        items(all.size) { i ->
                            val bm = all[i]
                            Column(
                                Modifier.fillMaxWidth().clickable {
                                    model.savePage(bm.book, bm.page)
                                    model.open(bm.book)
                                    showAllBookmarks = false
                                }.padding(vertical = 12.dp),
                            ) {
                                Text("${bm.book.name} — стр. ${bm.page + 1}", fontSize = 18.sp)
                                if (bm.label.isNotEmpty()) {
                                    Text(bm.label, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = { SciTextButton(onClick = { showAllBookmarks = false }) { Text("Закрыть") } },
            )
        }

        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(
                    Modifier.width(380.dp),
                    drawerContainerColor = if (LocalSciFi.current) HudPanel.copy(alpha = 0.9f) else DrawerDefaults.containerColor,
                ) {
                    LibraryMenu(model, onPick = {
                        model.open(it)
                        scope.launch { drawer.close() }
                    })
                }
            },
        ) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        navigationIcon = {
                            SciTextButton(onClick = { scope.launch { drawer.open() } }) {
                                Text("☰", fontSize = 26.sp)
                            }
                        },
                        title = {
                            Text(
                                model.current?.name ?: "Библиотека",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        actions = {
                            if (model.current != null) {
                                SciTextButton(onClick = model::close) { Text("Главная", fontSize = 16.sp) }
                            }
                            if (model.current == null && model.bookmarks.isNotEmpty()) {
                                SciTextButton(onClick = { showAllBookmarks = true }) { Text("★ Закладки", fontSize = 16.sp) }
                            }
                            SciTextButton(onClick = { showSettings = true }) {
                                Text("Настройки", fontSize = 16.sp)
                            }
                        },
                    )
                },
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    val book = model.current
                    when {
                        book != null -> {
                            BackHandler { model.close() }
                            PdfViewer(model, book)
                        }
                        model.library != null && model.library!!.bookCount > 0 -> {
                            BackHandler(enabled = model.homePath.isNotEmpty()) {
                                model.goHome(model.homePath.size - 1)
                            }
                            Home(model, model.library!!)
                        }
                        model.scanning -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        else -> Text(
                            model.error ?: "Папка не выбрана",
                            Modifier.align(Alignment.Center).padding(32.dp),
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun LibraryMenu(model: AppViewModel, onPick: (Book) -> Unit) {
    val library = model.library
    val expanded = remember { mutableStateOf(setOf<String>()) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Библиотека", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SciTextButton(onClick = model::rescan, enabled = !model.scanning) { Text("Обновить") }
        }
        HorizontalDivider()
        when {
            model.scanning -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            library == null -> Text(model.error ?: "Папка не выбрана", Modifier.padding(20.dp))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                val rows = ArrayList<MenuRow>()
                flatten(library, "", 0, expanded.value, rows, isRoot = true)
                items(rows.size) { i ->
                    when (val row = rows[i]) {
                        is MenuRow.Dir -> MenuItem(
                            (if (row.open) "▾  📁  " else "▸  📁  ") + row.folder.name + "  (${row.folder.bookCount})",
                            row.depth, bold = true, selected = false,
                        ) {
                            expanded.value = if (row.open) expanded.value - row.path else expanded.value + row.path
                        }
                        is MenuRow.File -> MenuItem(
                            "📄  " + row.book.name, row.depth, bold = false,
                            selected = row.book.uri == model.current?.uri,
                        ) { onPick(row.book) }
                    }
                }
            }
        }
    }
}

private sealed interface MenuRow {
    class Dir(val folder: Folder, val path: String, val depth: Int, val open: Boolean) : MenuRow
    class File(val book: Book, val depth: Int) : MenuRow
}

/** Expanded folders show their subfolders first, then their books, preserving disk layout. */
private fun flatten(
    folder: Folder, path: String, depth: Int, expanded: Set<String>,
    out: MutableList<MenuRow>, isRoot: Boolean = false,
) {
    val items = { d: Int ->
        folder.folders.forEach { sub ->
            val subPath = "$path/${sub.name}"
            val open = subPath in expanded
            out += MenuRow.Dir(sub, subPath, d, open)
            if (open) flatten(sub, subPath, d + 1, expanded, out)
        }
        folder.books.forEach { out += MenuRow.File(it, d) }
    }
    items(depth)
}

@Composable
private fun MenuItem(text: String, depth: Int, bold: Boolean, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = (20 + depth * 22).dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
    )
}

@Composable
private fun SettingsScreen(model: AppViewModel, onClose: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) model.setRoot(uri)
    }
    BackHandler(enabled = model.rootUri != null, onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Transparent) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Папка с учебниками", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                model.rootUri?.let { Library.displayName(it) } ?: "не выбрана",
                fontSize = 18.sp,
            )
            model.library?.let { Text("Найдено учебников: ${it.bookCount}", fontSize = 16.sp) }
            model.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 16.sp) }
            Row(
                Modifier.fillMaxWidth().clickable { model.chooseImmersive(!model.immersive) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Switch(checked = model.immersive, onCheckedChange = model::chooseImmersive)
                Text("  Скрывать системные панели (свайп от края их покажет)", fontSize = 18.sp)
            }
            Row(
                Modifier.fillMaxWidth().clickable { model.chooseThumbStrip(!model.thumbStrip) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Switch(checked = model.thumbStrip, onCheckedChange = model::chooseThumbStrip)
                Text("  Лента превью страниц, если страница уже экрана", fontSize = 18.sp)
            }
            Text("Тема", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            var themeMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { themeMenu = true }, modifier = Modifier.width(320.dp)) {
                    Text(model.theme.label, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text("▾", fontSize = 18.sp)
                }
                val sciFi = LocalSciFi.current
                androidx.compose.material3.DropdownMenu(
                    expanded = themeMenu,
                    onDismissRequest = { themeMenu = false },
                    modifier = Modifier.width(320.dp),
                    shape = if (sciFi) hudShape(12.dp) else androidx.compose.material3.MenuDefaults.shape,
                    containerColor = if (sciFi) HudPanel.copy(alpha = 0.95f) else androidx.compose.material3.MenuDefaults.containerColor,
                    border = if (sciFi) androidx.compose.foundation.BorderStroke(1.dp, HudCyan.copy(alpha = 0.75f)) else null,
                ) {
                    AppTheme.entries.forEach { t ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(
                                    (if (t == model.theme) "✓  " else "     ") + t.label,
                                    fontSize = 18.sp,
                                )
                            },
                            onClick = {
                                model.chooseTheme(t)
                                themeMenu = false
                            },
                        )
                    }
                }
            }
            Button(onClick = { picker.launch(model.rootUri) }) { Text("Выбрать папку…", fontSize = 18.sp) }
            if (model.rootUri != null) {
                OutlinedButton(onClick = model::rescan, enabled = !model.scanning) {
                    Text(if (model.scanning) "Сканирование…" else "Пересканировать", fontSize = 18.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onClose) { Text("Готово", fontSize = 18.sp) }
            }
            Text(
                "Структура вложенных папок сохраняется в меню. Экран не гаснет, пока приложение открыто.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val MIN_ZOOM = 0.3f
private const val MAX_ZOOM = 2.5f

@Composable
private fun PdfViewer(model: AppViewModel, book: Book) {
    val context = LocalContext.current
    var doc by remember(book) { mutableStateOf<PdfDoc?>(null) }
    var ratios by remember(book) { mutableStateOf<List<Float>>(emptyList()) }
    var failed by remember(book) { mutableStateOf(false) }

    LaunchedEffect(book) {
        withContext(Dispatchers.IO) {
            runCatching {
                val d = PdfDoc(context, book.uri)
                doc = d
                ratios = d.pageRatios()
            }.onFailure { failed = true }
        }
    }
    DisposableEffect(book) { onDispose { doc?.close() } }

    if (failed) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Не удалось открыть файл", fontSize = 20.sp) }
        return
    }
    val d = doc
    if (d == null || ratios.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = model.savedPage(book).coerceIn(0, ratios.size - 1),
    )
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var zoom by remember(book, landscape) {
        mutableFloatStateOf(model.savedZoom(book, landscape).coerceIn(MIN_ZOOM, MAX_ZOOM))
    }
    var showJump by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var labelPage by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(book, landscape) {
        // Debounced so a pinch gesture writes once, not on every frame.
        snapshotFlow { zoom }.collectLatest {
            delay(400)
            model.saveZoom(book, landscape, it)
        }
    }

    LaunchedEffect(book, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { model.savePage(book, it) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        val viewW = maxWidth
        val density = LocalDensity.current
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Two-finger pinch only; single-finger drags still reach the scrollers.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.count { it.pressed } >= 2) {
                                zoom = (zoom * event.calculateZoom()).coerceIn(MIN_ZOOM, MAX_ZOOM)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .horizontalScroll(rememberScrollState()),
        ) {
            // Zoomed out, pages stay centred; zoomed in, the row grows and scrolls sideways.
            Box(Modifier.width(maxOf(viewW, viewW * zoom)), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(viewW * zoom),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(d.pageCount) { i ->
                        // Bitmaps are rendered at bucketed widths so pinching doesn't re-render every frame.
                        val px = with(density) { (viewW * zoom).roundToPx() }
                        val bucket = ((px + 255) / 256) * 256
                        PdfPage(d, i, bucket, ratios[i])
                    }
                }
            }
        }
        val currentPage = listState.firstVisibleItemIndex
        // Pages are centred, so the free space on the right is half of what zooming out leaves over.
        val freeRight = (viewW - viewW * zoom) / 2
        if (model.thumbStrip && freeRight >= 72.dp) {
            ThumbStrip(
                doc = d, ratios = ratios, current = currentPage,
                width = minOf(freeRight - 14.dp, 200.dp), height = maxHeight - 100.dp,
                onPick = { scope.launch { listState.scrollToItem(it) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            )
        }
        val marks = model.bookmarksOf(book)
        val marked = marks.any { it.page == currentPage }
        Row(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(MIN_ZOOM) }) { Text("−", fontSize = 20.sp) }
            Button(onClick = { zoom = (zoom + 0.5f).coerceAtMost(MAX_ZOOM) }) { Text("+", fontSize = 20.sp) }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {
                if (marked) model.removeBookmark(book, currentPage) else labelPage = currentPage
            }) { Text(if (marked) "★ Снять закладку" else "☆ Закладка", fontSize = 16.sp) }
            Button(onClick = { showBookmarks = true }) {
                Text("Закладки" + if (marks.isEmpty()) "" else " (${marks.size})", fontSize = 16.sp)
            }
            Button(onClick = { showJump = true }) {
                Text("${currentPage + 1} / ${d.pageCount}", fontSize = 16.sp)
            }
        }
    }

    if (showJump) {
        var text by remember { mutableStateOf("") }
        val go = {
            text.toIntOrNull()?.let { n ->
                scope.launch { listState.scrollToItem((n - 1).coerceIn(0, d.pageCount - 1)) }
            }
            showJump = false
        }
        val focus = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        AppDialog(
            onDismissRequest = { showJump = false },
            title = { Text("Перейти к странице (1–${d.pageCount})") },
            text = {
                OutlinedTextField(
                    text, { text = it.filter(Char::isDigit) },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go() }),
                )
            },
            confirmButton = { SciTextButton(onClick = go) { Text("Перейти") } },
            dismissButton = { SciTextButton(onClick = { showJump = false }) { Text("Отмена") } },
        )
    }

    labelPage?.let { page ->
        val existing = model.bookmarksOf(book).firstOrNull { it.page == page }?.label.orEmpty()
        var label by remember(page) { mutableStateOf(existing) }
        val save = {
            model.putBookmark(book, page, label)
            labelPage = null
        }
        val focus = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(page) { focus.requestFocus() }
        AppDialog(
            onDismissRequest = { labelPage = null },
            title = { Text("Закладка на стр. ${page + 1}") },
            text = {
                OutlinedTextField(
                    label, { label = it },
                    singleLine = true,
                    label = { Text("Подпись (необязательно)") },
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                )
            },
            confirmButton = { SciTextButton(onClick = save) { Text("Сохранить") } },
            dismissButton = { SciTextButton(onClick = { labelPage = null }) { Text("Отмена") } },
        )
    }

    if (showBookmarks) {
        val marks = model.bookmarksOf(book)
        AppDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text("Закладки") },
            text = {
                if (marks.isEmpty()) {
                    Text("Закладок нет. Нажмите «☆ Закладка», чтобы отметить текущую страницу.")
                } else {
                    LazyColumn {
                        items(marks.size) { i ->
                            val m = marks[i]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    Modifier.weight(1f).clickable {
                                        scope.launch { listState.scrollToItem(m.page) }
                                        showBookmarks = false
                                    }.padding(vertical = 12.dp),
                                ) {
                                    Text("Страница ${m.page + 1}", fontSize = 18.sp)
                                    if (m.label.isNotEmpty()) {
                                        Text(m.label, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                SciTextButton(onClick = { showBookmarks = false; labelPage = m.page }) { Text("✎", fontSize = 18.sp) }
                                SciTextButton(onClick = { model.removeBookmark(book, m.page) }) { Text("✕", fontSize = 18.sp) }
                            }
                        }
                    }
                }
            },
            confirmButton = { SciTextButton(onClick = { showBookmarks = false }) { Text("Закрыть") } },
        )
    }
}

@Composable
private fun PdfPage(doc: PdfDoc, index: Int, width: Int, ratio: Float) {
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(doc.cached(index, width)) }
    LaunchedEffect(index, width) {
        bitmap = withContext(Dispatchers.Default) { doc.render(index, width) }
    }
    val shown = bitmap
    if (shown != null) {
        Image(
            shown.asImageBitmap(), null,
            Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White),
            contentScale = ContentScale.FillWidth,
        )
    } else {
        Box(Modifier.fillMaxWidth().aspectRatioBox(ratio).background(androidx.compose.ui.graphics.Color.White))
    }
}

private fun Modifier.aspectRatioBox(ratio: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val w = constraints.maxWidth
        val p = measurable.measure(
            androidx.compose.ui.unit.Constraints.fixed(w, (w * ratio).toInt()),
        )
        layout(w, p.height) { p.place(0, 0) }
    },
)
