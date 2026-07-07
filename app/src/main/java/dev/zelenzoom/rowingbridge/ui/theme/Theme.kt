package dev.zelenzoom.rowingbridge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Semantic mapping used across the app: primary=green (start/connected/good),
// secondary=orange (recording in progress/paused), error=red (discard/lost
// connection). All built from the standard Material Design color palette
// (see Color.kt) rather than one-off hex constants per screen.

private val LightColors = lightColorScheme(
    primary = GreenA400,
    onPrimary = White,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    secondary = Orange800,
    onSecondary = White,
    secondaryContainer = Orange100,
    onSecondaryContainer = OrangeE65100,
    tertiary = Blue700,
    error = Red700,
    onError = White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey800,
    outline = Grey500,
)

private val DarkColors = darkColorScheme(
    primary = GreenA400,
    onPrimary = Green900,
    primaryContainer = Green800,
    onPrimaryContainer = Green100,
    secondary = Orange300,
    onSecondary = OrangeE65100,
    secondaryContainer = Orange800,
    onSecondaryContainer = Orange100,
    tertiary = Blue300,
    error = Red300,
    onError = Red900,
    errorContainer = Red800,
    onErrorContainer = Red100,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey300,
    outline = Grey500,
)

@Composable
fun RowingBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
