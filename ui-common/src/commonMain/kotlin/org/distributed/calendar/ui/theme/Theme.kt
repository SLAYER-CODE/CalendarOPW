package org.distributed.calendar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    secondary = Color(0xFF66BB6A),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE0E0E0),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outlineVariant = Color(0xFF333333)
)

@Composable
fun CalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme) {
        content()
    }
}
