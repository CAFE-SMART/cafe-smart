package com.cafesmart.app.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.finance.icono
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.DividerOnBlue
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.ui.theme.SuccessIcon
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.inventory.FilaSubloteGrupo
import com.cafesmart.app.viewmodel.inventory.GrupoSublotes
import com.cafesmart.app.viewmodel.inventory.InventoryUiState
import com.cafesmart.app.viewmodel.inventory.armarGrupo

// "Inventario · Detalle · Lista de sublotes" (Figma): resumen del grupo (tipo + calidad), información del grupo y
// los sublotes con café, cada uno abre su detalle. Datos de Room vía InventoryViewModel (misma lectura que Inventario).

/** Mensaje cuando el lote de la ruta no existe (id inválido o de otra cuenta). Texto propuesto, pendiente del equipo. */
const val GRUPO_NO_ENCONTRADO = "No encontramos este grupo de café."

/** Mensaje cuando el grupo existe pero ya no le queda café. Texto propuesto, pendiente del equipo. */
const val GRUPO_SIN_CAFE = "Este grupo ya no tiene sublotes con café."

@Composable
fun SublotesGrupoScreen(
    estado: InventoryUiState,
    loteId: Long?,
    hoy: Fecha,
    onBack: () -> Unit,
    onAbrirSublote: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onReintentar: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().background(SurfaceScreen).safeDrawingPadding()) {
        EncabezadoPantalla(titulo = "Sublotes", onBack = onBack)
        val grupo = loteId?.let { armarGrupo(estado.lotes, it, hoy, estado.resumen.pesoBultoKg) }
        when {
            estado.error != null -> Mensaje(estado.error, onReintentar)
            estado.cargando -> Box(Modifier.fillMaxWidth().padding(Spacing.s28), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag("grupo_cargando"))
            }
            grupo == null -> Mensaje(GRUPO_NO_ENCONTRADO, null)
            else -> Contenido(grupo, onAbrirSublote)
        }
    }
}

@Composable
private fun Mensaje(texto: String, onReintentar: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Text(texto, style = AppTypography.bodyLg, modifier = Modifier.testTag("grupo_mensaje"))
        if (onReintentar != null) TextButton(onClick = onReintentar) { Text("Reintentar") }
    }
}

@Composable
private fun Contenido(g: GrupoSublotes, onAbrirSublote: (Long) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("grupo_lista"),
        contentPadding = PaddingValues(start = Spacing.s20, end = Spacing.s20, top = Spacing.s8, bottom = Spacing.s28),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        item(key = "resumen") { Resumen(g) }
        item(key = "info") { Informacion(g) }
        item(key = "titulo") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sublotes (${g.cantidad})", style = AppTypography.titleCard, modifier = Modifier.weight(1f))
                Text("Más reciente primero", style = AppTypography.helper, color = TextMuted)
            }
        }
        if (g.sublotes.isEmpty()) item(key = "vacio") {
            Text(GRUPO_SIN_CAFE, style = AppTypography.bodyMd, color = TextSecondary, modifier = Modifier.testTag("grupo_sin_cafe"))
        }
        items(g.sublotes, key = { it.id }) { fila -> TarjetaSublote(fila, g.calidad) { onAbrirSublote(fila.id) } }
    }
}

// ---------------- Resumen del grupo ----------------

@Composable
private fun Resumen(g: GrupoSublotes) {
    Surface(shape = AppShapes.large, color = SurfaceSelected, border = BorderStroke(1.dp, BorderSelectedSoft)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Box(Modifier.size(48.dp).background(SurfaceCard, AppShapes.medium), contentAlignment = Alignment.Center) {
                    Icon(iconoTipo(g.tipo), contentDescription = null, tint = SuccessIcon, modifier = Modifier.size(24.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                    Text("Café ${g.tipo.etiqueta.lowercase()}", style = AppTypography.titleScreen, modifier = Modifier.testTag("grupo_titulo"))
                    PildoraCalidad(g.calidad)
                }
            }
            HorizontalDivider(color = DividerOnBlue)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Metrica("Peso total", g.pesoTotal, g.bultosTotal, "grupo_peso", Modifier.weight(1f))
                Divisor()
                Metrica("Sublotes", g.cantidad.toString(), "disponibles", "grupo_cantidad", Modifier.weight(1f))
                Divisor()
                Metrica("Valor", g.valorTotal, "en bodega", "grupo_valor", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Divisor() = Box(Modifier.width(1.dp).fillMaxHeight().background(DividerOnBlue))

// Mismo tratamiento que D4 en el detalle: el valor no se parte entre "$" y la cifra; si no cabe, baja la letra.
@Composable
private fun Metrica(titulo: String, valor: String, nota: String, tag: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = AppTypography.caption)
        BasicText(
            valor.replace(' ', ' '),
            style = AppTypography.valueStrongBold.copy(color = PrimaryDark),
            maxLines = 1, softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
            modifier = Modifier.testTag(tag),
        )
        Text(nota, style = AppTypography.helper)
    }
}

@Composable
private fun PildoraCalidad(calidad: Calidad) {
    val (fondo, punto, letra) = coloresCalidad(calidad)
    Surface(shape = CircleShape, color = fondo) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).background(punto, CircleShape))
            Text("Calidad ${calidad.etiqueta.lowercase()}", style = AppTypography.pill, color = letra)
        }
    }
}

