package com.cafesmart.app.ui.inventory

import com.cafesmart.app.ui.theme.TextStrong
import com.cafesmart.app.ui.theme.Primary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.data.model.Calidad
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import com.cafesmart.app.data.sample.lotesEjemplo
import com.cafesmart.app.ui.components.AppBottomBar
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.viewmodel.inventory.InventoryUiState
import com.cafesmart.app.viewmodel.inventory.OrdenInventario
import com.cafesmart.app.viewmodel.inventory.ResumenCalidad
import com.cafesmart.app.viewmodel.inventory.detallePeso
import com.cafesmart.app.viewmodel.inventory.formatoSublotes
import com.cafesmart.app.viewmodel.inventory.resumirInventario

// "Inventario · Lista por tipo" del Figma (ruta `inventory_type/{tipo}`): las calidades de un tipo de café.
// Sin estado propio. Las pestañas filtran por tipo y cada tarjeta abre los sublotes de esa calidad.

@Composable
fun CalidadesTipoScreen(
    estado: InventoryUiState,
    tipo: TipoCafe,
    onTodosClick: () -> Unit,
    onTipoClick: (TipoCafe) -> Unit,
    onCalidadClick: (Long) -> Unit,
    onIniciarSecado: () -> Unit,
    onOrdenChange: (OrdenInventario) -> Unit,
    onAjustarBodega: () -> Unit,
    onNavegar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = SurfaceScreen,
        bottomBar = { AppBottomBar(selectedRoute = "inventario", onNavigate = onNavegar) }
    ) { innerPadding ->
        // Igual que el Resumen: sin la primera lectura de Room no se dibuja nada (evita un vacío falso).
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(innerPadding))
            return@Scaffold
        }
        val calidades = estado.calidadesDe(tipo)
        val hayCafe = calidades.any { it.cantidadSublotes > 0 }
        val nombre = tipo.etiqueta.lowercase()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = Spacing.s20, end = Spacing.s20, top = Spacing.s48, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)
        ) {
            item(key = "titulo") { Text("Inventario", style = TituloPantalla) }
            item(key = "resumen") { ResumenBodega(estado.resumen, onAjustarBodega) }
            item(key = "filtro") {
                FiltroPorTipo(seleccionado = tipo, onTipoClick = onTipoClick, onTodosClick = onTodosClick)
            }
            if (!hayCafe) {
                item(key = "vacio") {
                    Text(
                        "No tienes café $nombre en inventario.",
                        style = AppTypography.bodyMd.copy(color = TextMuted),
                        modifier = Modifier.testTag("inventario_tipo_vacio")
                    )
                }
            } else {
                // Solo el café verde se manda a secar.
                if (tipo == TipoCafe.VERDE) {
                    item(key = "secado") {
                        Button(
                            onClick = onIniciarSecado,
                            shape = AppShapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("inventario_iniciar_secado")
                        ) {
                            Icon(AppIcons.Sun, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.s8))
                            Text("Iniciar secado", style = AppTypography.buttonPrimary, color = OnPrimary)
                        }
                    }
                }
                item(key = "encabezado") {
                    EncabezadoLista(estado.orden, onOrdenChange, titulo = "Café $nombre por calidad")
                }
                items(calidades, key = { it.lote.id }) { fila ->
                    TarjetaCalidad(fila, estado.resumen.pesoBultoKg, onClick = { onCalidadClick(fila.lote.id) })
                }
            }
        }
    }
}

@Composable
private fun TarjetaCalidad(fila: ResumenCalidad, pesoBultoKg: Double, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderDefault, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("inventario_calidad_${fila.calidad.name}")
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                Box(Modifier.size(8.dp).background(colorDeCalidad(fila.calidad), CircleShape))
                Text("Calidad ${fila.calidad.etiqueta.lowercase()}", style = AppTypography.pill.copy(color = TextStrong))
            }
            Text(detallePeso(fila.pesoKg, pesoBultoKg), style = AppTypography.titleCard)
        }
        EtiquetaSublotes(formatoSublotes(fila.cantidadSublotes))
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CalidadesTipoScreenPreview() {
    CafesmartTheme {
        CalidadesTipoScreen(
            estado = InventoryUiState(
                resumen = resumirInventario(lotesEjemplo(), CAPACIDAD_EJEMPLO_KG),
                lotes = lotesEjemplo()
            ),
            tipo = TipoCafe.VERDE,
            onTodosClick = {}, onTipoClick = {}, onCalidadClick = {}, onIniciarSecado = {},
            onOrdenChange = {}, onAjustarBodega = {}, onNavegar = {}
        )
    }
}

internal fun colorDeCalidad(calidad: Calidad): Color = when (calidad) {
    Calidad.BUENA -> SuccessDot
    Calidad.REGULAR -> WarningDot
    Calidad.MALA -> ErrorDot
}
