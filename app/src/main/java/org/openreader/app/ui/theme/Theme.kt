package org.openreader.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B4332),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF40916C),
    background = Color(0xFFF4F1EA),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFE7E1D4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF95D5B2),
    onPrimary = Color(0xFF081C15),
    secondary = Color(0xFFD8F3DC),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun OpenReaderTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
