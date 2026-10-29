package nl.tcgvault.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val Achtergrond = Color(0xFF080E19)
val Paneel = Color(0xFF131E2E)
val Licht = Color(0xFFF4F7FD)
val Muted = Color(0xFF94A8BD)
val Accent = Color(0xFF5AE4D0)
val Goud = Color(0xFFFFD48D)
val Lijn = Color(0xFF26364A)
val PaneelLicht = Color(0xFF1B2A3F)

private val kleuren = darkColorScheme(
    primary = Accent, onPrimary = Achtergrond,
    secondary = Goud, onSecondary = Achtergrond,
    background = Achtergrond, onBackground = Licht,
    surface = Paneel, onSurface = Licht,
    surfaceVariant = PaneelLicht, onSurfaceVariant = Muted,
    outline = Lijn, outlineVariant = Lijn
)

private val typografie = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
)

@Composable
fun VaultThema(inhoud: @Composable () -> Unit) {
    MaterialTheme(colorScheme = kleuren, typography = typografie, content = inhoud)
}
