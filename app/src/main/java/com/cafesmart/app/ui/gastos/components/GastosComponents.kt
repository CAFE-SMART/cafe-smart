package com.cafesmart.app.ui.gastos.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafesmart.app.R
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.theme.*

/** Encabezado de las dos pantallas; [accion] reserva el espacio derecho sin descentrar el título. */
@Composable
fun GastosHeader(titulo: String, onBack: () -> Unit, accion: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = Spacing.s16)) {
        Box(Modifier.align(Alignment.CenterStart)) { BackArrow(onClick = onBack) }
        Text(titulo, style = AppTypography.titleScreen, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.CenterEnd)) { accion() }
    }
}

/** Iconos originales exportados de Figma. No se usan emojis ni URLs temporales en ejecución. */
@DrawableRes
fun iconoTipoGasto(tipo: TipoGasto): Int = when (tipo) {
    TipoGasto.TRANSPORTE -> R.drawable.gasto_transporte
    TipoGasto.COMIDA -> R.drawable.gasto_comida
    TipoGasto.SECADO -> R.drawable.gasto_secado
    TipoGasto.CARGUE -> R.drawable.gasto_cargue
    TipoGasto.DESCARGUE -> R.drawable.gasto_descargue
    TipoGasto.OTROS -> R.drawable.gasto_otros
}

/** Tarjeta vertical de categoría o aplicación; [alto] respeta sus dos variantes de Figma. */
@Composable
fun GastoOpcionCard(
    texto: String, @DrawableRes icono: Int, seleccionado: Boolean, onClick: () -> Unit,
    modifier: Modifier = Modifier, habilitado: Boolean = true, alto: Int = 82
) {
    Surface(
        selected = seleccionado, onClick = onClick, enabled = habilitado,
        color = if (seleccionado) SurfaceSelected else SurfaceCard,
        shape = AppShapes.medium,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) Primary else BorderDefault),
        modifier = modifier.heightIn(min = alto.dp).semantics { role = Role.RadioButton }
    ) {
        Column(Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s12),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s8, Alignment.CenterVertically)) {
            Icon(painterResource(icono), null, tint = if (seleccionado) Primary else TextMuted, modifier = Modifier.size(24.dp))
            Text(texto, style = AppTypography.bodyMd, color = if (seleccionado) Primary else TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

/** Control segmentado propio de Gastos, compartido entre filtros y estado de pago. */
@Composable
fun <T> GastosSegmentos(opciones: List<T>, elegido: T, etiqueta: (T) -> String, onElegir: (T) -> Unit, habilitado: Boolean = true) {
    Row(Modifier.fillMaxWidth().background(BorderDefault, AppShapes.medium).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        opciones.forEach { opcion ->
            Surface(selected = opcion == elegido, onClick = { onElegir(opcion) }, enabled = habilitado,
                color = if (opcion == elegido) SurfaceCard else BorderDefault, shape = AppShapes.medium,
                modifier = Modifier.weight(1f).heightIn(min = 40.dp).semantics { role = Role.RadioButton }) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(etiqueta(opcion), style = AppTypography.buttonLink, color = if (opcion == elegido) Primary else TextSecondary)
                }
            }
        }
    }
}
