package com.cafesmart.app.ui.gastos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.gastos.components.GastosHeader
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.gastos.*
import com.cafesmart.app.viewmodel.purchase.formatoKg

/** Usa el borrador compartido: Atrás y Guardar selección conservan los mismos IDs y montos. */
@Composable
fun AsociarSublotesScreen(
    estado: GastosUiState,
    onAlternar: (Long) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habilitado = estado.resultado == ResultadoGasto.EDICION
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        GastosHeader("Seleccionar sublotes", onVolver)
        Text("${estado.formulario.asignaciones.size} sublote(s) seleccionado(s)",
            style = AppTypography.valueStrong, color = Primary,
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8))
        LazyColumn(Modifier.weight(1f).testTag("gastos_sublotes"), contentPadding = PaddingValues(Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            item {
                Text("Selecciona los sublotes y asigna sus montos al regresar al formulario.",
                    style = AppTypography.bodyMd, color = TextMuted)
                Text("Sublotes de ejemplo del inventario.", style = AppTypography.helper)
            }
            if (estado.sublotes.isEmpty()) item {
                Text("No hay sublotes disponibles", style = AppTypography.titleCard)
                Text("Puedes volver al formulario y registrar un gasto general.",
                    style = AppTypography.bodyMd, color = TextSecondary)
            }
            items(estado.sublotes, key = { it.id }) { sublote ->
                val seleccionado = sublote.id in estado.formulario.asignaciones
                Surface(shape = AppShapes.medium,
                    color = if (seleccionado) SurfaceSelected else SurfaceCard,
                    border = BorderStroke(1.dp, if (seleccionado) Primary else BorderDefault)) {
                    Row(Modifier.fillMaxWidth().toggleable(
                        value = seleccionado, enabled = habilitado, role = Role.Checkbox,
                        onValueChange = { onAlternar(sublote.id) }
                    ).padding(Spacing.s16), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                            Text("Sublote ${sublote.id}", style = AppTypography.titleCard)
                            Text("Lote ${sublote.loteId} · ${formatoKg(sublote.pesoDisponibleKg)}",
                                style = AppTypography.bodyMd, color = TextSecondary)
                            Text(fechaGasto(sublote.fechaIngreso), style = AppTypography.caption)
                        }
                        Checkbox(checked = seleccionado, onCheckedChange = null, enabled = habilitado)
                    }
                }
            }
        }
        Box(Modifier.padding(Spacing.s16)) {
            ContinueButton("Guardar selección", onVolver, enabled = habilitado)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AsociarSublotesPreview() {
    CafesmartTheme {
        AsociarSublotesScreen(
            GastosUiState(
                sublotes = com.cafesmart.app.data.sample.lotesEjemplo().flatMap { it.sublotesConExistencia },
                formulario = GastoFormulario(asignaciones = mapOf(1L to "5000", 2L to "7000"))
            ), {}, {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AsociarSublotesVacioPreview() {
    CafesmartTheme { AsociarSublotesScreen(GastosUiState(), {}, {}) }
}
