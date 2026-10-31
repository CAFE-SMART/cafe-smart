package com.cafesmart.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cafesmart.app.R
import androidx.compose.ui.graphics.Color



val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

@Stable
object AppTypography {
    val displayValor = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = PrimaryDark
    )
    val displayValorXl = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        color = PrimaryDark
    )
    val titleExito = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = TextPrimary
    )
    val titleModal = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    )
    val titleScreen = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = TextPrimary
    )
    val titleCard = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = TextPrimary
    )
    val labelSection = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = TextSecondary
    )
    val labelField = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = TextStrong
    )
    val bodyLg = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = TextPrimary
    )
    val bodyMd = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = TextSecondary
    )
    val valueStrong = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = TextPrimary
    )
    val valueStrongBold = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = PrimaryDark
    )
    val caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = TextMuted
    )
    val helper = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = TextMuted
    )
    val pill = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = SuccessText
    )
    val buttonPrimary = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = OnPrimary
    )
    val buttonLink = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = Primary
    )
}

private val InterDefaults = Typography().let { material ->
    Typography(
        displayLarge = material.displayLarge.copy(fontFamily = Inter),
        displayMedium = material.displayMedium.copy(fontFamily = Inter),
        displaySmall = material.displaySmall.copy(fontFamily = Inter),
        headlineLarge = material.headlineLarge.copy(fontFamily = Inter),
        headlineMedium = material.headlineMedium.copy(fontFamily = Inter),
        headlineSmall = material.headlineSmall.copy(fontFamily = Inter),
        titleLarge = material.titleLarge.copy(fontFamily = Inter),
        titleMedium = material.titleMedium.copy(fontFamily = Inter),
        titleSmall = material.titleSmall.copy(fontFamily = Inter),
        bodyLarge = material.bodyLarge.copy(fontFamily = Inter),
        bodyMedium = material.bodyMedium.copy(fontFamily = Inter),
        bodySmall = material.bodySmall.copy(fontFamily = Inter),
        labelLarge = material.labelLarge.copy(fontFamily = Inter),
        labelMedium = material.labelMedium.copy(fontFamily = Inter),
        labelSmall = material.labelSmall.copy(fontFamily = Inter)
    )
}

// Tipografia Material completa en Inter; los roles del design system
// quedan mapeados encima para que los componentes que usan Typography
// directamente (TextField, Chip, etc.) también se vean en Inter.
val Typography = InterDefaults.copy(
    bodyLarge = AppTypography.bodyLg,
    bodyMedium = AppTypography.bodyMd,
    titleLarge = AppTypography.titleScreen,
    titleMedium = AppTypography.titleCard,
    labelLarge = AppTypography.buttonPrimary.copy(
        color = Color.Unspecified
    )
)
