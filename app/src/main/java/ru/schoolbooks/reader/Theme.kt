package ru.schoolbooks.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

val SpaceBg = Color(0xFF05071A)
val SpacePanel = Color(0xFF0E1330)
val NeonCyan = Color(0xFF3DE8FF)
val NeonViolet = Color(0xFFB07CFF)
val NeonPink = Color(0xFFFF5CC8)

private val SciFiColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00222A),
    primaryContainer = Color(0xFF123A66),
    onPrimaryContainer = Color(0xFFD6F7FF),
    secondary = NeonViolet,
    secondaryContainer = Color(0xFF2A1E5C),
    background = SpaceBg,
    surface = SpacePanel,
    onSurface = Color(0xFFE4EBFF),
    onSurfaceVariant = Color(0xFF9FB0D9),
    surfaceVariant = Color(0xFF1A2147),
    error = Color(0xFFFF6B8A),
)

enum class AppTheme(val label: String) {
    LIBRARY("Библиотека"), SCIFI("Фантастика"), LIGHT("Светлая"), DARK("Тёмная"), SYSTEM("Как в системе");

    fun isDark(systemDark: Boolean) = when (this) {
        SCIFI, LIBRARY, DARK -> true
        LIGHT -> false
        SYSTEM -> systemDark
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondaryContainer = Color(0xFFE3E7FA),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6E8F2),
)

private val PlainDarkColors = darkColorScheme(
    primary = Color(0xFF9FB4FF),
    background = Color(0xFF121317),
    surface = Color(0xFF1C1E24),
    surfaceVariant = Color(0xFF2A2D36),
    secondaryContainer = Color(0xFF2E3350),
)

val LocalSciFi = compositionLocalOf { false }
val LocalLibrary = compositionLocalOf { false }

val Mahogany = Color(0xFF24150E)
val Leather = Color(0xFF3B261B)
val Brass = Color(0xFFD4A24C)
val Parchment = Color(0xFFF3E6CF)

private val LibraryColors = darkColorScheme(
    primary = Brass,
    onPrimary = Color(0xFF2B1B0A),
    primaryContainer = Color(0xFF6B4A22),
    onPrimaryContainer = Parchment,
    secondary = Color(0xFFC98B5E),
    secondaryContainer = Color(0xFF5A3A27),
    background = Mahogany,
    surface = Color(0xFF33201A),
    onSurface = Parchment,
    onSurfaceVariant = Color(0xFFCDB894),
    surfaceVariant = Color(0xFF2E1D15),
    outlineVariant = Color(0xFF6B4A33),
    error = Color(0xFFFF8A7A),
)

private fun serifTypography(): Typography {
    val t = Typography()
    fun TextStyle.s() = copy(fontFamily = FontFamily.Serif)
    return Typography(
        displayLarge = t.displayLarge.s(), displayMedium = t.displayMedium.s(), displaySmall = t.displaySmall.s(),
        headlineLarge = t.headlineLarge.s(), headlineMedium = t.headlineMedium.s(), headlineSmall = t.headlineSmall.s(),
        titleLarge = t.titleLarge.s(), titleMedium = t.titleMedium.s(), titleSmall = t.titleSmall.s(),
        bodyLarge = t.bodyLarge.s(), bodyMedium = t.bodyMedium.s(), bodySmall = t.bodySmall.s(),
        labelLarge = t.labelLarge.s(), labelMedium = t.labelMedium.s(), labelSmall = t.labelSmall.s(),
    )
}

@Composable
fun BooksTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = theme.isDark(isSystemInDarkTheme())
    val colors = when {
        theme == AppTheme.SCIFI -> SciFiColors
        theme == AppTheme.LIBRARY -> LibraryColors
        dark -> PlainDarkColors
        else -> LightColors
    }
    val typography = if (theme == AppTheme.LIBRARY) serifTypography() else Typography()
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalSciFi provides (theme == AppTheme.SCIFI),
            LocalLibrary provides (theme == AppTheme.LIBRARY),
            LocalTextStyle provides if (theme == AppTheme.LIBRARY) LocalTextStyle.current.copy(fontFamily = FontFamily.Serif) else LocalTextStyle.current,
            content = content,
        )
    }
}

