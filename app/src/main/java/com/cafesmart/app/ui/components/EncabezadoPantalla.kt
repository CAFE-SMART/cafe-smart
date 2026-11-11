package com.cafesmart.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.Spacing

/**
 * Encabezado del Figma (guía "Encabezado de pantalla"): flecha atrás a la izquierda, título centrado y, si se
 * da, una acción a la derecha (por ejemplo recargar).
 */
@Composable
fun EncabezadoPantalla(
    titulo: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
        contentAlignment = Alignment.Center
    ) {
        BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
        Text(text = titulo, style = AppTypography.titleScreen)
        if (accion != null) {
            Box(Modifier.align(Alignment.CenterEnd)) { accion() }
        }
    }
}
