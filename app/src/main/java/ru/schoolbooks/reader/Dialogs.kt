package ru.schoolbooks.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Dialog that matches the dashboard-style cards in the sci-fi theme and is a plain AlertDialog elsewhere. */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    if (!LocalSciFi.current) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
        )
        return
    }
    Dialog(onDismissRequest) {
        CompositionLocalProvider(
            LocalDialogScope provides true,
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        ) {
            Column(Modifier.widthIn(min = 300.dp, max = 560.dp).hudFrame().padding(24.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace)) { title() }
                Spacer(Modifier.height(16.dp))
                text()
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

val HudCyan = Color(0xFF22C7E0)
val HudPanel = Color(0xFF0B1A2B)
val LocalDialogScope = compositionLocalOf { false }

fun hudShape(cut: Dp = 16.dp) = CutCornerShape(topStart = cut, bottomEnd = cut)

/** Dashboard card: translucent navy panel, cyan outline, chamfered corners with bright corner brackets. */
fun Modifier.hudFrame(cut: Dp = 16.dp, alpha: Float = 0.86f): Modifier {
    val shape = hudShape(cut)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(HudPanel.copy(alpha = alpha), Color(0xFF081422).copy(alpha = alpha))))
        .border(1.dp, HudCyan.copy(alpha = 0.75f), shape)
        .drawBehind {
            val c = cut.toPx()
            val len = 18.dp.toPx()
            val sw = 2.5.dp.toPx()
            val col = Color(0xFF5CE8FF)
            drawLine(col, Offset(0f, c), Offset(c, 0f), sw)
            drawLine(col, Offset(size.width, size.height - c), Offset(size.width - c, size.height), sw)
            drawLine(col, Offset(size.width - len, 0f), Offset(size.width, 0f), sw)
            drawLine(col, Offset(size.width, 0f), Offset(size.width, len), sw)
            drawLine(col, Offset(0f, size.height - len), Offset(0f, size.height), sw)
            drawLine(col, Offset(0f, size.height), Offset(len, size.height), sw)
        }
}

/** TextButton that turns into a flat dark-blue dashboard button inside sci-fi dialogs. */
@Composable
fun SciTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    if (LocalSciFi.current && LocalDialogScope.current) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12263D), contentColor = Color(0xFFBFEFFF)),
            border = BorderStroke(1.dp, HudCyan.copy(alpha = 0.4f)),
        ) { content() }
    } else {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled) { content() }
    }
}
