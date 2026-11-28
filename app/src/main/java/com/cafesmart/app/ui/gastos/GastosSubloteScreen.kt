package com.cafesmart.app.ui.gastos

import com.cafesmart.app.ui.gastos.components.GastosSegmentos
import com.cafesmart.app.ui.gastos.components.GastosHeader
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.data.model.EstadoPago
import com.cafesmart.app.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
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
import com.cafesmart.app.data.inventario.InventoryRepository
import androidx.compose.ui.platform.LocalContext
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
    // Tipo y calidad del lote para el encabezado del Figma, de la misma base Room que Inventario.
    val contexto = LocalContext.current
    val lotes by remember { InventoryRepository.de(contexto).observarLotes() }.collectAsState(initial = emptyList())
    GastosSubloteScreen(estado, { navController.popBackStack() }, { intento++ },
        Modifier.consumeWindowInsets(WindowInsets.systemBars), mostrarAvisoDemostracion = modelo.permiteEjemplos,
        encabezado = estado.sublote?.let { encabezadoGastosSublote(it, lotes) },
        // Registro ya asociado a este sublote (mismo ViewModel que Registrar gasto, ligado a Inicio).
        // Solo si el sublote tiene café: a un sublote agotado no se le asignan gastos nuevos (no se ofrece el botón).
        onRegistrarGasto = estado.sublote?.takeUnless { it.estaAgotado }?.let { sublote ->
            {
                if (modelo.nuevoRegistroParaSublote(sublote)) navController.navigate(Routes.EXPENSE_NEW) { launchSingleTop = true }
            }
        },
        onActualizar = { intento++ })
}

/**
 * "Inventario · Detalle · Gastos del sublote" (Figma). Solo presenta lo asignado a este sublote; no sustituye el
 * importe asignado por el monto global del gasto.
 * - Chips "Pagado" y "Pendiente": suma de lo asignado según el estado de pago guardado en Room.
 * - Pestañas Pendientes / Pagados / Todos: solo filtran la lista (el total de arriba es siempre el del sublote).
 * - "Registrar gasto": abre el registro ya asociado a este sublote. "Actualizar": vuelve a leer Room.
 * - La nota del Figma "Estos gastos se suman al costo del sublote" NO se muestra: la regla aprobada es que los gastos
 *   se restan en la utilidad (CLAUDE.md), y sumarlos además al costo por kg los descontaría dos veces. Pendiente
 *   de confirmar con el equipo.
 */
@Composable
fun GastosSubloteScreen(
    estado: GastosSubloteUiState, onBack: () -> Unit, onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    // Solo con el repositorio de demostración (en memoria). Con Room los gastos sí se guardan: el aviso sería falso.
    mostrarAvisoDemostracion: Boolean = false,
    onRegistrarGasto: (() -> Unit)? = null,
    onActualizar: () -> Unit = onReintentar,
    // "Sublote #9 · Verde · Regular" (ver encabezadoGastosSublote). null: solo el número del sublote.
    encabezado: String? = null,
) {
    val titulo = encabezado ?: estado.sublote?.let { "Sublote #${it.id}" }
    var filtro by rememberSaveable { mutableStateOf(FiltroGasto.TODOS) }
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        GastosHeader("Gastos del sublote", onBack) {
            IconButton(onClick = onActualizar, modifier = Modifier.testTag("gastos_sublote_actualizar")) {
                Icon(painterResource(R.drawable.gasto_refresh), "Actualizar gastos", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        LazyColumn(Modifier.weight(1f).testTag("gastos_sublote_lista"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when {
                estado.cargando -> item { CircularProgressIndicator(Modifier.testTag("gastos_sublote_cargando")) }
                estado.error != null -> item {
                    titulo?.let { Text(it, style = AppTypography.titleCard) }
                    Text(estado.error, style = AppTypography.bodyLg)
                    if (estado.recuperable) TextButton(onClick = onReintentar) { Text("Reintentar") }
                }
                else -> {
                    item {
                        Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                titulo?.let {
                                    Text(it, style = AppTypography.caption,
                                        modifier = Modifier.testTag("gastos_sublote_titulo"))
                                }
                                Text(dineroGasto(estado.total), style = AppTypography.displayValor, color = Primary,
                                    modifier = Modifier.testTag("gastos_sublote_total"))
                                Text("Gastos asociados a este sublote.", style = AppTypography.bodyMd)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ChipEstado("Pagado", estado.totalPagado, SuccessDot, "gastos_sublote_total_pagado")
                                    ChipEstado("Pendiente", estado.totalPendiente, WarningDot, "gastos_sublote_total_pendiente")
                                }
                            }
                        }
                    }
                    if (onRegistrarGasto != null) item {
                        ContinueButton(text = "Registrar gasto", onClick = onRegistrarGasto,
                            modifier = Modifier.testTag("gastos_sublote_registrar"),
                            leadingIcon = { Icon(AppIcons.Plus, null, modifier = Modifier.size(20.dp)) })
                    }
                    item { GastosSegmentos(FiltroGasto.entries, filtro, { it.etiqueta }, { filtro = it }) }
                    val visibles = estado.visibles(filtro)
                    if (visibles.isEmpty()) item {
                        Text(mensajeSinGastos(filtro), style = AppTypography.bodyLg, modifier = Modifier.testTag("gastos_sublote_vacio"))
                    }
                    items(visibles, key = { it.gasto.id }) { fila -> FilaGastoSublote(fila) }
                }
            }
            if (mostrarAvisoDemostracion) item {
                Text("Demostración: inventario de ejemplo y gastos temporales; se pierden al cerrar la sesión o el proceso.",
                    style = AppTypography.helper, modifier = Modifier.testTag("gastos_sublote_aviso_demo"))
            }
        }
    }
}

@Composable
private fun ChipEstado(texto: String, monto: java.math.BigInteger, punto: androidx.compose.ui.graphics.Color, tag: String) {
    Surface(shape = AppShapes.medium, color = SurfaceSoft) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).background(punto, CircleShape))
            Text("$texto ${dineroGasto(monto)}", style = AppTypography.caption, modifier = Modifier.testTag(tag))
        }
    }
}

@Composable
private fun FilaGastoSublote(fila: com.cafesmart.app.data.gastos.GastoDelSublote) {
    val pagado = fila.gasto.estadoPago == EstadoPago.PAGADO
    Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.testTag("gastos_sublote_fila_${fila.gasto.id}")) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(fila.gasto.concepto, style = AppTypography.titleCard)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = AppShapes.small, color = if (pagado) SuccessBg else WarningBg) {
                        Text(fila.gasto.estadoPago.etiqueta, style = AppTypography.caption,
                            color = if (pagado) SuccessText else WarningText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Text("${fila.gasto.tipo.etiqueta} · ${fechaGasto(fila.gasto.fecha)}", style = AppTypography.caption)
                }
                if (fila.gasto.descripcion.isNotBlank()) Text(fila.gasto.descripcion, style = AppTypography.bodyMd)
            }
            Text(dineroGasto(fila.montoAsignado), style = AppTypography.valueStrongBold)
        }
    }
}
