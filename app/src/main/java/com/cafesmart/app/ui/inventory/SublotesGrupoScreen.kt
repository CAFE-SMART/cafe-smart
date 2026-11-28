package com.cafesmart.app.ui.inventory

import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.DividerOnBlue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import com.cafesmart.app.ui.theme.SurfaceSoft
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.sample.CAPACIDAD_EJEMPLO_KG
import com.cafesmart.app.data.sample.lotesEjemplo
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.viewmodel.inventory.FilaSublote
import com.cafesmart.app.viewmodel.inventory.GrupoSublotes
import com.cafesmart.app.viewmodel.inventory.InventoryUiState
import com.cafesmart.app.viewmodel.inventory.OrdenInventario
import com.cafesmart.app.viewmodel.inventory.armarGrupo
import com.cafesmart.app.viewmodel.inventory.resumirInventario
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy

// "Inventario · Sublotes del grupo" del Figma (ruta `inventory_list/{loteId}`): los sublotes con café de un
// tipo y calidad. Sin estado propio. Se actualiza sola, por eso no lleva el botón de recargar.

@Composable
fun SublotesGrupoScreen(
    estado: InventoryUiState,
    loteId: Long,
    onBack: () -> Unit,
    onOrdenChange: (OrdenInventario) -> Unit,
    onSubloteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hoy = fechaDeHoy()
    val grupo = remember(estado.lotes, estado.orden, estado.resumen.pesoBultoKg, loteId, hoy) {
        armarGrupo(estado.lotes, loteId, estado.orden, estado.resumen.pesoBultoKg, hoy)
    }
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        EncabezadoPantalla(titulo = "Sublotes", onBack = onBack)
        when {
            estado.cargando -> Box(Modifier.padding(Spacing.s20)) { CircularProgressIndicator() }
            grupo == null -> Text(
                "No hay sublotes con café disponible en este grupo.",
                style = AppTypography.bodyLg,
                modifier = Modifier.padding(Spacing.s20).testTag("inventario_sublotes_vacio")
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.s20, end = Spacing.s20, top = Spacing.s8, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.s28)
            ) {
                item(key = "resumen") { ResumenGrupo(grupo) }
                item(key = "informacion") { InformacionGrupo(grupo) }
                item(key = "encabezado") { EncabezadoSublotes(grupo.cantidad, estado.orden, onOrdenChange) }
                items(grupo.filas, key = { it.id }) { fila ->
                    TarjetaSublote(fila, colorDeCalidad(grupo.calidad), onClick = { onSubloteClick(fila.id) })
                }
            }
        }
    }
}

@Composable
private fun ResumenGrupo(g: GrupoSublotes) {
    Surface(shape = AppShapes.large, color = SurfaceSelected, border = BorderStroke(1.dp, BorderSelectedSoft)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Box(Modifier.size(48.dp).background(SurfaceCard, AppShapes.medium), contentAlignment = Alignment.Center) {
                    Icon(iconoDelTipo(g.tipo), contentDescription = null, tint = colorDelTipo(g.tipo), modifier = Modifier.size(26.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                    Text("Café ${g.tipo.etiqueta.lowercase()}", style = AppTypography.titleModal.copy(fontSize = 20.sp))
                    EtiquetaCalidad(g.calidad, "Calidad ${g.calidad.etiqueta.lowercase()}")
                }
            }
            HorizontalDivider(color = DividerOnBlue)
            TresMetricas(
                DividerOnBlue,
                Triple("Peso total", g.pesoTotal, g.bultos),
                Triple("Sublotes", g.cantidad.toString(), if (g.cantidad == 1) "disponible" else "disponibles"),
                Triple("Valor", g.valorTotal, "en bodega"),
            )
        }
    }
}

