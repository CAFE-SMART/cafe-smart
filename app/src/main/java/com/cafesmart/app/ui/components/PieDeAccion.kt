package com.cafesmart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard

/**
 * Pie de pantalla (guía del Design System "Pie de acción"): línea superior, botón principal y, si se da,
 * un enlace debajo. Relleno 16 / 20 / 28 / 20.
 */
@Composable
fun PieDeAccion(
    textoBoton: String,
    botonActivo: Boolean,
    onBoton: () -> Unit,
    modifier: Modifier = Modifier,
    textoEnlace: String? = null,
    onEnlace: () -> Unit = {},
    cargando: Boolean = false,
) {
    Column(modifier.fillMaxWidth().background(SurfaceCard)) {
        HorizontalDivider(color = BorderDefault)
        Column(
            modifier = Modifier.padding(start = Spacing.s20, top = Spacing.s16, end = Spacing.s20, bottom = Spacing.s28),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s8)
        ) {
            ContinueButton(text = textoBoton, onClick = onBoton, enabled = botonActivo, cargando = cargando)
            if (textoEnlace != null) {
                TextButton(onClick = onEnlace, modifier = Modifier.height(44.dp)) {
                    Text(text = textoEnlace, style = AppTypography.buttonLink.copy(color = Primary))
                }
            }
        }
    }
}
