package com.cafesmart.app.ui.screens.purchase

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
import androidx.compose.ui.text.style.TextAlign
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

// Total de pasos del registro de compra.
const val PURCHASE_STEPS = 3

// Parte de arriba de cada paso: ← "Registro de compra", "Paso n de 3", barra de progreso y la pregunta
@Composable
fun PurchaseHeader(
    paso: Int,
    pregunta: String,
    onBack: () -> Unit,
    // modifier permite que quien usa este componente ajuste su tamano o margen.
    modifier: Modifier = Modifier
) {
    // Guia de diseno: encabezado de alto 44 con la flecha a la izquierda y el titulo centrado,
    // luego "Paso n de 3" (label/field), la barra y la pregunta (title/screen)
    // Column pone los elementos uno debajo del otro.
    Column(
        modifier = modifier
            // Ocupa todo el ancho; luego el margen lateral y superior.
            .fillMaxWidth()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s8)
    ) {
        // Box superpone elementos: el titulo queda centrado y la flecha a la izquierda.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
            Text(text = "Registro de compra", style = AppTypography.titleScreen)
        }
        // Spacer deja un espacio vacio entre elementos.
        Spacer(modifier = Modifier.height(Spacing.s16))
        Text(text = "Paso $paso de $PURCHASE_STEPS", style = AppTypography.labelField)
        Spacer(modifier = Modifier.height(Spacing.s8))
        // Barra de progreso del equipo: pinta de azul los pasos completados.
        SegmentedProgressBar(total = PURCHASE_STEPS, current = paso)
        Spacer(modifier = Modifier.height(Spacing.s20))
        Text(text = pregunta, style = AppTypography.titleScreen)
    }
}

// Pie de accion: barra blanca fija abajo con linea superior de 1 px (border/default)
// y relleno 16 arriba, 20 a los lados y 28 abajo, segun la guia de diseno
@Composable
fun PurchaseBottomBar(
    modifier: Modifier = Modifier,
    // content es lo que va dentro de la barra: cada pantalla pone sus botones.
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard)
    ) {
        // Linea superior de 1 px que separa el pie del contenido.
        HorizontalDivider(color = BorderDefault)
        Column(
            modifier = Modifier.padding(start = Spacing.s20, top = Spacing.s16, end = Spacing.s20, bottom = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
            content = content
        )
    }
}

// Texto pequeno sobre "Continuar" que dice que falta para poder seguir
@Composable
// mensaje puede ser null: en ese caso no se dibuja nada.
fun PendingMessage(mensaje: String?) {
    if (mensaje != null) {
        Text(
            text = mensaje,
            style = AppTypography.helper,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
// Preview: permite ver el componente en Android Studio sin ejecutar la app.
private fun PurchaseHeaderPreview() {
    CafesmartTheme {
        Column(modifier = Modifier.background(SurfaceScreen)) {
            PurchaseHeader(paso = 1, pregunta = "¿A quién le compras el café?", onBack = {})
            PurchaseBottomBar {
                PendingMessage(mensaje = "Por favor, selecciona una opción para continuar el proceso.")
                ContinueButton(text = "Continuar", enabled = false, onClick = {})
            }
        }
    }
}
