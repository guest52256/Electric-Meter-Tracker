package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String, val iconText: String) {
    SYSTEM("System Default", "🌓"),
    LIGHT("Light Mode", "☀️"),
    DARK("Dark Mode", "🌙")
}

enum class AppColorPalette(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    ELECTRIC_BLUE(
        title = "Electric Blue",
        subtitle = "Modern Precision Slates",
        primaryColor = Color(0xFF2563EB),
        secondaryColor = Color(0xFF06B6D4),
        accentColor = Color(0xFF38BDF8)
    ),
    EMERALD_ECO(
        title = "Emerald Eco",
        subtitle = "Clean Energy & Mint",
        primaryColor = Color(0xFF059669),
        secondaryColor = Color(0xFF10B981),
        accentColor = Color(0xFF34D399)
    ),
    AMBER_POWER(
        title = "Sunset Amber",
        subtitle = "Warm Power Grid Glow",
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFFEA580C),
        accentColor = Color(0xFFFBBF24)
    ),
    CYBER_NEON(
        title = "Cyber Neon",
        subtitle = "High-Contrast Tech Pulse",
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFF06B6D4),
        accentColor = Color(0xFFA78BFA)
    ),
    CRIMSON_FLUX(
        title = "Crimson Dynamo",
        subtitle = "Bold Dynamic Energy",
        primaryColor = Color(0xFFE11D48),
        secondaryColor = Color(0xFFF43F5E),
        accentColor = Color(0xFFFB7185)
    )
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow(loadColorPalette())
    val colorPalette: StateFlow<AppColorPalette> = _colorPalette.asStateFlow()

    private fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString("theme_mode", AppThemeMode.LIGHT.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.LIGHT.name)
        } catch (e: Exception) {
            AppThemeMode.LIGHT
        }
    }

    private fun loadColorPalette(): AppColorPalette {
        val saved = prefs.getString("color_palette", AppColorPalette.ELECTRIC_BLUE.name)
        return try {
            AppColorPalette.valueOf(saved ?: AppColorPalette.ELECTRIC_BLUE.name)
        } catch (e: Exception) {
            AppColorPalette.ELECTRIC_BLUE
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setColorPalette(palette: AppColorPalette) {
        prefs.edit().putString("color_palette", palette.name).apply()
        _colorPalette.value = palette
    }
}
