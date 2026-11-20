package com.cafesmart.app.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.inventory.DetalleSublote
import com.cafesmart.app.viewmodel.inventory.DetalleSubloteUiState
import com.cafesmart.app.viewmodel.inventory.DetalleSubloteViewModel

// Inventario · Detalle del sublote (solo lectura). Los datos salen de Room y se actualizan solos.

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
            Tarjeta {
                Text(d.codigo, style = AppTypography.titleCard, modifier = Modifier.testTag("detalle_sublote_codigo"))
                Text("${d.tipo} · Calidad ${d.calidad.lowercase()}", style = AppTypography.caption)
                Text("Valor en bodega", style = AppTypography.caption)
                Text(d.valorEnBodega, style = AppTypography.displayValor, color = Primary)
            }
            Tarjeta {
                Text("Información básica", style = AppTypography.titleCard)
                Dato("Tipo de café", d.tipo)
                Dato("Calidad", d.calidad)
                Dato("Peso inicial", d.pesoInicial, "detalle_sublote_peso_inicial")
                Dato("Peso actual", d.pesoActual, "detalle_sublote_peso_actual")
                Text(d.pesoActualBultos, style = AppTypography.helper)
                Dato("Precio por kg", d.precioPorKg)
                Dato("Fecha de ingreso", d.fecha)
                if (d.agotado) Text("Este sublote ya no tiene café disponible.", style = AppTypography.helper)
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

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contenido()
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, tag: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = AppTypography.bodyMd)
        Text(valor, style = AppTypography.valueStrongBold, modifier = if (tag != null) Modifier.testTag(tag) else Modifier)
    }
}
