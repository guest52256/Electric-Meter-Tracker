package sadaqat.kinzadigitalhub.electricmetertracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElectricCyan = Color(0xFF00B4D8)
private val ElectricDarkCyan = Color(0xFF0077B6)
private val LightSurface = Color(0xFFF8F9FA)
private val DarkSurface = Color(0xFF121417)
private val PrimaryDark = Color(0xFF90E0EF)

private val LightColorScheme = lightColorScheme(
    primary = ElectricDarkCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCAF0F8),
    onPrimaryContainer = Color(0xFF03045E),
    secondary = Color(0xFF0096C7),
    onSecondary = Color.White,
    background = Color(0xFFF4F6F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9ECEF),
    onSurface = Color(0xFF1B263B)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF03045E),
    primaryContainer = Color(0xFF0077B6),
    onPrimaryContainer = Color(0xFFCAF0F8),
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    background = DarkSurface,
    surface = Color(0xFF1E2228),
    surfaceVariant = Color(0xFF2B303A),
    onSurface = Color(0xFFE0E1DD)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
