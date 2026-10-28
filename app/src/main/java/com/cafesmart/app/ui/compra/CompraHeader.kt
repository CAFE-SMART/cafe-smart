package com.cafesmart.app.ui.compra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.SegmentedProgressBar
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen

const val TOTAL_PASOS_COMPRA = 3

// Parte de arriba de cada paso: ← "Registro de compra", "Paso n de 3", barra de progreso y la pregunta
@Composable
fun CompraHeader(
    paso: Int,
    pregunta: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Guia de diseno: encabezado de alto 44 con la flecha a la izquierda y el titulo centrado,
    // luego "Paso n de 3" (label/field), la barra y la pregunta (title/screen)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s8)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
            Text(text = "Registro de compra", style = AppTypography.titleScreen)
        }
        Spacer(modifier = Modifier.height(Spacing.s16))
        Text(text = "Paso $paso de $TOTAL_PASOS_COMPRA", style = AppTypography.labelField)
        Spacer(modifier = Modifier.height(Spacing.s8))
        SegmentedProgressBar(total = TOTAL_PASOS_COMPRA, current = paso)
        Spacer(modifier = Modifier.height(Spacing.s20))
        Text(text = pregunta, style = AppTypography.titleScreen)
    }
}

// Pie de accion: barra blanca fija abajo con linea superior de 1 px (border/default)
// y relleno 16 arriba, 20 a los lados y 28 abajo, segun la guia de diseno
@Composable
fun CompraBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard)
    ) {
        HorizontalDivider(color = BorderDefault)
        Column(
            modifier = Modifier.padding(start = Spacing.s20, top = Spacing.s16, end = Spacing.s20, bottom = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
            content = content
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompraHeaderPreview() {
    CafesmartTheme {
        Column(modifier = Modifier.background(SurfaceScreen)) {
            CompraHeader(paso = 1, pregunta = "¿A quién le compras el café?", onBack = {})
            CompraBottomBar {
                ContinueButton(text = "Continuar", enabled = false, onClick = {})
            }
        }
    }
}
