package ru.schoolbooks.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Page previews for the free space beside the page. Scrolling it changes nothing;
 * it follows [current] (kept centred) and a tap on a preview calls [onPick].
 */
@Composable
fun ThumbStrip(
    doc: PdfDoc,
    ratios: List<Float>,
    current: Int,
    width: Dp,
    height: Dp,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }
    val heightPx = with(density) { height.toPx() }
    // Rendered at a bucketed width so small layout changes reuse cached bitmaps.
    val renderWidth = (((widthPx.toInt() + 63) / 64) * 64).coerceAtLeast(64)
    val state = rememberLazyListState()

    LaunchedEffect(current, widthPx, heightPx) {
        val itemHeight = (widthPx * ratios[current]).toInt()
        state.animateScrollToItem(current, -((heightPx - itemHeight) / 2).toInt())
    }

    LazyColumn(
        state = state,
        modifier = modifier.width(width).height(height),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ratios.size) { i ->
            var bitmap by remember(i) { mutableStateOf<Bitmap?>(doc.cached(i, renderWidth)) }
            LaunchedEffect(i, renderWidth) {
                if (bitmap == null) bitmap = withContext(Dispatchers.Default) { doc.render(i, renderWidth) }
            }
            val shape = RoundedCornerShape(4.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / ratios[i])
                    .clip(shape)
                    .background(Color.White)
                    .clickable { onPick(i) }
                    .border(
                        if (i == current) 3.dp else 1.dp,
                        if (i == current) MaterialTheme.colorScheme.primary else Color(0x66000000),
                        shape,
                    ),
            ) {
                bitmap?.let {
                    Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                }
                Text(
                    "${i + 1}", fontSize = 11.sp, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).background(Color(0x99000000)).padding(horizontal = 4.dp),
                )
            }
        }
    }
}