// ---------------- Información del grupo ----------------

@Composable
private fun Informacion(g: GrupoSublotes) {
    Surface(shape = AppShapes.large, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.s16, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Box(Modifier.size(36.dp).background(BorderSelectedSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(IconoInfo, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Text("Información del grupo", style = AppTypography.titleCard)
            }
            HorizontalDivider(color = BorderDefault)
            Dato(IconoEtiqueta, "Costo promedio", g.costoPromedio, "grupo_costo_promedio")
            HorizontalDivider(color = BorderDefault)
            Dato(IconoReloj, "Más antiguo", g.masAntiguo, "grupo_mas_antiguo")
            HorizontalDivider(color = BorderDefault)
            Dato(IconoGota, "Humedad promedio", g.humedadPromedio, "grupo_humedad")
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, etiqueta: String, valor: String, tag: String) {
    Row(Modifier.fillMaxWidth().padding(Spacing.s16), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Box(Modifier.size(32.dp).background(SurfaceSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        Text(etiqueta, style = AppTypography.bodyMd, modifier = Modifier.weight(1f))
        Text(valor, style = if (valor == "Sin dato") AppTypography.bodyMd.copy(color = TextMuted) else AppTypography.valueStrongBold,
            modifier = Modifier.testTag(tag))
    }
}

// ---------------- Tarjeta de un sublote ----------------

@Composable
private fun TarjetaSublote(f: FilaSubloteGrupo, calidad: Calidad, onClick: () -> Unit) {
    val forma = AppShapes.large
    Surface(shape = forma, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth().clip(forma).clickable(role = Role.Button, onClick = onClick).testTag("grupo_sublote_${f.id}")) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja de color de la calidad (Figma).
            Box(Modifier.width(5.dp).fillMaxHeight().background(coloresCalidad(calidad).second))
            Column(Modifier.weight(1f).padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.titulo, style = AppTypography.titleCard, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceSoft) {
                        Text(f.antiguedad, style = AppTypography.helper, color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Icon(AppIcons.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                }
                HorizontalDivider(color = BorderDefault)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    MetricaFila("Peso", f.peso, f.bultos, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(BorderDefault))
                    MetricaFila("Costo", f.costoKg, "por kg", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(BorderDefault))
                    MetricaFila("Valor", f.valor, "en bodega", Modifier.weight(1f))
                }
                HorizontalDivider(color = BorderDefault)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s16)) {
                    Text(f.humedad, style = AppTypography.helper, color = TextSecondary)
                    Text(f.factor, style = AppTypography.helper, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MetricaFila(titulo: String, valor: String, nota: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = AppTypography.caption)
        BasicText(
            valor.replace(' ', ' '),
            style = AppTypography.valueStrongBold.copy(color = PrimaryDark),
            maxLines = 1, softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
        )
        Text(nota, style = AppTypography.helper)
    }
}

private fun coloresCalidad(calidad: Calidad): Triple<Color, Color, Color> = when (calidad) {
    Calidad.BUENA -> Triple(SuccessBg, SuccessDot, SuccessText)
    Calidad.REGULAR -> Triple(WarningBg, WarningDot, WarningText)
    Calidad.MALA -> Triple(ErrorBg, ErrorDot, ErrorText)
}

private fun iconoTipo(tipo: TipoCafe): ImageVector = when (tipo) {
    TipoCafe.VERDE -> AppIcons.Sprout
    TipoCafe.SECO -> AppIcons.Sun
    TipoCafe.TRILLADO -> AppIcons.Coffee
    TipoCafe.PASILLA -> AppIcons.Package
}

// Íconos de línea (Lucide), privados para no editar AppIcons.kt (lo comparte todo el equipo).
private val IconoInfo by lazy { icono("Info", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20Z", "M12 16v-4", "M12 8h.01") }
private val IconoReloj by lazy { icono("Reloj", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20Z", "M12 6v6l4 2") }
private val IconoGota by lazy {
    icono("Gota", "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z")
}
private val IconoEtiqueta by lazy {
    icono(
        "Etiqueta",
        "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z",
        "M7.5 7.5h.01",
    )
}
