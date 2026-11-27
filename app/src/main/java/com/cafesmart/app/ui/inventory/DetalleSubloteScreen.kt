package com.cafesmart.app.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafesmart.app.data.inventario.InventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.finance.IconoDinero
import com.cafesmart.app.ui.finance.icono
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.BorderField
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.DividerOnBlue
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.OnPrimary
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.PrimaryDark
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessDot
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSelected
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningDot
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.inventory.DetalleSublote
import com.cafesmart.app.viewmodel.inventory.DetalleSubloteUiState
import com.cafesmart.app.viewmodel.inventory.DetalleSubloteViewModel
import com.cafesmart.app.viewmodel.inventory.subtituloDatoCalidad

// Inventario · Detalle del sublote (solo lectura), siguiendo el Figma "Inventario · Detalle del sublote".
// Los datos salen de Room y se actualizan solos (por eso no hay botón de actualizar). Quedan fuera, por ser de
// otras tarjetas o no tener tarjeta asignada: Análisis financiero, Movimientos del sublote, Ajustar peso y el
// botón "Agregar" de Datos de calidad (humedad y peso: tarjeta #87). Así no hay botones que no hagan nada.

/** Conecta la pantalla con su ViewModel; [batchId] es el argumento de la ruta INVENTORY_DETAIL. */
@Composable
fun DetalleSubloteRoute(
    inventario: InventoryRepository,
    batchId: String,
    onBack: () -> Unit,
    onVerGastos: () -> Unit,
    onVender: () -> Unit,
    negocios: NegocioRepository? = null,
    uid: String? = null,
) {
    val modelo: DetalleSubloteViewModel = viewModel(key = "detalle_sublote_$batchId") {
        DetalleSubloteViewModel(inventario, batchId, negocios, uid)
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
        EncabezadoPantalla(titulo = "Detalles", onBack = onBack)
        when {
            estado.cargando -> Column(Modifier.weight(1f).padding(Spacing.s20)) {
                CircularProgressIndicator(Modifier.testTag("detalle_sublote_cargando"))
            }
            estado.detalle == null -> Column(Modifier.weight(1f).padding(Spacing.s20)) {
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
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s28),
        ) {
            Resumen(d)
            InformacionBasica(d)
            DatosDeCalidad(d)
        }
        // Pie de acciones del Figma: "Vender sublote" y debajo "Ver gastos". "Ajustar peso" no se dibuja (otra tarjeta).
        Surface(color = SurfaceCard) {
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(color = BorderDefault)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ContinueButton(
                        text = "Vender sublote",
                        onClick = onVender,
                        enabled = !d.agotado,
                        modifier = Modifier.fillMaxWidth().testTag("detalle_sublote_vender"),
                        leadingIcon = {
                            Icon(IconoDinero, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                        },
                    )
                    OutlinedButton(
                        onClick = onVerGastos,
                        shape = AppShapes.medium,
                        border = BorderStroke(1.5.dp, BorderField),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceCard, contentColor = PrimaryDark),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("detalle_sublote_ver_gastos"),
                    ) { Text("Ver gastos", style = AppTypography.buttonLink.copy(color = PrimaryDark)) }
                }
            }
        }
    }
}

// Tarjeta azul del encabezado: nombre, etiqueta de calidad y tipo, y las tres métricas del Figma.
@Composable
private fun Resumen(d: DetalleSublote) {
    Surface(shape = AppShapes.large, color = SurfaceSelected, border = BorderStroke(1.dp, BorderSelectedSoft)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                Text(d.codigo, style = AppTypography.displayValor, modifier = Modifier.testTag("detalle_sublote_codigo"))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    EtiquetaCalidad(d.calidadTipo, "Calidad ${d.calidad.lowercase()}")
                    Text("Café ${d.tipo.lowercase()}", style = AppTypography.bodyMd)
                }
            }
            HorizontalDivider(color = DividerOnBlue)
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Metrica("Peso", d.pesoActual, d.pesoActualBultos, Modifier.weight(1f))
                DivisorVertical()
                Metrica("Precio por kg", d.precioPorKg, null, Modifier.weight(1f))
                DivisorVertical()
                Metrica("Valor total", d.valorEnBodega, "en bodega", Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun DivisorVertical() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(DividerOnBlue))
}

@Composable
internal fun Metrica(titulo: String, valor: String, nota: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = AppTypography.caption)
        Text(valor, style = AppTypography.valueStrongBold, color = PrimaryDark)
        if (nota != null) Text(nota, style = AppTypography.helper)
    }
}

// Etiqueta con punto de color según la calidad: buena (verde), regular (ámbar) y mala (rojo).
@Composable
internal fun EtiquetaCalidad(calidad: Calidad, texto: String) {
    val (fondo, punto, letra) = when (calidad) {
        Calidad.BUENA -> Triple(SuccessBg, SuccessDot, SuccessText)
        Calidad.REGULAR -> Triple(WarningBg, WarningDot, WarningText)
        Calidad.MALA -> Triple(ErrorBg, ErrorDot, ErrorText)
    }
    Surface(shape = CircleShape, color = fondo) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(punto, CircleShape))
            Text(texto, style = AppTypography.pill, color = letra)
        }
    }
}

