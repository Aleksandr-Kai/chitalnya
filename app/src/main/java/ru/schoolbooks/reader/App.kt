package ru.schoolbooks.reader

import androidx.compose.ui.res.stringResource
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.Velocity
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

/** Re-resolves resources for the language chosen in settings; the activity stays reachable through [findActivity]. */
private class LocalizedContext(base: android.content.Context, private val localized: android.content.res.Resources) :
    android.content.ContextWrapper(base) {
    override fun getResources() = localized
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var c = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun App(model: AppViewModel) {
    val base = LocalContext.current
    val context = remember(base, model.lang) {
        val tag = model.lang.tag
        if (tag == null) {
            base
        } else {
            val config = Configuration(base.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(tag)) }
            LocalizedContext(base, base.createConfigurationContext(config).resources)
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalContext provides context) { AppContent(model) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(model: AppViewModel) {
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
                title = { Text(stringResource(R.string.all_bookmarks)) },
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
                                Text(stringResource(R.string.book_page, bm.book.name, bm.page + 1), fontSize = 18.sp)
                                if (bm.label.isNotEmpty()) {
                                    Text(bm.label, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = { SciTextButton(onClick = { showAllBookmarks = false }) { Text(stringResource(R.string.close)) } },
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
                                model.current?.name ?: stringResource(R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        actions = {
                            if (model.current != null) {
                                SciTextButton(onClick = model::close) { Text(stringResource(R.string.home), fontSize = 16.sp) }
                            }
                            if (model.current == null && model.bookmarks.isNotEmpty()) {
                                SciTextButton(onClick = { showAllBookmarks = true }) { Text(stringResource(R.string.bookmarks_star), fontSize = 16.sp) }
                            }
                            SciTextButton(onClick = { showSettings = true }) {
                                Text(stringResource(R.string.settings), fontSize = 16.sp)
                            }
                            val activity = LocalContext.current.findActivity()
                            Button(
                                onClick = { activity?.finish() },
                                modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                                shape = if (LocalSciFi.current) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(20.dp),
                            ) { Text(stringResource(R.string.close), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
                            model.error?.let { stringResource(it) } ?: stringResource(R.string.no_folder),
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
            Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SciTextButton(onClick = model::rescan, enabled = !model.scanning) { Text(stringResource(R.string.refresh)) }
        }
        HorizontalDivider()
        when {
            model.scanning -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            library == null -> Text(model.error?.let { stringResource(it) } ?: stringResource(R.string.no_folder), Modifier.padding(20.dp))
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

private const val DOWNLOAD_URL = "https://aleksandr-kai.github.io/chitalnya/#install"
private const val TELEGRAM_URL = "https://t.me/aleksandr_kai"

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
}

/** The app has no internet access, so "checking" means showing the version and where new ones are published. */
@Composable
private fun UpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val failed = stringResource(R.string.link_failed)
    val open = { url: String ->
        runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { android.widget.Toast.makeText(context, failed, android.widget.Toast.LENGTH_LONG).show() }
        Unit
    }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.version_line, appVersion()), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.update_text), fontSize = 16.sp)
                Button(onClick = { open(DOWNLOAD_URL) }) { Text(stringResource(R.string.open_download), fontSize = 16.sp) }
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(DOWNLOAD_URL.removePrefix("https://"), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
                Text(stringResource(R.string.support_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.support_text), fontSize = 16.sp)
                OutlinedButton(onClick = { open(TELEGRAM_URL) }) { Text(stringResource(R.string.write_telegram), fontSize = 16.sp) }
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(TELEGRAM_URL.removePrefix("https://"), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { SciTextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** A titled panel that groups related settings. */
@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        val shape = RoundedCornerShape(16.dp)
        val frame = if (LocalSciFi.current) {
            Modifier.hudFrame(14.dp, alpha = 0.7f)
        } else {
            Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape)
        }
        Column(
            Modifier.fillMaxWidth().then(frame).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** Fixed-width drop-down list used for the theme and language pickers. */
@Composable
private fun <T> ChoiceDropdown(items: List<T>, selected: T, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.width(320.dp)) {
            Text(label(selected), fontSize = 18.sp, modifier = Modifier.weight(1f))
            Text("▾", fontSize = 18.sp)
        }
        val sciFi = LocalSciFi.current
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.width(320.dp),
            shape = if (sciFi) hudShape(12.dp) else androidx.compose.material3.MenuDefaults.shape,
            containerColor = if (sciFi) HudPanel.copy(alpha = 0.95f) else androidx.compose.material3.MenuDefaults.containerColor,
            border = if (sciFi) androidx.compose.foundation.BorderStroke(1.dp, HudCyan.copy(alpha = 0.75f)) else null,
        ) {
            items.forEach { item ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text((if (item == selected) "✓  " else "     ") + label(item), fontSize = 18.sp) },
                    onClick = {
                        onPick(item)
                        open = false
                    },
                )
            }
        }
    }
}

/** Switch and strength slider for the paper-white filter, with a swatch that shows the effect. */
@Composable
private fun DimControls(model: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { model.chooseDimWhite(!model.dimWhite) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Switch(checked = model.dimWhite, onCheckedChange = model::chooseDimWhite)
            Text(stringResource(R.string.dim_label), fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(44.dp).height(44.dp)
                    .background(model.pageTint ?: androidx.compose.ui.graphics.Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline),
            )
            androidx.compose.material3.Slider(
                value = model.dimLevel,
                onValueChange = model::chooseDimLevel,
                enabled = model.dimWhite,
                modifier = Modifier.width(280.dp).padding(horizontal = 12.dp),
            )
            Text("${(model.dimLevel * 100).toInt()}%", fontSize = 16.sp)
        }
    }
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
            Text(stringResource(R.string.settings), fontSize = 28.sp, fontWeight = FontWeight.Bold)

            SettingsSection(stringResource(R.string.settings_folder)) {
                Text(
                    model.rootUri?.let { Library.displayPath(it, stringResource(R.string.internal_storage)) }
                        ?: stringResource(R.string.not_chosen),
                    fontSize = 18.sp,
                )
                model.library?.let { Text(stringResource(R.string.books_found, it.bookCount), fontSize = 16.sp) }
                model.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, fontSize = 16.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { picker.launch(model.rootUri) }) {
                        Text(stringResource(R.string.choose_folder), fontSize = 18.sp)
                    }
                    if (model.rootUri != null) {
                        OutlinedButton(onClick = model::rescan, enabled = !model.scanning) {
                            Text(
                                if (model.scanning) stringResource(R.string.scanning) else stringResource(R.string.rescan),
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }

            SettingsSection(stringResource(R.string.section_reading)) {
                Row(
                    Modifier.fillMaxWidth().clickable { model.chooseImmersive(!model.immersive) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Switch(checked = model.immersive, onCheckedChange = model::chooseImmersive)
                    Text(stringResource(R.string.hide_bars), fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().clickable { model.chooseThumbStrip(!model.thumbStrip) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Switch(checked = model.thumbStrip, onCheckedChange = model::chooseThumbStrip)
                    Text(stringResource(R.string.thumb_strip), fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
                DimControls(model)
            }

            SettingsSection(stringResource(R.string.section_appearance)) {
                Text(stringResource(R.string.theme), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                ChoiceDropdown(
                    items = AppTheme.entries, selected = model.theme, label = { stringResource(it.label) },
                    onPick = model::chooseTheme,
                )
                Text(stringResource(R.string.language), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                ChoiceDropdown(
                    items = AppLang.entries, selected = model.lang, label = { stringResource(it.label) },
                    onPick = model::chooseLang,
                )
            }

            var showUpdate by remember { mutableStateOf(false) }
            SettingsSection(stringResource(R.string.section_about)) {
                Text(stringResource(R.string.version_line, appVersion()), fontSize = 18.sp)
                OutlinedButton(onClick = { showUpdate = true }) {
                    Text(stringResource(R.string.check_update), fontSize = 18.sp)
                }
            }
            if (showUpdate) UpdateDialog(onDismiss = { showUpdate = false })

            if (model.rootUri != null) {
                Button(onClick = onClose) { Text(stringResource(R.string.done), fontSize = 18.sp) }
            }
            Text(
                stringResource(R.string.settings_hint),
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
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.open_failed), fontSize = 20.sp) }
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
    var showDim by remember { mutableStateOf(false) }
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
        val viewH = maxHeight
        val density = LocalDensity.current
        // Pages are centred, so the free space on the right is half of what zooming out leaves over.
        val freeRight = (viewW - viewW * zoom) / 2
        val inlineStrip = model.thumbStrip && freeRight >= 72.dp
        // Without the inline strip it can still be pulled over the page.
        var peek by remember { mutableStateOf(false) }
        BackHandler(enabled = peek && !inlineStrip) { peek = false }
        // Like the menu drawer: a leftward drag the page can't use any further pulls the strip out.
        val pullPx = with(density) { 72.dp.toPx() }
        val pullOut = remember(inlineStrip) {
            object : NestedScrollConnection {
                var dragged = 0f
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && !inlineStrip && available.x < 0f) {
                        dragged += available.x
                        if (-dragged > pullPx) {
                            dragged = 0f
                            peek = true
                        }
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    dragged = 0f
                    return Velocity.Zero
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(pullOut)
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
                        PdfPage(d, i, bucket, ratios[i], model.pageTint)
                    }
                }
            }
        }
        val currentPage = listState.firstVisibleItemIndex
        if (inlineStrip) {
            ThumbStrip(
                doc = d, ratios = ratios, current = currentPage,
                width = minOf(freeRight - 14.dp, 200.dp), height = maxHeight - 100.dp,
                onPick = { scope.launch { listState.scrollToItem(it) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                tint = model.pageTint,
            )
        } else {
            AnimatedVisibility(
                visible = peek,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { peek = false },
                ) {
                    val panel = if (LocalSciFi.current) {
                        Modifier.hudFrame(14.dp, alpha = 0.95f)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .then(panel)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .padding(10.dp)
                            .pointerInput(Unit) {
                                // A swipe back towards the edge tucks the strip away again.
                                var total = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { total = 0f },
                                    onHorizontalDrag = { _, dx -> total += dx },
                                    onDragEnd = { if (total > 48.dp.toPx()) peek = false },
                                )
                            },
                    ) {
                        ThumbStrip(
                            doc = d, ratios = ratios, current = currentPage,
                            width = 150.dp, height = viewH - 120.dp,
                            onPick = { scope.launch { listState.scrollToItem(it) } },
                            tint = model.pageTint,
                        )
                    }
                }
            }
        }
        val marks = model.bookmarksOf(book)
        val marked = marks.any { it.page == currentPage }
        Row(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1.0 is the default: the page fills the screen width.
            if (kotlin.math.abs(zoom - 1f) > 0.01f) {
                Button(onClick = { zoom = 1f }) {
                    Text(stringResource(R.string.fit_width), fontSize = 16.sp)
                }
            }
            Button(onClick = { showDim = true }) { Text("◐", fontSize = 20.sp) }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = {
                if (marked) model.removeBookmark(book, currentPage) else labelPage = currentPage
            }) { Text(if (marked) stringResource(R.string.remove_bookmark) else stringResource(R.string.add_bookmark), fontSize = 16.sp) }
            Button(onClick = { showBookmarks = true }) {
                Text(stringResource(R.string.bookmarks) + if (marks.isEmpty()) "" else " (${marks.size})", fontSize = 16.sp)
            }
            Button(onClick = { showJump = true }) {
                Text("${currentPage + 1} / ${d.pageCount}", fontSize = 16.sp)
            }
        }
    }

    if (showDim) {
        AppDialog(
            onDismissRequest = { showDim = false },
            title = { Text(stringResource(R.string.dim_title)) },
            text = { DimControls(model) },
            confirmButton = { SciTextButton(onClick = { showDim = false }) { Text(stringResource(R.string.done)) } },
        )
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
            title = { Text(stringResource(R.string.jump_title, d.pageCount)) },
            text = {
                OutlinedTextField(
                    text, { text = it.filter(Char::isDigit) },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go() }),
                )
            },
            confirmButton = { SciTextButton(onClick = go) { Text(stringResource(R.string.go)) } },
            dismissButton = { SciTextButton(onClick = { showJump = false }) { Text(stringResource(R.string.cancel)) } },
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
            title = { Text(stringResource(R.string.bookmark_on_page, page + 1)) },
            text = {
                OutlinedTextField(
                    label, { label = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_hint)) },
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                )
            },
            confirmButton = { SciTextButton(onClick = save) { Text(stringResource(R.string.save)) } },
            dismissButton = { SciTextButton(onClick = { labelPage = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showBookmarks) {
        val marks = model.bookmarksOf(book)
        AppDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text(stringResource(R.string.bookmarks)) },
            text = {
                if (marks.isEmpty()) {
                    Text(stringResource(R.string.no_bookmarks))
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
                                    Text(stringResource(R.string.page_n, m.page + 1), fontSize = 18.sp)
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
            confirmButton = { SciTextButton(onClick = { showBookmarks = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun PdfPage(doc: PdfDoc, index: Int, width: Int, ratio: Float, tint: androidx.compose.ui.graphics.Color?) {
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
            colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it, androidx.compose.ui.graphics.BlendMode.Modulate) },
        )
    } else {
        Box(Modifier.fillMaxWidth().aspectRatioBox(ratio).background(tint ?: androidx.compose.ui.graphics.Color.White))
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