/** Fixed random starfield with a couple of faint nebula glows. */
@Composable
fun SpaceBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0E2B), SpaceBg, Color(0xFF090418))))
        drawCircle(
            Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.85f, size.height * 0.15f), size.minDimension * 0.7f),
            radius = size.minDimension * 0.7f, center = Offset(size.width * 0.85f, size.height * 0.15f),
        )
        drawCircle(
            Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.10f), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.85f), size.minDimension * 0.8f),
            radius = size.minDimension * 0.8f, center = Offset(size.width * 0.1f, size.height * 0.85f),
        )
        val rnd = Random(7)
        repeat(220) {
            val r = rnd.nextFloat()
            drawCircle(
                Color.White.copy(alpha = 0.25f + rnd.nextFloat() * 0.65f),
                radius = if (r > 0.96f) 2.6f else 1.2f + r,
                center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height),
            )
        }
    }
}

/** Spiral galaxy; shape and tint are derived from the folder name so each looks distinct. */
@Composable
fun Galaxy(seed: String, modifier: Modifier = Modifier) {
    val hues = listOf(NeonCyan, NeonViolet, NeonPink, Color(0xFFFFB84D), Color(0xFF6BFF9E))
    val tint = hues[(seed.hashCode() and 0x7fffffff) % hues.size]
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val rad = size.minDimension / 2
        val rnd = Random(seed.hashCode())
        val tilt = rnd.nextFloat() * PI.toFloat()
        val arms = 2 + rnd.nextInt(2)
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.35f), Color.Transparent), c, rad), rad, c)
        repeat(900) {
            val arm = rnd.nextInt(arms)
            val t = rnd.nextFloat()
            val dist = t * rad * 0.95f
            val angle = tilt + arm * (2 * PI.toFloat() / arms) + t * 4.2f + (rnd.nextFloat() - 0.5f) * 0.55f * (0.4f + t)
            val x = c.x + cos(angle) * dist
            val y = c.y + sin(angle) * dist * 0.62f
            val a = (0.85f * exp(-1.8f * t)).coerceIn(0.1f, 0.9f)
            drawCircle(
                (if (rnd.nextFloat() > 0.7f) Color.White else tint).copy(alpha = a),
                radius = 1f + rnd.nextFloat() * (2.2f - t), center = Offset(x, y),
            )
        }
        drawCircle(Brush.radialGradient(listOf(Color.White, tint.copy(alpha = 0.7f), Color.Transparent), c, rad * 0.28f), rad * 0.28f, c)
    }
}

/** Warm lamp light over dark wood, with a faint row of book spines along the bottom edge. */
@Composable
fun LibraryBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2E1D14), Mahogany, Color(0xFF1A0E09))))
        val rnd = Random(11)
        repeat(60) {
            val y = rnd.nextFloat() * size.height
            drawLine(
                Color(0xFF000000).copy(alpha = 0.05f + rnd.nextFloat() * 0.05f),
                Offset(0f, y), Offset(size.width, y + (rnd.nextFloat() - 0.5f) * 6f),
                strokeWidth = 1f + rnd.nextFloat() * 2f,
            )
        }
        val lamp = Offset(size.width * 0.12f, size.height * 0.04f)
        val reach = size.maxDimension * 0.75f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC86B).copy(alpha = 0.22f), Color.Transparent), lamp, reach), reach, lamp)
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color(0xFF000000).copy(alpha = 0.45f)),
                Offset(size.width / 2, size.height / 2), size.maxDimension * 0.75f,
            ),
        )
        val spines = listOf(Color(0xFF7A2E2E), Color(0xFF2F5A4A), Color(0xFF3B4A7A), Color(0xFF8A6A2E), Color(0xFF5A3A5E), Color(0xFF6B4A33))
        var x = 0f
        val base = size.height
        while (x < size.width) {
            val w = 14f + rnd.nextFloat() * 22f
            val h = 70f + rnd.nextFloat() * 70f
            drawRect(spines[rnd.nextInt(spines.size)].copy(alpha = 0.22f), Offset(x, base - h), androidx.compose.ui.geometry.Size(w - 2f, h))
            x += w
        }
    }
}
