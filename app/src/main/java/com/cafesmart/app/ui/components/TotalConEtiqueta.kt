package com.cafesmart.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard

/**
 * Baja el tamano de la letra cuando el monto tiene muchas cifras ("$ 99.999.000.000"), para que quepa
 * en su lugar sin partirse a mitad del numero. Hasta 11 caracteres ("$ 9.999.999") queda igual.
 */
fun TextStyle.ajustadoAlLargo(texto: String): TextStyle {
    val factor = when {
        texto.length <= 11 -> 1f
        texto.length <= 14 -> 0.8f
        texto.length <= 17 -> 0.65f
        else -> 0.5f
    }
    if (factor == 1f) return this
    return copy(
        fontSize = fontSize * factor,
        lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight
    )
}

/**
 * Etiqueta pequena y valor grande del pie de los pasos 2 ("Total kg" / "0 kg").
 * Se usa en Compras y en Ventas; el valor se achica si es muy largo.
 */
@Composable
fun TotalConEtiqueta(
    etiqueta: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier,
    alineadoALaDerecha: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alineadoALaDerecha) Alignment.End else Alignment.Start
    ) {
        Text(text = etiqueta, style = AppTypography.caption)
        Text(
            text = valor,
            style = AppTypography.titleModal.ajustadoAlLargo(valor).copy(color = color),
            textAlign = if (alineadoALaDerecha) TextAlign.End else TextAlign.Start
        )
    }
}

/**
 * Bloque de pago del Paso 2 (Compras y Ventas): totales y boton Continuar en una tarjeta.
 * Va DENTRO del scroll, al final del contenido, y no fijo abajo: asi no le quita pantalla a la seleccion.
 * [arriba] es para avisos propios de cada flujo (capacidad de bodega en Compras, minimo de kg en Ventas).
 */
@Composable
fun BloqueDeTotales(
    etiquetaIzquierda: String,
    valorIzquierdo: String,
    etiquetaDerecha: String,
    valorDerecho: String,
    color: Color,
    habilitado: Boolean,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
    mensajePendiente: String? = null,
    arriba: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        arriba()
        // weight(1f): cada total tiene la mitad del ancho y el monto se achica si es muy largo.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            TotalConEtiqueta(
                etiqueta = etiquetaIzquierda, valor = valorIzquierdo, color = color,
                modifier = Modifier.weight(1f)
            )
            TotalConEtiqueta(
                etiqueta = etiquetaDerecha, valor = valorDerecho, color = color,
                modifier = Modifier.weight(1f), alineadoALaDerecha = true
            )
        }
        // Texto pequeno que dice que falta para poder seguir (no se dibuja si es null).
        if (mensajePendiente != null) {
            Text(
                text = mensajePendiente,
                style = AppTypography.helper,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        ContinueButton(text = "Continuar", enabled = habilitado, onClick = onContinuar)
    }
}
