package com.cafesmart.app.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.finanzas.FiltroMovimiento
import com.cafesmart.app.data.finanzas.MovimientoFinanciero
import com.cafesmart.app.data.finanzas.TipoMovimiento
import com.cafesmart.app.data.finanzas.mensajeHistorialVacio
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.viewmodel.finance.HistorialUiState
import com.cafesmart.app.viewmodel.purchase.formatoMiles

// Pantalla "Finanzas · Historial financiero" (Figma, sección NUEVAS de Flujos): filtro por tipo arriba, los
// movimientos agrupados por día y "Ver más movimientos" al final. Sin estado propio: recibe el estado y avisa con callbacks.

@Composable
fun HistorialFinancieroScreen(
    estado: HistorialUiState,
    onBack: () -> Unit,
    onFiltro: (FiltroMovimiento) -> Unit,
    onVerMas: () -> Unit,
    onReintentar: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SurfaceScreen)) {
        EncabezadoPantalla(titulo = "Historial financiero", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.s20, end = Spacing.s20, top = Spacing.s16, bottom = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)
        ) {
            FiltroDeMovimientos(estado.filtro, onFiltro)
            when {
                estado.cargando -> Box(Modifier.fillMaxWidth().padding(Spacing.s28), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                estado.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(estado.error, style = AppTypography.bodyMd.copy(color = ErrorText))
                    TextButton(onClick = onReintentar) {
                        Text("Reintentar", style = AppTypography.buttonLink.copy(color = Primary))
                    }
                }
                estado.filtrados.isEmpty() -> Text(
                    text = mensajeHistorialVacio(estado.filtro),
                    style = AppTypography.bodyMd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceSelected, AppShapes.medium)
                        .border(BorderStroke(1.dp, BorderSelectedSoft), AppShapes.medium)
                        .padding(Spacing.s16)
                )
                else -> {
                    estado.grupos.forEach { grupo ->
                        Text(grupo.etiqueta, style = AppTypography.labelSection.copy(color = TextMuted))
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                            grupo.movimientos.forEach { FilaMovimiento(it) }
                        }
                    }
                    if (estado.hayMas) {
                        TextButton(onClick = onVerMas) {
                            Text("Ver más movimientos", style = AppTypography.buttonLink.copy(color = Primary))
                        }
                    }
                }
            }
        }
    }
}

// Cuatro opciones del mismo ancho; la activa va con borde y fondo azules (como el Figma).
@Composable
private fun FiltroDeMovimientos(actual: FiltroMovimiento, onFiltro: (FiltroMovimiento) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        FiltroMovimiento.entries.forEach { filtro ->
            val activo = filtro == actual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (activo) SurfaceSelected else SurfaceCard, AppShapes.medium)
                    .border(BorderStroke(if (activo) 2.dp else 1.dp, if (activo) Primary else BorderDefault), AppShapes.medium)
                    .selectable(selected = activo, role = Role.Tab, onClick = { onFiltro(filtro) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filtro.etiqueta,
                    style = AppTypography.labelField.copy(color = if (activo) Primary else TextPrimary),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FilaMovimiento(movimiento: MovimientoFinanciero) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(Modifier.size(40.dp).background(ButtonSoft, AppShapes.medium), contentAlignment = Alignment.Center) {
            Icon(iconoDe(movimiento.tipo), contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(movimiento.titulo, style = AppTypography.labelField)
            if (movimiento.detalle.isNotEmpty()) {
                Text(movimiento.detalle, style = AppTypography.helper.copy(color = TextMuted))
            }
        }
        val suma = movimiento.montoPesos > 0
        Text(
            text = (if (suma) "+ " else "− ") + "$ " + formatoMiles(kotlin.math.abs(movimiento.montoPesos)),
            style = AppTypography.valueStrong.copy(color = if (suma) SuccessText else TextPrimary)
        )
    }
}

private fun iconoDe(tipo: TipoMovimiento): ImageVector = when (tipo) {
    TipoMovimiento.VENTA -> IconoDinero
    TipoMovimiento.COMPRA -> AppIcons.Package
    TipoMovimiento.GASTO -> IconoRecibo
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun HistorialConDatosPreview() {
    CafesmartTheme {
        val hoy = Fecha(2, 6, 2026)
        HistorialFinancieroScreen(
            HistorialUiState(
                hoy = hoy,
                cargando = false,
                movimientos = listOf(
                    MovimientoFinanciero(TipoMovimiento.VENTA, hoy, "Venta · Exportadora del Sur S.A.", "Café seco · 200 kg", 2_400_000),
                    MovimientoFinanciero(TipoMovimiento.COMPRA, hoy, "Compra · Carlos Mejía", "Café verde · 25 kg", -350_000),
                    MovimientoFinanciero(TipoMovimiento.GASTO, hoy, "Gasto · Transporte", "Gasto general", -45_000),
                    MovimientoFinanciero(TipoMovimiento.COMPRA, Fecha(1, 6, 2026), "Compra · Sin registrar", "Café verde · 40 kg", -560_000),
                )
            ),
            {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun HistorialVacioPreview() {
    CafesmartTheme {
        HistorialFinancieroScreen(HistorialUiState(hoy = Fecha(2, 6, 2026), cargando = false), {}, {}, {}, {})
    }
}
