package com.narezany.modrinthclient.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Фирменный зелёный Modrinth. */
val ModrinthGreen = Color(0xFF1BD96A)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006D36),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8CF8AF),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF4F6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D5),
    onSecondaryContainer = Color(0xFF0D1F13),
    tertiary = Color(0xFF3A656E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBEEBF4),
    onTertiaryContainer = Color(0xFF001F25),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF70DC92),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005227),
    onPrimaryContainer = Color(0xFF8CF8AF),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3D),
    onSecondaryContainer = Color(0xFFD2E8D5),
    tertiary = Color(0xFFA2CED8),
    onTertiary = Color(0xFF023640),
    tertiaryContainer = Color(0xFF214C56),
    onTertiaryContainer = Color(0xFFBEEBF4),
    background = Color(0xFF0B1F14),
    onBackground = Color(0xFFE1E3DD),
    surface = Color(0xFF0B1F14),
    onSurface = Color(0xFFE1E3DD),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8A938C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/**
 * Expressive-геометрия: крупные скругления вместо обычных «карточных».
 */
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Плотная «плакатная» типографика: тяжёлые начертания и отрицательный трекинг
 * у крупных заголовков — характерная черта Material 3 Expressive.
 */
private val ExpressiveTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModrinthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content
    )
}