@Composable
private fun InformacionBasica(d: DetalleSublote) {
    Tarjeta {
        EncabezadoSeccion(IconoDetInfo, BorderSelectedSoft, Primary, "Información básica", null)
        HorizontalDivider(color = BorderDefault)
        FilaDato(AppIcons.Sprout, "Tipo de café") { Valor(d.tipo) }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetEstrella, "Calidad") { EtiquetaCalidad(d.calidadTipo, d.calidad) }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetPeso, "Peso inicial") { Valor(d.pesoInicial, "detalle_sublote_peso_inicial") }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetPeso, "Peso actual") { Valor(d.pesoActual, "detalle_sublote_peso_actual") }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetEtiqueta, "Precio por kg") { Valor(d.precioPorKg) }
        HorizontalDivider(color = BorderDefault)
        FilaDato(AppIcons.Calendar, d.etiquetaFecha) { Valor(d.fecha) }
        HorizontalDivider(color = BorderDefault)
        FilaDato(IconoDetReloj, "Tiempo en bodega") { Valor(d.tiempoEnBodega, "detalle_sublote_tiempo") }
        if (d.agotado) {
            HorizontalDivider(color = BorderDefault)
            Text(
                "Este sublote ya no tiene café disponible.",
                style = AppTypography.helper,
                modifier = Modifier.padding(Spacing.s16),
            )
        }
    }
}

// Humedad y factor de rendimiento: solo lectura. Sin dato muestra "Sin dato · entre 8 y 15 %" y sin botón Agregar.
@Composable
private fun DatosDeCalidad(d: DetalleSublote) {
    Tarjeta {
        EncabezadoSeccion(
            IconoDetFrasco, WarningBg, WarningText, "Datos de calidad",
            "Opcionales. Se agregan cuando se mide la humedad o el factor del café.",
        )
        HorizontalDivider(color = BorderDefault)
        FilaCalidad(
            IconoDetGota, "Humedad", subtituloDatoCalidad(d.rangoHumedad, d.humedadConDato),
            if (d.humedadConDato) d.humedad else null, "detalle_sublote_humedad",
        )
        HorizontalDivider(color = BorderDefault)
        FilaCalidad(
            IconoDetEtiqueta, "Factor de rendimiento", subtituloDatoCalidad(d.rangoFactor, d.factorConDato),
            if (d.factorConDato) d.factorRendimiento else null, "detalle_sublote_factor",
        )
    }
}

@Composable
internal fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(shape = AppShapes.large, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault)) {
        Column(Modifier.fillMaxWidth()) { contenido() }
    }
}

@Composable
internal fun EncabezadoSeccion(icono: ImageVector, fondo: Color, tinte: Color, titulo: String, subtitulo: String?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s16, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Box(
            Modifier.size(36.dp).background(fondo, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = AppTypography.titleCard)
            if (subtitulo != null) Text(subtitulo, style = AppTypography.helper)
        }
    }
}

@Composable
internal fun IconoCircular(icono: ImageVector) {
    Box(Modifier.size(32.dp).background(SurfaceSoft, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
internal fun FilaDato(icono: ImageVector, etiqueta: String, valor: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        IconoCircular(icono)
        Text(etiqueta, style = AppTypography.bodyMd, modifier = Modifier.weight(1f))
        valor()
    }
}

@Composable
internal fun Valor(texto: String, tag: String? = null) {
    Text(
        texto,
        style = AppTypography.valueStrongBold,
        textAlign = TextAlign.End,
        modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
    )
}

@Composable
private fun FilaCalidad(icono: ImageVector, titulo: String, subtitulo: String, valor: String?, tag: String) {
    Row(
        Modifier.fillMaxWidth().padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        IconoCircular(icono)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = AppTypography.valueStrong)
            Text(subtitulo, style = AppTypography.helper, modifier = Modifier.testTag("${tag}_ayuda"))
        }
        if (valor != null) Valor(valor, tag) else Box(Modifier.testTag(tag))
    }
}

// Íconos de línea (Lucide) de Inventario, definidos aquí para no editar AppIcons.kt, que comparte todo el equipo.
internal val IconoDetInfo by lazy { icono("Info", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20Z", "M12 16v-4", "M12 8h.01") }
private val IconoDetEstrella by lazy {
    icono("Estrella", "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z")
}
internal val IconoDetReloj by lazy { icono("Reloj", "M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20Z", "M12 6v6l4 2") }
internal val IconoDetGota by lazy {
    icono("Gota", "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z")
}
internal val IconoDetEtiqueta by lazy {
    icono(
        "Etiqueta",
        "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z",
        "M7.5 7.5h.01",
    )
}
private val IconoDetFrasco by lazy {
    icono(
        "Frasco",
        "M10 2v7.527a2 2 0 0 1-.211.896L4.72 20.55a1 1 0 0 0 .9 1.45h12.76a1 1 0 0 0 .9-1.45l-5.069-10.127A2 2 0 0 1 14 9.527V2",
        "M8.5 2h7", "M7 16h10",
    )
}
// Balanza (el ícono de Ajustar peso en el Figma) para las filas de peso.
private val IconoDetPeso by lazy {
    icono(
        "Balanza",
        "m16 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z",
        "m2 16 3-8 3 8c-.87.65-1.92 1-3 1s-2.13-.35-3-1Z",
        "M7 21h10", "M12 3v18", "M3 7h2c2 0 5-1 7-2 2 1 5 2 7 2h2",
    )
}
