package com.cafesmart.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.BorderField

// Barrita vertical a la derecha que le muestra a la persona que hay mas contenido abajo
// y en que parte de la lista va. Solo se dibuja cuando el contenido no cabe en la pantalla.
// Se pone ANTES de verticalScroll, con el mismo ScrollState, para que la barra quede fija
// y no se desplace junto con el contenido:
//   Modifier.barraDeDesplazamiento(scroll).verticalScroll(scroll)
fun Modifier.barraDeDesplazamiento(
    estado: ScrollState,
    color: Color = BorderField,
    grosor: Dp = 4.dp,
    margen: Dp = 2.dp,
    altoMinimo: Dp = 32.dp
): Modifier = drawWithContent {
    drawContent()
    // maxValue es cuanto se puede desplazar; 0 significa que todo cabe y no hace falta barra.
    if (estado.maxValue > 0) {
        val visible = size.height
        val total = visible + estado.maxValue
        // El alto de la barra es la parte visible respecto al total (nunca menos del minimo).
        val alto = maxOf(visible * visible / total, altoMinimo.toPx())
        // La posicion avanza de 0 a (visible - alto) segun lo desplazado.
        val y = estado.value.toFloat() / estado.maxValue * (visible - alto)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - grosor.toPx() - margen.toPx(), y),
            size = Size(grosor.toPx(), alto),
            cornerRadius = CornerRadius(grosor.toPx() / 2)
        )
    }
}
