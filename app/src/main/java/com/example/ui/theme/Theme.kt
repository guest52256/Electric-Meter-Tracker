package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun getThemeColorScheme(
    palette: AppColorPalette,
    isDark: Boolean
): ColorScheme {
    return when (palette) {
        AppColorPalette.ELECTRIC_BLUE -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF60A5FA),
                onPrimary = Slate950,
                primaryContainer = Color(0xFF1D4ED8),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFF67E8F9),
                onSecondary = Slate950,
                secondaryContainer = Color(0xFF0E7490),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFFFBBF24),
                background = Color(0xFF090D16),
                surface = Color(0xFF111827),
                surfaceVariant = Color(0xFF1F2937),
                onBackground = Slate100,
                onSurface = Slate100,
                onSurfaceVariant = Slate300,
                error = AlertRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF2563EB),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDBEAFE),
                onPrimaryContainer = Color(0xFF1E40AF),
                secondary = Color(0xFF0891B2),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCFFAFE),
                onSecondaryContainer = Color(0xFF155E75),
                tertiary = Color(0xFFD97706),
                background = Color(0xFFF8FAFC),
                surface = Color.White,
                surfaceVariant = Color(0xFFF1F5F9),
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceVariant = Color(0xFF475569),
                error = AlertRed,
                onError = Color.White
            )
        }

        AppColorPalette.EMERALD_ECO -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFF34D399),
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF065F46),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondary = Color(0xFF6EE7B7),
                onSecondary = Color(0xFF022C22),
                secondaryContainer = Color(0xFF047857),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFF38BDF8),
                background = Color(0xFF061A14),
                surface = Color(0xFF0A2920),
                surfaceVariant = Color(0xFF133E33),
                onBackground = Color(0xFFECFDF5),
                onSurface = Color(0xFFECFDF5),
                onSurfaceVariant = Color(0xFFA7F3D0),
                error = AlertRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF065F46),
                secondary = Color(0xFF0D9488),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCCFBF1),
                onSecondaryContainer = Color(0xFF115E59),
                tertiary = Color(0xFF0284C7),
                background = Color(0xFFF4FBF7),
                surface = Color.White,
                surfaceVariant = Color(0xFFE6F4EA),
                onBackground = Color(0xFF062E23),
                onSurface = Color(0xFF062E23),
                onSurfaceVariant = Color(0xFF2D5A4C),
                error = AlertRed,
                onError = Color.White
            )
        }

        AppColorPalette.AMBER_POWER -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFBBF24),
                onPrimary = Color(0xFF451A03),
                primaryContainer = Color(0xFF92400E),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondary = Color(0xFFFB923C),
                onSecondary = Color(0xFF431407),
                secondaryContainer = Color(0xFF9A3412),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFFF87171),
                background = Color(0xFF140E06),
                surface = Color(0xFF24180A),
                surfaceVariant = Color(0xFF382613),
                onBackground = Color(0xFFFEF3C7),
                onSurface = Color(0xFFFEF3C7),
                onSurfaceVariant = Color(0xFFFDE68A),
                error = AlertRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFEF3C7),
                onPrimaryContainer = Color(0xFF78350F),
                secondary = Color(0xFFEA580C),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFFEDD5),
                onSecondaryContainer = Color(0xFF9A3412),
                tertiary = Color(0xFFDC2626),
                background = Color(0xFFFFFDF5),
                surface = Color.White,
                surfaceVariant = Color(0xFFFEF7E6),
                onBackground = Color(0xFF451A03),
                onSurface = Color(0xFF451A03),
                onSurfaceVariant = Color(0xFF78350F),
                error = AlertRed,
                onError = Color.White
            )
        }

        AppColorPalette.CYBER_NEON -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFA78BFA),
                onPrimary = Color(0xFF2E1065),
                primaryContainer = Color(0xFF5B21B6),
                onPrimaryContainer = Color(0xFFEDE9FE),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF082F49),
                secondaryContainer = Color(0xFF0369A1),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFFF472B6),
                background = Color(0xFF0B0817),
                surface = Color(0xFF17112E),
                surfaceVariant = Color(0xFF251C48),
                onBackground = Color(0xFFF5F3FF),
                onSurface = Color(0xFFF5F3FF),
                onSurfaceVariant = Color(0xFFDDD6FE),
                error = AlertRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF7C3AED),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEDE9FE),
                onPrimaryContainer = Color(0xFF4C1D95),
                secondary = Color(0xFF0284C7),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE0F2FE),
                onSecondaryContainer = Color(0xFF075985),
                tertiary = Color(0xFFDB2777),
                background = Color(0xFFFAF8FF),
                surface = Color.White,
                surfaceVariant = Color(0xFFF3E8FF),
                onBackground = Color(0xFF2E1065),
                onSurface = Color(0xFF2E1065),
                onSurfaceVariant = Color(0xFF5B21B6),
                error = AlertRed,
                onError = Color.White
            )
        }

        AppColorPalette.CRIMSON_FLUX -> if (isDark) {
            darkColorScheme(
                primary = Color(0xFFFB7185),
                onPrimary = Color(0xFF4C0519),
                primaryContainer = Color(0xFF9F1239),
                onPrimaryContainer = Color(0xFFFFE4E6),
                secondary = Color(0xFFF43F5E),
                onSecondary = Color(0xFF4C0519),
                secondaryContainer = Color(0xFF881337),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFFF59E0B),
                background = Color(0xFF14060A),
                surface = Color(0xFF240B13),
                surfaceVariant = Color(0xFF3B1220),
                onBackground = Color(0xFFFFF1F2),
                onSurface = Color(0xFFFFF1F2),
                onSurfaceVariant = Color(0xFFFECDD3),
                error = AlertRed,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFE11D48),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE4E6),
                onPrimaryContainer = Color(0xFF881337),
                secondary = Color(0xFFBE123C),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFFECDD3),
                onSecondaryContainer = Color(0xFF4C0519),
                tertiary = Color(0xFFD97706),
                background = Color(0xFFFFF8F9),
                surface = Color.White,
                surfaceVariant = Color(0xFFFFECEF),
                onBackground = Color(0xFF4C0519),
                onSurface = Color(0xFF4C0519),
                onSurfaceVariant = Color(0xFF881337),
                error = AlertRed,
                onError = Color.White
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorPalette: AppColorPalette = AppColorPalette.ELECTRIC_BLUE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getThemeColorScheme(colorPalette, isDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
