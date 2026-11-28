package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.SegmentedProgressBar
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.ui.theme.SuccessSolid
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.WarningText

// Piezas que comparten las pantallas de Secado. Son solo apariencia: no tienen logica.

// Total de pasos del registro de secado en el Figma: "Paso 1 de 2" y "Paso 2 de 2" (el paso 3 es la pantalla final).
const val PASOS_DE_INICIO_DE_SECADO = 2

// Parte de arriba de los pasos 1 y 2: ← "Iniciar secado", "Paso n de 2", barra de progreso y la pregunta.
// Es como PurchaseHeader, que tiene fijos los 3 pasos de Compras y Ventas.
@Composable
fun EncabezadoSecado(
    paso: Int,
    pregunta: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s20)
            .padding(top = Spacing.s8)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BackArrow(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
            Text(text = "Iniciar secado", style = AppTypography.titleScreen)
        }
        Spacer(modifier = Modifier.height(Spacing.s16))
        Text(text = "Paso $paso de $PASOS_DE_INICIO_DE_SECADO", style = AppTypography.labelField)
        Spacer(modifier = Modifier.height(Spacing.s8))
        SegmentedProgressBar(total = PASOS_DE_INICIO_DE_SECADO, current = paso)
        Spacer(modifier = Modifier.height(Spacing.s20))
        Text(text = pregunta, style = AppTypography.titleScreen)
    }
}

// Pastilla de una calidad con su punto de color ("Calidad buena" o solo "Buena")
@Composable
fun EtiquetaCalidad(calidad: Calidad, texto: String) {
    val (fondo, punto, color) = when (calidad) {
        Calidad.BUENA -> Triple(SuccessBg, SuccessDot, SuccessText)
        Calidad.REGULAR -> Triple(WarningBg, WarningDot, WarningText)
        Calidad.MALA -> Triple(ErrorBg, ErrorDot, ErrorText)
    }
    Row(
        modifier = Modifier
            .background(fondo, CircleShape)
            .padding(horizontal = Spacing.s12, vertical = Spacing.s6),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(punto, CircleShape))
        Spacer(modifier = Modifier.width(Spacing.s6))
        Text(text = texto, style = AppTypography.pill.copy(color = color))
    }
}

// Casilla cuadrada: vacia, con ✓ (marcada) o con "-" (algunos elegidos).
@Composable
fun Casilla(marcada: Boolean, aMedias: Boolean = false) {
    if (marcada || aMedias) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Primary, AppShapes.small),
            contentAlignment = Alignment.Center
        ) {
            if (marcada) {
                Icon(imageVector = AppIcons.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
            } else {
                // La raya del "-"
                Box(modifier = Modifier.size(width = 12.dp, height = 2.dp).background(OnPrimary, CircleShape))
            }
        }
    } else {
        Box(modifier = Modifier.size(24.dp).border(1.5.dp, BorderField, AppShapes.small))
    }
}

// Tarjeta "Resumen del secado" de los pasos 2 y 3: tipo, calidades, sublotes, (fecha de inicio en el paso 3)
// y, en la fila azul de abajo, los kg. etiquetaKg es "Cantidad a secar" (paso 2) o "En secado" (paso 3).
@Composable
fun ResumenDeSecado(
    calidades: List<Calidad>,
    sublotes: String,
    etiquetaKg: String,
    kg: String,
    modifier: Modifier = Modifier,
    // Solo el paso 3 la muestra
    fechaInicio: String? = null
) {
    // Surface recorta el fondo azul de la ultima fila con las esquinas redondeadas de la tarjeta
    Surface(
        shape = AppShapes.medium,
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            FilaDeResumen(etiqueta = "Tipo de café") {
                Text(text = "Verde", style = AppTypography.valueStrongBold)
            }
            HorizontalDivider(color = BorderDefault)
            FilaDeResumen(etiqueta = "Calidad") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                    calidades.forEach { EtiquetaCalidad(calidad = it, texto = it.etiqueta) }
                }
            }
            HorizontalDivider(color = BorderDefault)
            FilaDeResumen(etiqueta = "Sublotes") {
                Text(text = sublotes, style = AppTypography.valueStrongBold)
            }
            if (fechaInicio != null) {
                HorizontalDivider(color = BorderDefault)
                FilaDeResumen(etiqueta = "Fecha de inicio") {
                    Text(text = fechaInicio, style = AppTypography.valueStrongBold)
                }
            }
            FilaFinalAzul(etiqueta = etiquetaKg, valor = kg)
        }
    }
}

// Ultima fila azul de un resumen: etiqueta a la izquierda y el valor grande a la derecha.
@Composable
fun FilaFinalAzul(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSelected)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = AppTypography.labelField.copy(color = PrimaryDark),
            modifier = Modifier.weight(1f)
        )
        Text(text = valor, style = AppTypography.titleScreen.copy(color = PrimaryDark))
    }
}

// Tarjeta con el icono de calendario, una fecha y "Cambiar" (inicio en el paso 2, finalizacion en el paso 5)
@Composable
fun TarjetaFecha(etiqueta: String, fecha: String, onCambiar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(start = Spacing.s16, top = Spacing.s8, end = Spacing.s4, bottom = Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = AppIcons.Calendar, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = etiqueta, style = AppTypography.caption)
            Text(text = fecha, style = AppTypography.valueStrong)
        }
        TextButton(onClick = onCambiar) {
            Text(text = "Cambiar", style = AppTypography.buttonLink)
        }
    }
}

// Circulo verde con chulito de las pantallas finales (pasos 3 y 6)
@Composable
fun CirculoCheck() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(SuccessSolid, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = AppIcons.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(44.dp))
    }
}

// Boton secundario con fondo azul suave ("Ir a inventario")
@Composable
fun BotonSuave(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = AppShapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = ButtonSoft, contentColor = PrimaryDark),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text = texto, style = AppTypography.buttonPrimary.copy(color = PrimaryDark))
    }
}

// Una fila "etiqueta ........ valor" del resumen
@Composable
fun FilaDeResumen(etiqueta: String, valor: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s16, vertical = Spacing.s12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = etiqueta, style = AppTypography.bodyMd, modifier = Modifier.weight(1f))
        valor()
    }
}