@Composable
private fun TresMetricas(divisor: Color, vararg metricas: Triple<String, String, String>) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        metricas.forEachIndexed { i, (titulo, valor, nota) ->
            if (i > 0) Box(Modifier.width(1.dp).height(48.dp).background(divisor))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = AppTypography.caption.copy(color = TextSecondary))
                Text(valor, style = AppTypography.valueStrongBold.copy(fontSize = 15.sp))
                Text(nota, style = AppTypography.helper.copy(color = TextSecondary))
            }
        }
    }
}

@Composable
private fun InformacionGrupo(g: GrupoSublotes) {
    Tarjeta {
        EncabezadoSeccion(IconoDetInfo, BorderSelectedSoft, Primary, "Información del grupo", null)
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetEtiqueta, "Costo promedio") { ValorGrupo(g.costoPromedio) }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetReloj, "Más antiguo") { ValorGrupo(g.masAntiguo) }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetGota, "Humedad promedio") { ValorGrupo(g.humedadPromedio, sinDato = g.humedadPromedio == "Sin dato") }
    }
}

@Composable
private fun ValorGrupo(texto: String, sinDato: Boolean = false) {
    Text(
        texto,
        style = AppTypography.titleCard.copy(
            fontSize = 15.sp,
            fontWeight = if (sinDato) FontWeight.Medium else FontWeight.SemiBold,
            color = if (sinDato) TextMuted else TextPrimary,
        ),
    )
}

// "Sublotes (2)" y, a la derecha, el orden; al tocarlo se invierte.
@Composable
private fun EncabezadoSublotes(cantidad: Int, orden: OrdenInventario, onOrdenChange: (OrdenInventario) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Sublotes ($cantidad)", style = AppTypography.titleCard.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
        val otro = if (orden == OrdenInventario.MAS_RECIENTE) OrdenInventario.MAS_ANTIGUO else OrdenInventario.MAS_RECIENTE
        TextButton(onClick = { onOrdenChange(otro) }) {
            Text("${orden.etiqueta} primero", style = AppTypography.helper.copy(fontSize = 13.sp))
        }
    }
}

@Composable
private fun TarjetaSublote(f: FilaSublote, colorCalidad: Color, onClick: () -> Unit) {
    Surface(
        shape = AppShapes.large,
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth().testTag("inventario_sublote_${f.id}"),
    ) {
        Column(
            Modifier
                .clickable(onClick = onClick)
                .drawBehind { drawRect(colorCalidad, size = Size(5.dp.toPx(), size.height)) }
                .padding(start = 23.dp, end = 16.dp, top = 18.dp, bottom = 18.dp), // 5 dp de franja + 18
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Column(Modifier.weight(1f)) {
                    Text(f.nombre, style = AppTypography.titleCard)
                    Text("Entró el ${f.fechaEntrada}", style = AppTypography.helper)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = SurfaceSoft) {
                    Text(f.antiguedad, style = AppTypography.caption.copy(color = TextSecondary), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
                Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider(color = BorderDefault)
            TresMetricas(
                BorderDefault,
                Triple("Peso", f.peso, f.bultos),
                Triple("Costo", f.costoKg, "por kg"),
                Triple("Valor", f.valor, "en bodega"),
            )
            HorizontalDivider(color = BorderDefault)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DatoConIcono(IconoDetGota, "Humedad: ${f.humedad}")
                DatoConIcono(IconoDetEtiqueta, "Factor: ${f.factor}")
            }
        }
    }
}

@Composable
private fun DatoConIcono(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        Icon(icono, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
        Text(texto, style = AppTypography.helper.copy(fontSize = 13.sp, color = TextSecondary))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun SublotesGrupoScreenPreview() {
    CafesmartTheme {
        SublotesGrupoScreen(
            estado = InventoryUiState(
                resumen = resumirInventario(lotesEjemplo(), CAPACIDAD_EJEMPLO_KG),
                lotes = lotesEjemplo()
            ),
            loteId = 2, onBack = {}, onOrdenChange = {}, onSubloteClick = {}
        )
    }
}
