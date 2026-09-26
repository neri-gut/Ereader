package org.openreader.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.openreader.core.model.ThemeType

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B4332),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF40916C),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF4F1EA),
    onSurfaceVariant = Color(0xFF3D3D3D)
)

private val SepiaColors = lightColorScheme(
    primary = Color(0xFF5F4B32),
    onPrimary = Color(0xFFF4ECD8),
    secondary = Color(0xFF8C7355),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF5F4B32),
    surface = Color(0xFFF4ECD8),
    onSurface = Color(0xFF5F4B32),
    surfaceVariant = Color(0xFFE6D7B8),
    onSurfaceVariant = Color(0xFF6B5840)
)

private val NightColors = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF121212),
    secondary = Color(0xFFB0BEC5),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

@Composable
fun OpenReaderTheme(
    themeType: ThemeType = ThemeType.LIGHT,
    content: @Composable () -> Unit
) {
    val scheme = when (themeType) {
        ThemeType.LIGHT -> LightColors
        ThemeType.SEPIA -> SepiaColors
        ThemeType.NIGHT -> NightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
