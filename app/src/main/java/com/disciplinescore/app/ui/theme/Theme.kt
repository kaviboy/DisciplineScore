package com.disciplinescore.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.disciplinescore.app.R

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class AccentTheme(val label: String, val swatch: Color) {
    AMBER("Amber", Color(0xFF8C4E12)),
    FOREST("Forest", Color(0xFF1F6B4A)),
    OCEAN("Ocean", Color(0xFF1D5C91)),
    BERRY("Berry", Color(0xFF8E3D6B)),
    SLATE("Slate", Color(0xFF3E4C59)),
}

private data class AccentSpec(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightSecondary: Color,
    val lightOnSecondary: Color,
    val lightTertiary: Color,
    val lightPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkSecondary: Color,
    val darkOnSecondary: Color,
    val darkTertiary: Color,
    val darkPrimaryContainer: Color,
)

private val Accents = mapOf(
    AccentTheme.AMBER to AccentSpec(
        lightPrimary = Color(0xFF8C4E12),
        lightOnPrimary = Color(0xFFFFFBF5),
        lightSecondary = Color(0xFF1F6B4A),
        lightOnSecondary = Color(0xFFF3EFE6),
        lightTertiary = Color(0xFF8A4B32),
        lightPrimaryContainer = Color(0xFFF6E2C8),
        darkPrimary = Color(0xFFE2A04A),
        darkOnPrimary = Color(0xFF2A1C08),
        darkSecondary = Color(0xFF8FCBB0),
        darkOnSecondary = Color(0xFF10241A),
        darkTertiary = Color(0xFFE7A090),
        darkPrimaryContainer = Color(0xFF3A2A16),
    ),
    AccentTheme.FOREST to AccentSpec(
        lightPrimary = Color(0xFF1F6B4A),
        lightOnPrimary = Color(0xFFF3EFE6),
        lightSecondary = Color(0xFF8C4E12),
        lightOnSecondary = Color(0xFFFFFBF5),
        lightTertiary = Color(0xFF2F6F6A),
        lightPrimaryContainer = Color(0xFFD7EADF),
        darkPrimary = Color(0xFF8FCBB0),
        darkOnPrimary = Color(0xFF10241A),
        darkSecondary = Color(0xFFE2A04A),
        darkOnSecondary = Color(0xFF2A1C08),
        darkTertiary = Color(0xFFA7D4C4),
        darkPrimaryContainer = Color(0xFF163028),
    ),
    AccentTheme.OCEAN to AccentSpec(
        lightPrimary = Color(0xFF1D5C91),
        lightOnPrimary = Color(0xFFF4F8FC),
        lightSecondary = Color(0xFF1F6B4A),
        lightOnSecondary = Color(0xFFF3EFE6),
        lightTertiary = Color(0xFF3E6A8A),
        lightPrimaryContainer = Color(0xFFD6E6F5),
        darkPrimary = Color(0xFF9CC4EA),
        darkOnPrimary = Color(0xFF0E2436),
        darkSecondary = Color(0xFF8FCBB0),
        darkOnSecondary = Color(0xFF10241A),
        darkTertiary = Color(0xFFC5D8EA),
        darkPrimaryContainer = Color(0xFF163044),
    ),
    AccentTheme.BERRY to AccentSpec(
        lightPrimary = Color(0xFF8E3D6B),
        lightOnPrimary = Color(0xFFFFF7FB),
        lightSecondary = Color(0xFF1F6B4A),
        lightOnSecondary = Color(0xFFF3EFE6),
        lightTertiary = Color(0xFF8C4E12),
        lightPrimaryContainer = Color(0xFFF6D9E8),
        darkPrimary = Color(0xFFF0B4D0),
        darkOnPrimary = Color(0xFF3A1428),
        darkSecondary = Color(0xFF8FCBB0),
        darkOnSecondary = Color(0xFF10241A),
        darkTertiary = Color(0xFFE2A04A),
        darkPrimaryContainer = Color(0xFF3A2030),
    ),
    AccentTheme.SLATE to AccentSpec(
        lightPrimary = Color(0xFF3E4C59),
        lightOnPrimary = Color(0xFFF7F5F1),
        lightSecondary = Color(0xFF1F6B4A),
        lightOnSecondary = Color(0xFFF3EFE6),
        lightTertiary = Color(0xFF8C4E12),
        lightPrimaryContainer = Color(0xFFE4E1DA),
        darkPrimary = Color(0xFFD5D0C6),
        darkOnPrimary = Color(0xFF1C1915),
        darkSecondary = Color(0xFF8FCBB0),
        darkOnSecondary = Color(0xFF10241A),
        darkTertiary = Color(0xFFE2A04A),
        darkPrimaryContainer = Color(0xFF2A3038),
    ),
)

private val Paper = Color(0xFFF3EFE6)
private val PaperSurface = Color(0xFFFFFBF5)
private val PaperContainer = Color(0xFFE7E0D4)
private val Ink = Color(0xFF1C1915)
private val InkMuted = Color(0xFF5E584F)
private val PaperLine = Color(0xFFD4CBBC)

private val Night = Color(0xFF101318)
private val NightSurface = Color(0xFF1A1F27)
private val NightContainer = Color(0xFF232A34)
private val NightInk = Color(0xFFF3EDE3)
private val NightMuted = Color(0xFFB7AFA3)
private val NightLine = Color(0xFF3A414C)

private fun AccentSpec.lightScheme() = lightColorScheme(
    background = Paper,
    surface = PaperSurface,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperContainer,
    primary = lightPrimary,
    onPrimary = lightOnPrimary,
    primaryContainer = lightPrimaryContainer,
    onPrimaryContainer = Ink,
    secondary = lightSecondary,
    onSecondary = lightOnSecondary,
    secondaryContainer = lightSecondary.copy(alpha = 0.16f),
    tertiary = lightTertiary,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
    outline = PaperLine,
    error = Color(0xFF9C3B2E),
)

private fun AccentSpec.darkScheme() = darkColorScheme(
    background = Night,
    surface = NightSurface,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightContainer,
    primary = darkPrimary,
    onPrimary = darkOnPrimary,
    primaryContainer = darkPrimaryContainer,
    onPrimaryContainer = NightInk,
    secondary = darkSecondary,
    onSecondary = darkOnSecondary,
    secondaryContainer = darkSecondary.copy(alpha = 0.18f),
    tertiary = darkTertiary,
    onBackground = NightInk,
    onSurface = NightInk,
    onSurfaceVariant = NightMuted,
    outline = NightLine,
    error = Color(0xFFE7A090),
)

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Settings theme needs an activity window")
}

fun resolveDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemDark
}

private val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
)

private fun TextStyle.withGoogleSans() = copy(fontFamily = GoogleSans)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSans,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
).run {
    copy(
        displayMedium = displayMedium.withGoogleSans(),
        displaySmall = displaySmall.withGoogleSans(),
        headlineLarge = headlineLarge.withGoogleSans(),
        headlineSmall = headlineSmall.withGoogleSans(),
        titleSmall = titleSmall.withGoogleSans(),
        bodySmall = bodySmall.withGoogleSans(),
        labelMedium = labelMedium.withGoogleSans(),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun DisciplineTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentTheme = AccentTheme.AMBER,
    content: @Composable () -> Unit,
) {
    val dark = resolveDarkTheme(mode, isSystemInDarkTheme())
    val spec = Accents.getValue(accent)
    val scheme = if (dark) spec.darkScheme() else spec.lightScheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity().window
            val background = scheme.background.toArgb()
            window.decorView.setBackgroundColor(background)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
