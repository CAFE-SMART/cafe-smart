package com.cafesmart.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = ButtonSoft,
    onPrimaryContainer = PrimaryDark,
    secondary = PrimaryDark,
    onSecondary = OnPrimary,
    secondaryContainer = SurfaceSelected,
    onSecondaryContainer = PrimaryDark,
    tertiary = SuccessSolid,
    onTertiary = OnPrimary,
    tertiaryContainer = SuccessBg,
    onTertiaryContainer = SuccessText,
    background = SurfaceScreen,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondary,
    outline = BorderField,
    outlineVariant = BorderDefault,
    error = ErrorDot,
    onError = OnPrimary,
    errorContainer = ErrorBg,
    onErrorContainer = ErrorText,
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceCard,
    inversePrimary = SurfaceSelected
)

val AppShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

object Spacing {
    val s4 = 4.dp
    val s6 = 6.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp // margen lateral de pantalla
    val s28 = 28.dp
    val s48 = 48.dp // relleno superior del encabezado
}

@Composable
fun CafesmartTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
