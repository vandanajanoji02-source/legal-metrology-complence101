package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
// SmartVerify Material 3 Color Scheme
// All colors reference Color.kt — no hardcoded hex values here.
// =============================================================================

private val LightColorScheme = lightColorScheme(
    // Primary
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryContainerDark,
    // Secondary
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color(0xFF0C4A6E),
    // Tertiary (Accent/Teal)
    tertiary = Accent,
    onTertiary = OnPrimary,
    tertiaryContainer = AccentContainer,
    onTertiaryContainer = AccentContainerDark,
    // Background
    background = Background,
    onBackground = OnBackground,
    // Surface
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    // Error
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    // Outline
    outline = Outline,
    outlineVariant = Slate200,
    // Inverse
    inverseSurface = Slate800,
    inverseOnSurface = Slate50,
    inversePrimary = PrimaryLight
)

private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = PrimaryLight,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = PrimaryContainer,
    // Secondary
    secondary = SecondaryDark,
    onSecondary = Color(0xFF0C4A6E),
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = SecondaryContainer,
    // Tertiary
    tertiary = AccentDark,
    onTertiary = AccentContainerDark,
    tertiaryContainer = AccentContainerDark,
    onTertiaryContainer = AccentContainer,
    // Background
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    // Surface
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    // Error
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    // Outline
    outline = OutlineDark,
    outlineVariant = Slate800,
    // Inverse
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    inversePrimary = Primary
)

@Composable
fun SmartVerifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartVerifyTypography,
        content = content
    )
}
