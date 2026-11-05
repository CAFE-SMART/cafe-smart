package com.cafesmart.app.ui.gastos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.R
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.gastos.components.*
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.gastos.*

/**
 * Lista de gastos: presenta resumen, filtros y filas; no contiene formulario ni persistencia.
 * [estado] procede del ViewModel y [onRegistrar] navega a la pantalla de registro.
 */
@Composable
fun ListaGastosScreen(estado: GastosUiState, onBack: () -> Unit, onRegistrar: () -> Unit,
    onFiltro: (FiltroGasto) -> Unit, modifier: Modifier = Modifier, onActualizar: () -> Unit = {}) {
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        GastosHeader("Gastos generales", onBack) {
            IconButton(onClick = onActualizar) {
                Icon(painterResource(R.drawable.gasto_refresh), "Actualizar gastos", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = Spacing.s16, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s20)) {
            item {
                Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                    Column(Modifier.fillMaxWidth().padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        Text("Total acumulado", style = AppTypography.caption)
                        Text(dineroGasto(estado.totalPendiente), style = AppTypography.displayValor, color = Primary)
                        Text("Suma de gastos pendientes por pagar.", style = AppTypography.bodyMd)
                    }
                }
            }
            item { ContinueButton(text = "Registrar gasto", onClick = onRegistrar,
                leadingIcon = { Icon(AppIcons.Plus, null, modifier = Modifier.size(20.dp)) }) }
            item { GastosSegmentos(FiltroGasto.entries, estado.filtro, { it.etiqueta }, onFiltro) }
            if (estado.visibles.isEmpty()) item { GastoVacio(estado.gastos.isEmpty()) }
            items(estado.visibles, key = { it.id }) { gasto ->
                Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
                    Column(Modifier.fillMaxWidth().padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                            Icon(painterResource(iconoTipoGasto(gasto.tipo)), null, tint = Primary, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text(gasto.concepto, style = AppTypography.titleCard)
                                Text("${gasto.tipo.etiqueta} · ${fechaGasto(gasto.fecha)}", style = AppTypography.caption)
                            }
                        }
                        Text(dineroGasto(gasto.monto), style = AppTypography.valueStrongBold)
                        Text(gasto.estadoPago.etiqueta, style = AppTypography.bodyMd)
                        if (gasto.descripcion.isNotBlank()) Text(gasto.descripcion, style = AppTypography.bodyMd)
                    }
                }
            }
            item { Text("Demostración: los gastos son temporales y se pierden al cerrar la sesión o el proceso.", style = AppTypography.helper) }
        }
    }
}

/** Distingue una lista sin registros de un filtro que no contiene resultados. */
@Composable
private fun GastoVacio(sinRegistros: Boolean) {
    Surface(shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Box(Modifier.size(64.dp).background(SurfaceSelected, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.gasto_empty), null, tint = Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(Spacing.s4))
            Text(if (sinRegistros) "Aún no has registrado gastos" else "No hay gastos en este estado", style = AppTypography.titleScreen, textAlign = TextAlign.Center)
            Text(if (sinRegistros) "Registra el primero para empezar a gestionarlos." else "Selecciona otro filtro para ver tus gastos.",
                style = AppTypography.bodyLg, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ListaGastosPreview() { CafesmartTheme { ListaGastosScreen(GastosUiState(), {}, {}, {}) } }
