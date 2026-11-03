package com.cafesmart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing

// Hoja inferior comun: icono en circulo, titulo, mensaje, un boton y un enlace.
// La usan cancelar compra y, mas adelante, ventas, secado y gastos.
@Composable
fun ConfirmSheet(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit,
    linkText: String,
    onLink: () -> Unit,
    // Azul por defecto; cancelar lo cambia a rojo.
    actionContainerColor: Color = Primary,
    actionContentColor: Color = OnPrimary
) {
    // Hoja del equipo. Cerrarla deslizando hace lo mismo que el enlace.
    AppBottomSheet(onDismiss = onLink) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            // Icono centrado en un circulo de color.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(text = title, style = AppTypography.titleModal, textAlign = TextAlign.Center)
            Text(text = message, style = AppTypography.bodyMd, textAlign = TextAlign.Center)
            // Boton principal de la hoja.
            Button(
                onClick = onAction,
                shape = AppShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionContainerColor,
                    contentColor = actionContentColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(text = actionText, style = AppTypography.buttonPrimary.copy(color = actionContentColor))
            }
            // Enlace secundario debajo del boton.
            TextButton(onClick = onLink) {
                Text(text = linkText, style = AppTypography.buttonLink)
            }
        }
    }
}
