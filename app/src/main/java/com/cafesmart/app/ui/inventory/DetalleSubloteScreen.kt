package com.cafesmart.app.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.DividerOnBlue
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.inventory.DetalleSublote
import com.cafesmart.app.viewmodel.inventory.DetalleSubloteUiState
import com.cafesmart.app.viewmodel.inventory.DetalleSubloteViewModel

// Inventario · Detalle del sublote (solo lectura), siguiendo el Figma "Inventario · Detalle del sublote".
// Los datos salen de Room y se actualizan solos (por eso no hay botón de actualizar). Quedan fuera, por ser de
// otras tarjetas: Análisis financiero, Movimientos del sublote, Ajustar peso y el botón "Agregar" de Datos de calidad.

/** Conecta la pantalla con su ViewModel; [batchId] es el argumento de la ruta INVENTORY_DETAIL. */
@Composable
fun DetalleSubloteRoute(
    inventario: InventoryRepository,
    batchId: String,
    onBack: () -> Unit,
    onVerGastos: () -> Unit,
    onVender: () -> Unit,
) {
    val modelo: DetalleSubloteViewModel = viewModel(key = "detalle_sublote_$batchId") {
        DetalleSubloteViewModel(inventario, batchId)
    }
    val estado by modelo.uiState.collectAsState()
    DetalleSubloteScreen(estado, onBack, onVerGastos, onVender)
}

@Composable
fun DetalleSubloteScreen(
    estado: DetalleSubloteUiState,
    onBack: () -> Unit,
    onVerGastos: () -> Unit,
    onVender: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackArrow(onClick = onBack)
            Text("Detalles", style = AppTypography.titleScreen, modifier = Modifier.padding(horizontal = 8.dp))
        }
        when {
            estado.cargando -> Column(Modifier.weight(1f).padding(16.dp)) {
                CircularProgressIndicator(Modifier.testTag("detalle_sublote_cargando"))
            }
            estado.detalle == null -> Column(Modifier.weight(1f).padding(16.dp)) {
                Text(
                    "No encontramos este sublote. Vuelve al inventario y elige otro.",
                    style = AppTypography.bodyLg,
                    modifier = Modifier.testTag("detalle_sublote_no_encontrado"),
                )
            }
            else -> Contenido(estado.detalle, onVerGastos, onVender, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Contenido(
    d: DetalleSublote,
    onVerGastos: () -> Unit,
    onVender: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Resumen(d)
            Tarjeta {
                Text("Información básica", style = AppTypography.titleCard)
                Dato("Tipo de café", d.tipo)
                Fila("Calidad") { EtiquetaCalidad(d.calidadTipo, d.calidad) }
                Dato("Peso inicial", d.pesoInicial, "detalle_sublote_peso_inicial")
                Dato("Peso actual", d.pesoActual, "detalle_sublote_peso_actual")
                Dato("Precio por kg", d.precioPorKg)
                Dato(d.etiquetaFecha, d.fecha)
                Dato("Tiempo en bodega", d.tiempoEnBodega, "detalle_sublote_tiempo")
                if (d.agotado) Text("Este sublote ya no tiene café disponible.", style = AppTypography.helper)
            }
            Tarjeta {
                Text("Datos de calidad", style = AppTypography.titleCard)
                Text("Opcionales. Se agregan cuando se mide la humedad o el factor del café.", style = AppTypography.helper)
                DatoCalidad("Humedad", d.rangoHumedad, d.humedad, "detalle_sublote_humedad")
                DatoCalidad("Factor de rendimiento", d.rangoFactor, d.factorRendimiento, "detalle_sublote_factor")
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ContinueButton(text = "Vender sublote", onClick = onVender, enabled = !d.agotado, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = onVerGastos,
                shape = AppShapes.medium,
                border = BorderStroke(1.dp, BorderField),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard, contentColor = PrimaryDark),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("detalle_sublote_ver_gastos"),
            ) { Text("Ver gastos", style = AppTypography.buttonLink.copy(color = PrimaryDark)) }
        }
    }
}

// Tarjeta azul del encabezado: nombre, etiqueta de calidad y tipo, y las tres métricas del Figma.
@Composable
private fun Resumen(d: DetalleSublote) {
    Surface(shape = AppShapes.large, color = SurfaceSelected, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(d.codigo, style = AppTypography.displayValor, modifier = Modifier.testTag("detalle_sublote_codigo"))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaCalidad(d.calidadTipo, "Calidad ${d.calidad.lowercase()}")
                Text("Café ${d.tipo.lowercase()}", style = AppTypography.bodyMd)
            }
            HorizontalDivider(color = DividerOnBlue)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica("Peso", d.pesoActual, d.pesoActualBultos, Modifier.weight(1f))
                Metrica("Costo por kg", d.precioPorKg, "promedio", Modifier.weight(1f))
                Metrica("Valor total", d.valorEnBodega, "en bodega", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metrica(titulo: String, valor: String, nota: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = AppTypography.caption)
        Text(valor, style = AppTypography.valueStrongBold, color = PrimaryDark)
        Text(nota, style = AppTypography.helper)
    }
}

// Etiqueta con punto de color según la calidad: buena (verde), regular (ámbar) y mala (rojo).
@Composable
private fun EtiquetaCalidad(calidad: Calidad, texto: String) {
    val (fondo, punto, letra) = when (calidad) {
        Calidad.BUENA -> Triple(SuccessBg, SuccessDot, SuccessText)
        Calidad.REGULAR -> Triple(WarningBg, WarningDot, WarningText)
        Calidad.MALA -> Triple(ErrorBg, ErrorDot, ErrorText)
    }
    Surface(shape = CircleShape, color = fondo) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(punto, CircleShape))
            Text(texto, style = AppTypography.pill, color = letra)
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contenido()
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = AppTypography.bodyMd)
        valor()
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, tag: String? = null) {
    Fila(etiqueta) {
        Text(valor, style = AppTypography.valueStrongBold, modifier = if (tag != null) Modifier.testTag(tag) else Modifier)
    }
}

// Humedad / factor de rendimiento: el valor (o "Sin dato") a la derecha y el rango válido como ayuda debajo.
@Composable
private fun DatoCalidad(etiqueta: String, rango: String, valor: String, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Fila(etiqueta) { Text(valor, style = AppTypography.valueStrongBold, modifier = Modifier.testTag(tag)) }
        Text(rango, style = AppTypography.helper)
    }
}
