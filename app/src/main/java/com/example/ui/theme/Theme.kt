package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = compositionLocalOf { true }
val LocalThemeMode = compositionLocalOf { "Dark Navy" }

/**
 * Reactive application-wide theme controller so switching between "Dark Navy",
 * "Light", and "System" in Profile settings immediately updates both Material 3
 * color schemes and custom screen palettes across LingoSphere.
 */
object LingoThemeController {
  private var isInitialized = false
  var themeMode by mutableStateOf("Dark Navy")
  var systemIsDark by mutableStateOf(true)

  fun initialize(context: android.content.Context) {
    if (isInitialized) return
    isInitialized = true
    try {
      val offlinePrefs = context.getSharedPreferences("lingosphere_offline_memory", android.content.Context.MODE_PRIVATE)
      val saved = offlinePrefs.getString("global_theme_mode", null)
      if (!saved.isNullOrBlank()) {
        themeMode = saved
      }
    } catch (_: Exception) {}
  }

  val isDarkActive: Boolean
    get() = when (themeMode.trim().lowercase()) {
      "light" -> false
      "system" -> systemIsDark
      else -> true
    }

  fun resolveDarkTheme(mode: String = themeMode, sysDark: Boolean = systemIsDark): Boolean {
    return when (mode.trim().lowercase()) {
      "light" -> false
      "system" -> sysDark
      else -> true
    }
  }

  // Adaptive surface & text tokens aligned to #070B14 canvas
  val screenBgTop: Color
    get() = if (isDarkActive) Color(0xFF070B14) else Color(0xFFF6F8FC)

  val screenBgMiddle: Color
    get() = if (isDarkActive) Color(0xFF070B14) else Color(0xFFF6F8FC)

  val screenBgBottom: Color
    get() = if (isDarkActive) Color(0xFF070B14) else Color(0xFFF1F5F9)

  val cardBg: Color
    get() = if (isDarkActive) Color(0xFF111622) else Color(0xFFFFFFFF)

  val cardElevatedBg: Color
    get() = if (isDarkActive) Color(0xFF182033) else Color(0xFFEEF2F8)

  val cardBorder: Color
    get() = if (isDarkActive) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

  val dividerColor: Color
    get() = if (isDarkActive) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)

  val textPrimary: Color
    get() = if (isDarkActive) Color(0xFFFFFFFF) else Color(0xFF0B0F1A)

  val textSecondary: Color
    get() = if (isDarkActive) Color(0xFFA8B3CF) else Color(0xFF475569)

  val textAccentBlue: Color
    get() = if (isDarkActive) Color(0xFF2DD4FF) else Color(0xFF2563EB)

  val accentCyan: Color
    get() = if (isDarkActive) Color(0xFF2DD4FF) else Color(0xFF0284C7)

  val bottomBarBg: Color
    get() = if (isDarkActive) Color(0xFF121826) else Color(0xFFFFFFFF)

  val bottomBarIndicator: Color
    get() = if (isDarkActive) Color(0xFF182033) else Color(0xFFE0F2FE)
}

private val DarkColorScheme = darkColorScheme(
  primary = LingoBrandIndigo,
  onPrimary = Color(0xFF0B0F1A),
  primaryContainer = LingoNavyCard,
  onPrimaryContainer = LingoBrandCyan,
  secondary = LingoBrandCyan,
  onSecondary = Color(0xFF0B0F1A),
  secondaryContainer = LingoNavyCard,
  onSecondaryContainer = Color.White,
  tertiary = LingoGold,
  onTertiary = Color(0xFF0B0F1A),
  background = LingoNavyDeep,
  onBackground = LingoTextPrimaryLight,
  surface = LingoNavySurface,
  onSurface = LingoTextPrimaryLight,
  surfaceVariant = LingoNavyCard,
  onSurfaceVariant = LingoTextSecondaryLight,
  outline = LingoNavyStroke,
  error = LingoCoral,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF4F5FEF),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE8ECFF),
  onPrimaryContainer = Color(0xFF2633A5),
  secondary = Color(0xFF0284C7),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0F2FE),
  onSecondaryContainer = Color(0xFF0369A1),
  tertiary = Color(0xFFD97706),
  onTertiary = Color.White,
  background = LingoLightBg,
  onBackground = LingoTextPrimaryDark,
  surface = LingoLightSurface,
  onSurface = LingoTextPrimaryDark,
  surfaceVariant = LingoLightCard,
  onSurfaceVariant = LingoTextSecondaryDark,
  outline = LingoLightStroke,
  error = LingoCoral,
  onError = Color.White
)

@Composable
fun LingoSphereTheme(
  darkTheme: Boolean? = null,
  themeMode: String? = null,
  content: @Composable () -> Unit,
) {
  val sysDark = isSystemInDarkTheme()
  val effectiveMode = themeMode ?: if (darkTheme != null) {
    if (darkTheme) "Dark Navy" else "Light"
  } else {
    LingoThemeController.themeMode
  }
  LaunchedEffect(sysDark, themeMode, darkTheme) {
    if (LingoThemeController.systemIsDark != sysDark) {
      LingoThemeController.systemIsDark = sysDark
    }
    if (!themeMode.isNullOrBlank() && LingoThemeController.themeMode != themeMode) {
      LingoThemeController.themeMode = themeMode
    } else if (darkTheme != null) {
      val mapped = if (darkTheme) "Dark Navy" else "Light"
      if (LingoThemeController.themeMode != mapped) {
        LingoThemeController.themeMode = mapped
      }
    }
  }

  val resolvedDark = darkTheme ?: LingoThemeController.resolveDarkTheme(effectiveMode, sysDark)
  val colorScheme = if (resolvedDark) DarkColorScheme else LightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    var lastAppliedDark by androidx.compose.runtime.remember { mutableStateOf<Boolean?>(true) }
    androidx.compose.runtime.LaunchedEffect(resolvedDark) {
      if (lastAppliedDark == resolvedDark) return@LaunchedEffect
      lastAppliedDark = resolvedDark
      val activity = view.context as? Activity ?: return@LaunchedEffect
      val window = activity.window
      val controller = WindowCompat.getInsetsController(window, view)
      controller.isAppearanceLightStatusBars = !resolvedDark
      controller.isAppearanceLightNavigationBars = !resolvedDark
    }
  }

  val currentDensity = LocalDensity.current
  val responsiveDensity = androidx.compose.runtime.remember(currentDensity.density, currentDensity.fontScale) {
    Density(
      density = currentDensity.density,
      fontScale = currentDensity.fontScale.coerceIn(0.85f, 1.22f)
    )
  }

  CompositionLocalProvider(
    LocalIsDarkTheme provides resolvedDark,
    LocalThemeMode provides effectiveMode,
    LocalDensity provides responsiveDensity
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
