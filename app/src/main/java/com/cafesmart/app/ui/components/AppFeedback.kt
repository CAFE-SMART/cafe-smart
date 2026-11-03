
package com.cafesmart.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import com.cafesmart.app.ui.theme.*

enum class FeedbackType {
    SUCCESS,
    ERROR
}

data class FeedbackDetail(
    val label: String,
    val value: String
)

// ÍCONO DE ÉXITO O ERROR
@Composable
fun AppFeedbackIcon(
    type: FeedbackType,
    modifier: Modifier = Modifier
) {
    val success = type == FeedbackType.SUCCESS

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                if (success) SuccessSolid else ErrorIcon
            ),

        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (success) "✓" else "×",
            color = OnPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// BOTÓN PARA CERRAR
@Composable
fun AppFeedbackCloseButton(
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Text(
            text = "×",
            fontSize = 28.sp,
            color = TextMuted
        )
    }
}

// BOTÓN REUTILIZABLE
@Composable
fun AppFeedbackButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,

        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),

        shape = RoundedCornerShape(12.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (primary) Primary else ButtonSoft,

            contentColor =
                if (primary) OnPrimary else PrimaryDark
        )
    ) {
        Text(
            text = text,

            color =
                if (primary) OnPrimary else PrimaryDark,

            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

// RESUMEN REUTILIZABLE
@Composable
fun AppFeedbackSummary(
    title: String,
    details: List<FeedbackDetail>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = title,
            style = AppTypography.labelField,
            color = TextPrimary
        )

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard,

            border = BorderStroke(
                width = 1.dp,
                color = BorderDefault
            )
        ) {
            Column {

                details.forEachIndexed { index, detail ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = detail.label,
                            color = TextSecondary,
                            style = AppTypography.bodyMd,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = detail.value,
                            color = TextPrimary,
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (index < details.lastIndex) {
                        HorizontalDivider(
                            color = BorderDefault
                        )
                    }
                }
            }
        }
    }
}

// PANTALLA COMPLETA DE ÉXITO
@Composable
fun AppSuccessScreen(
    title: String,
    message: String,
    summaryTitle: String,
    details: List<FeedbackDetail>,
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(45.dp))

            AppFeedbackIcon(
                type = FeedbackType.SUCCESS
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = title,
                style = AppTypography.titleExito,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = message,
                style = AppTypography.bodyMd,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            if (details.isNotEmpty()) {

                AppFeedbackSummary(
                    title = summaryTitle,
                    details = details
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AppFeedbackButton(
                text = primaryButtonText,
                primary = true,
                onClick = onPrimaryClick
            )

            AppFeedbackButton(
                text = secondaryButtonText,
                primary = false,
                onClick = onSecondaryClick
            )
        }
    }
}

// VENTANA REUTILIZABLE DE ERROR
@Composable
fun AppErrorDialog(
    title: String,
    message: String,
    primaryButtonText: String = "Reintentar",
    secondaryButtonText: String = "Regresar",
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Dialog(
        onDismissRequest = onBack
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),

            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            shadowElevation = 16.dp
        ) {

            Column(
                modifier = Modifier.padding(22.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {

                    AppFeedbackCloseButton(
                        onClick = onBack
                    )
                }

                AppFeedbackIcon(
                    type = FeedbackType.ERROR
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    text = title,
                    style = AppTypography.titleExito,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    style = AppTypography.bodyMd,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(26.dp))

                AppFeedbackButton(
                    text = primaryButtonText,
                    primary = true,
                    onClick = onRetry
                )

                Spacer(Modifier.height(10.dp))

                AppFeedbackButton(
                    text = secondaryButtonText,
                    primary = false,
                    onClick = onBack
                )
            }
        }
    }
}

// VISTA PREVIA DE ÉXITO
@Preview(
    name = "Café Smart - Éxito",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CafeSmartFeedbackSuccessPreview() {

    CafesmartTheme {
        AppSuccessScreen(
            title = "Operación exitosa",
            message = "La operación se completó.",
            summaryTitle = "Resumen",

            details = listOf(
                FeedbackDetail(
                    label = "Estado",
                    value = "Completado"
                )
            ),

            primaryButtonText = "Ir a Inicio",
            secondaryButtonText = "Regresar",
            onPrimaryClick = {},
            onSecondaryClick = {}
        )
    }
}

// VISTA PREVIA DE ERROR
@Preview(
    name = "Café Smart - Error",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun CafeSmartFeedbackErrorPreview() {

    CafesmartTheme {
        AppErrorDialog(
            title = "¡Ups! Hubo un problema",
            message = "No pudimos completar la operación.",
            onRetry = {},
            onBack = {}
        )
    }
}
