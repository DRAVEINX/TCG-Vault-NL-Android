package nl.tcgvault.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Achtergrond = Color(0xFF0B1120)
val Paneel = Color(0xFF142035)
val Licht = Color(0xFFEAF1FF)
val Muted = Color(0xFF9BABBE)
val Accent = Color(0xFF4ED6BE)
val Goud = Color(0xFFFFCB75)

private val kleuren = darkColorScheme(
    primary = Accent, onPrimary = Achtergrond,
    secondary = Goud, onSecondary = Achtergrond,
    background = Achtergrond, onBackground = Licht,
    surface = Paneel, onSurface = Licht,
    surfaceVariant = Color(0xFF1E304A), onSurfaceVariant = Muted
)

@Composable
fun VaultThema(inhoud: @Composable () -> Unit) {
    MaterialTheme(colorScheme = kleuren, content = inhoud)
}
