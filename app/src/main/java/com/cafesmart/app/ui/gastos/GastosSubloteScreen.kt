package com.cafesmart.app.ui.gastos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.components.BackArrow
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.gastos.*

@Composable
fun GastosSubloteRoute(navController: NavHostController, entry: NavBackStackEntry) {
    val propietario = remember(entry) { navController.getBackStackEntry(Routes.HOME) }
    val modelo: GastosViewModel = viewModel(viewModelStoreOwner = propietario, factory = rememberGastosViewModelFactory())
    val batchId = entry.arguments?.getString(Routes.ARG_BATCH_ID)
    var intento by remember(entry) { mutableIntStateOf(0) }
    val fuente = remember(modelo, batchId, intento) { modelo.observarGastosSublote(batchId) }
    val estado by fuente.collectAsState(initial = GastosSubloteUiState(cargando = true))
    GastosSubloteScreen(estado, { navController.popBackStack() }, { intento++ },
        Modifier.consumeWindowInsets(WindowInsets.systemBars))
}

/** Solo presenta asignaciones; no sustituye el importe asignado por el monto global. */
@Composable
fun GastosSubloteScreen(
    estado: GastosSubloteUiState, onBack: () -> Unit, onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            BackArrow(onClick = onBack)
            Text("Gastos del sublote", style = AppTypography.titleScreen,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        }
        LazyColumn(Modifier.weight(1f).testTag("gastos_sublote_lista"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            estado.sublote?.let { sublote ->
                item { Text("Sublote #${sublote.id} · Lote #${sublote.loteId}", style = AppTypography.titleCard) }
            }
            when {
                estado.cargando -> item { CircularProgressIndicator(Modifier.testTag("gastos_sublote_cargando")) }
                estado.error != null -> item {
                    Text(estado.error, style = AppTypography.bodyLg)
                    if (estado.recuperable) TextButton(onClick = onReintentar) { Text("Reintentar") }
                }
                else -> {
                    item {
                        Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Total de gastos asignados", style = AppTypography.caption)
                                Text(dineroGasto(estado.total), style = AppTypography.displayValor, color = Primary,
                                    modifier = Modifier.testTag("gastos_sublote_total"))
                                Text("Valores correspondientes únicamente a este sublote.", style = AppTypography.bodyMd)
                            }
                        }
                    }
                    if (estado.gastos.isEmpty()) item {
                        Text("Este sublote todavía no tiene gastos registrados.", style = AppTypography.bodyLg)
                    }
                    items(estado.gastos, key = { it.gasto.id }) { fila ->
                        Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(fila.gasto.concepto, style = AppTypography.titleCard)
                                Text("${fila.gasto.tipo.etiqueta} · ${fechaGasto(fila.gasto.fecha)}", style = AppTypography.caption)
                                Text("Valor asignado: ${dineroGasto(fila.montoAsignado)}", style = AppTypography.valueStrongBold)
                                if (fila.gasto.descripcion.isNotBlank()) Text(fila.gasto.descripcion, style = AppTypography.bodyMd)
                            }
                        }
                    }
                }
            }
            item { Text("Demostración: inventario de ejemplo y gastos temporales; se pierden al cerrar la sesión o el proceso.", style = AppTypography.helper) }
        }
    }
}
