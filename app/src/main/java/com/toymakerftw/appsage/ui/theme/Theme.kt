package com.toymakerftw.appsage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Material 3 Expressive Color Schemes
private val LightExpressiveColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),
)

private val DarkExpressiveColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6750A4),
)

// Material 3 Expressive Typography
private val ExpressiveTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(57f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(-0.25f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(45f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(52f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(44f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(32f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(40f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(32f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.15f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.25f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
        lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
)

// Material 3 Expressive Shape System
val ExpressiveShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun AppsageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkExpressiveColorScheme
        else -> LightExpressiveColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content
    )
}