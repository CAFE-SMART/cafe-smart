package com.cafesmart.app.ui.finance

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoGasto
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.viewmodel.finance.DiaBalance
import com.cafesmart.app.viewmodel.finance.DistribucionGastos
import com.cafesmart.app.viewmodel.finance.GraficasUiState
import com.cafesmart.app.viewmodel.finance.escalaMonetaria
import com.cafesmart.app.viewmodel.finance.etiquetaDia
import com.cafesmart.app.viewmodel.finance.fechaParaLeer
import com.cafesmart.app.viewmodel.finance.pesosCortos
import com.cafesmart.app.viewmodel.purchase.formatoMiles

// Gráficas del Resumen financiero (ciclo 3, Amelia), con Compose Canvas y sin librerías externas:
// 1. "Distribución de gastos": dona con el total en el centro y la leyenda de categorías debajo (Hoy o Esta semana).
// 2. "Balance semanal" (solo con "Esta semana"): por día, barra verde de ingresos y barra coral de lo que costó lo
//    vendido. Se toca la barra y aparece un único tooltip con el valor exacto; tocar fuera lo oculta.
// Viven dentro del Resumen financiero, protegido por la clave financiera.

internal val VerdeIngresos = Color(0xFF369775)
internal val CoralCostos = Color(0xFFE77C72)
internal val VerdeOscuro = Color(0xFF287B57)
internal val RojoOscuro = Color(0xFFB94D42)
private val FondoTooltip = Color(0xFF172033)

const val TEXTO_SIN_GASTOS = "Aún no hay gastos registrados en este período"
const val TEXTO_BALANCE_SIN_VENTAS = "El balance se mostrará cuando las ventas estén conectadas a la app."

/** Color de cada categoría de gasto (además del color, la leyenda lleva nombre, valor y porcentaje). */
fun colorDeGasto(tipo: TipoGasto): Color = when (tipo) {
    TipoGasto.TRANSPORTE -> Color(0xFF285DB7)
    TipoGasto.COMIDA -> Color(0xFFE0A43A)
    TipoGasto.SECADO -> Color(0xFF8B6CC9)
    TipoGasto.CARGUE -> CoralCostos
    TipoGasto.DESCARGUE -> VerdeIngresos
    TipoGasto.OTROS -> Color(0xFF8A94A6)
}

/** "$350.000" (formato del tooltip). */
fun pesosExactos(valor: Long): String = "$" + formatoMiles(valor)

@Composable
fun GraficasFinancieras(estado: GraficasUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        TarjetaGrafica("Distribución de gastos", "graficas_distribucion") {
            when {
                estado.cargando -> Cargando()
                estado.error != null -> TextoError(estado.error)
                estado.distribucion == null || estado.distribucion.vacia -> Text(
                    TEXTO_SIN_GASTOS, style = AppTypography.bodyMd.copy(color = TextSecondary),
                    modifier = Modifier.testTag("gastos_vacios")
                )
                else -> DonaDeGastos(estado.distribucion)
            }
        }
        // Con "Hoy" la tarjeta no existe (no queda espacio vacío).
        if (estado.muestraBalance) {
            TarjetaGrafica("Balance semanal", "graficas_balance", subtitulo = rangoSemana(estado.semana)) {
                when {
                    estado.cargando -> Cargando()
                    estado.error != null -> TextoError(estado.error)
                    !estado.ventasConectadas || estado.balance == null -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No disponible", style = AppTypography.valueStrong.copy(color = TextMuted), modifier = Modifier.testTag("balance_no_disponible"))
                        Text(TEXTO_BALANCE_SIN_VENTAS, style = AppTypography.bodyMd.copy(color = TextSecondary))
                    }
                    else -> BalanceSemanal(estado.balance)
                }
            }
        }
    }
}

/** "Sáb 21 – Vie 27". */
fun rangoSemana(semana: List<Fecha>): String? =
    if (semana.isEmpty()) null else "${etiquetaDia(semana.first())} – ${etiquetaDia(semana.last())}"

@Composable
private fun TarjetaGrafica(titulo: String, tag: String, subtitulo: String? = null, contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp), color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    ) {
        Column(Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = AppTypography.titleCard, modifier = Modifier.semantics { heading() })
                subtitulo?.let { Text(it, style = AppTypography.helper.copy(color = TextMuted)) }
            }
            contenido()
        }
    }
}

@Composable
private fun Cargando() = Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = Primary)
}

@Composable
private fun TextoError(texto: String) =
    Text(texto, style = AppTypography.bodyMd.copy(color = ErrorText), modifier = Modifier.testTag("graficas_error"))

// ───────────────────────── Dona de gastos ─────────────────────────

@Composable
private fun DonaDeGastos(d: DistribucionGastos) {
    val total = pesosExactos(d.totalPesos)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s16), horizontalAlignment = Alignment.CenterHorizontally) {
        val diametro = 176.dp
        val grosor = 26.dp
        Box(
            Modifier.size(diametro).semantics(mergeDescendants = true) {
                contentDescription = "Total de gastos $total. " +
                    d.categorias.joinToString(". ") { "${it.tipo.etiqueta} ${pesosExactos(it.pesos)}, ${it.porcentaje} por ciento" }
            }.testTag("dona_gastos"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val g = grosor.toPx()
                val separacion = if (d.categorias.size > 1) 1.5f else 0f
                var inicio = -90f
                d.categorias.forEach { c ->
                    val barrido = 360f * c.pesos / d.totalPesos
                    drawArc(
                        color = colorDeGasto(c.tipo), startAngle = inicio + separacion / 2, sweepAngle = (barrido - separacion).coerceAtLeast(0.5f),
                        useCenter = false, topLeft = Offset(g / 2, g / 2), size = Size(size.width - g, size.height - g),
                        style = Stroke(width = g, cap = StrokeCap.Butt),
                    )
                    inicio += barrido
                }
            }
            // El total se achica si no cabe (montos grandes o fuente ampliada); nunca se corta.
            val densidad = LocalDensity.current
            val interior = diametro - grosor * 2 - 12.dp
            val tamano = with(densidad) { (interior.toPx() / (total.length * 0.6f)).toSp() }.let { if (it.value > 18f) 18.sp else it }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(interior)) {
                Text("Total de gastos", style = AppTypography.helper.copy(color = TextMuted), textAlign = TextAlign.Center)
                Text(total, style = AppTypography.valueStrong.copy(color = TextPrimary, fontSize = tamano, fontWeight = FontWeight.Bold),
                    maxLines = 1, softWrap = false, modifier = Modifier.testTag("dona_total"))
            }
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            d.categorias.forEach { c ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                    Box(Modifier.size(12.dp).background(colorDeGasto(c.tipo), CircleShape))
                    Text(c.tipo.etiqueta, style = AppTypography.labelField, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(pesosExactos(c.pesos), style = AppTypography.valueStrong)
                        Text("${c.porcentaje} %", style = AppTypography.helper.copy(color = TextMuted))
                    }
                }
            }
        }
    }
}

// ───────────────────────── Balance semanal ─────────────────────────

/** Barra seleccionada: el día (0..6) y la serie (true = ingresos, false = costos y gastos). */
data class BarraElegida(val dia: Int, val ingresos: Boolean)

/** Geometría de la gráfica, compartida por el dibujo, los toques y TalkBack. Todo en píxeles. */
internal class GeometriaBalance(val ancho: Float, val alto: Float, val izquierda: Float, val arriba: Float, val abajo: Float, val anchoBarra: Float, val hueco: Float) {
    val anchoGrupo: Float get() = (ancho - izquierda) / 7f
    fun centro(dia: Int): Float = izquierda + anchoGrupo * (dia + 0.5f)
    fun xBarra(dia: Int, ingresos: Boolean): Float = if (ingresos) centro(dia) - hueco / 2 - anchoBarra else centro(dia) + hueco / 2
    /** Qué barra se tocó: la columna del día, mitad izquierda ingresos y derecha costos. null = fuera. */
    fun barraEn(x: Float, y: Float): BarraElegida? {
        if (x < izquierda || x > ancho || y < 0f || y > alto) return null
        val dia = ((x - izquierda) / anchoGrupo).toInt().coerceIn(0, 6)
        // Los extremos de cada columna (separación entre días) no seleccionan nada.
        if (kotlin.math.abs(x - centro(dia)) > anchoGrupo * 0.45f) return null
        return BarraElegida(dia, x < centro(dia))
    }
}

@Composable
fun BalanceSemanal(dias: List<DiaBalance>, modifier: Modifier = Modifier) {
    // La selección se limpia si cambian los datos (otro periodo o recarga).
    var elegida by remember(dias) { mutableStateOf<BarraElegida?>(null) }
    val crecer = remember(dias) { Animatable(0f) }
    LaunchedEffect(dias) { crecer.animateTo(1f, tween(450)) }
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 11.sp, color = TextMuted)
    val estiloTooltip = TextStyle(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    val escala = escalaMonetaria(dias.maxOfOrNull { maxOf(it.ingresosPesos, it.costosPesos) } ?: 0L)
    val densidad = LocalDensity.current

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val anchoPx = with(densidad) { maxWidth.toPx() }
            val etiquetasY = escala.marcas.map { medidor.measure(pesosCortos(it), estiloEje) }
            val etiquetasX = dias.map { medidor.measure("${etiquetaDia(it.fecha).substringBefore(' ')}\n${it.fecha.dia}", estiloEje.copy(textAlign = TextAlign.Center)) }
            val muestraTooltip = medidor.measure("$0", estiloTooltip)
            val g = with(densidad) {
                val arriba = muestraTooltip.size.height + 14.dp.toPx()           // espacio reservado para el tooltip
                val abajoEtiquetas = (etiquetasX.maxOfOrNull { it.size.height } ?: 0) + 6.dp.toPx()
                val altoGrafica = 170.dp.toPx()
                val izquierda = (etiquetasY.maxOfOrNull { it.size.width } ?: 0) + 8.dp.toPx()
                val anchoGrupo = (anchoPx - izquierda) / 7f
                GeometriaBalance(
                    ancho = anchoPx, alto = arriba + altoGrafica + abajoEtiquetas, izquierda = izquierda,
                    arriba = arriba, abajo = arriba + altoGrafica,
                    anchoBarra = (anchoGrupo * 0.3f).coerceAtMost(14.dp.toPx()), hueco = 3.dp.toPx(),
                )
            }
            val altoDp: Dp = with(densidad) { g.alto.toDp() }
            Box(
                Modifier.fillMaxWidth().height(altoDp).testTag("balance_grafica")
                    .pointerInput(dias, g.ancho) { detectTapGestures { p -> elegida = g.barraEn(p.x, p.y) } }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val altoUtil = g.abajo - g.arriba
                    // Líneas guía discontinuas y escala.
                    escala.marcas.forEachIndexed { i, marca ->
                        val y = g.abajo - altoUtil * (marca.toFloat() / escala.tope)
                        drawLine(BorderDefault, Offset(g.izquierda, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        val t = etiquetasY[i]
                        drawText(t, topLeft = Offset(g.izquierda - 8.dp.toPx() - t.size.width, y - t.size.height / 2f))
                    }
                    dias.forEachIndexed { d, dia ->
                        listOf(true to dia.ingresosPesos, false to dia.costosPesos).forEach { (ingresos, valor) ->
                            val alto = altoUtil * (valor.toFloat() / escala.tope) * crecer.value
                            if (alto <= 0f) return@forEach
                            val esta = elegida == BarraElegida(d, ingresos)
                            val color = when {
                                ingresos && esta -> VerdeOscuro
                                !ingresos && esta -> RojoOscuro
                                ingresos -> VerdeIngresos
                                else -> CoralCostos
                            }
                            barraRedondeada(g.xBarra(d, ingresos), g.abajo - alto, g.anchoBarra, alto, color)
                        }
                        val t = etiquetasX[d]
                        drawText(t, topLeft = Offset(g.centro(d) - t.size.width / 2f, g.abajo + 6.dp.toPx()))
                    }
                    // Tooltip único sobre la barra elegida, sin salirse de la gráfica.
                    elegida?.let { e ->
                        val dia = dias.getOrNull(e.dia) ?: return@let
                        val valor = if (e.ingresos) dia.ingresosPesos else dia.costosPesos
                        val texto = medidor.measure(pesosExactos(valor), estiloTooltip)
                        val relleno = 8.dp.toPx()
                        val w = texto.size.width + relleno * 2
                        val h = texto.size.height + relleno
                        val alto = altoUtil * (valor.toFloat() / escala.tope)
                        val xCentro = g.xBarra(e.dia, e.ingresos) + g.anchoBarra / 2
                        val x = (xCentro - w / 2).coerceIn(0f, size.width - w)
                        val y = (g.abajo - alto - 6.dp.toPx() - h).coerceAtLeast(0f)
                        drawRoundRect(FondoTooltip, Offset(x, y), Size(w, h), CornerRadius(6.dp.toPx()))
                        drawText(texto, topLeft = Offset(x + relleno, y + relleno / 2))
                    }
                }
                // TalkBack: un nodo por barra (día, concepto e importe) sobre la misma zona que se toca.
                Row(Modifier.fillMaxSize().padding(start = with(densidad) { g.izquierda.toDp() })) {
                    dias.forEachIndexed { d, dia ->
                        listOf(true, false).forEach { ingresos ->
                            val valor = if (ingresos) dia.ingresosPesos else dia.costosPesos
                            Box(
                                Modifier.weight(1f).fillMaxSize().semantics {
                                    contentDescription = "${fechaParaLeer(dia.fecha)}, ${if (ingresos) "ingresos" else "costos y gastos"} ${pesosExactos(valor)}"
                                    role = Role.Button
                                    selected = elegida == BarraElegida(d, ingresos)
                                    onClick(label = "Mostrar el valor") { elegida = BarraElegida(d, ingresos); true }
                                }.testTag("barra_${d}_${if (ingresos) "ingresos" else "costos"}")
                            )
                        }
                    }
                }
                elegida?.let { e ->
                    // Texto invisible para pruebas y TalkBack: lo que muestra el tooltip.
                    val dia = dias.getOrNull(e.dia)
                    if (dia != null) Box(Modifier.size(1.dp).offset(0.dp, 0.dp).semantics {
                        contentDescription = "Valor seleccionado ${pesosExactos(if (e.ingresos) dia.ingresosPesos else dia.costosPesos)}"
                    }.testTag("balance_tooltip"))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s16), verticalAlignment = Alignment.CenterVertically) {
            listOf("Ingresos" to VerdeIngresos, "Costos y gastos" to CoralCostos).forEach { (texto, color) ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                    Text(texto, style = AppTypography.helper)
                }
            }
        }
    }
}

/** Barra con solo las esquinas de arriba redondeadas. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.barraRedondeada(x: Float, y: Float, w: Float, h: Float, color: Color) {
    val r = minOf(4.dp.toPx(), w / 2, h)
    val p = Path().apply {
        moveTo(x, y + h); lineTo(x, y + r)
        quadraticTo(x, y, x + r, y); lineTo(x + w - r, y)
        quadraticTo(x + w, y, x + w, y + r); lineTo(x + w, y + h); close()
    }
    drawPath(p, color)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1100)
@Composable
private fun GraficasSemanaPreview() {
    val hoy = Fecha(27, 11, 2026)
    val semana = com.cafesmart.app.viewmodel.finance.diasDeLaSemana(hoy)
    CafesmartTheme {
        Box(Modifier.padding(16.dp)) {
            GraficasFinancieras(
                GraficasUiState(
                    periodo = PeriodoFinanciero.SEMANA, cargando = false,
                    distribucion = com.cafesmart.app.viewmodel.finance.distribucionGastos(
                        listOf(
                            com.cafesmart.app.data.finanzas.GastoRegistrado(hoy, 120_000, tipo = TipoGasto.TRANSPORTE),
                            com.cafesmart.app.data.finanzas.GastoRegistrado(hoy, 45_000, tipo = TipoGasto.CARGUE),
                        ), PeriodoFinanciero.SEMANA, hoy,
                    ),
                    balance = semana.mapIndexed { i, f -> DiaBalance(f, (i * 150_000L), (i * 90_000L)) },
                    semana = semana,
                )
            )
        }
    }
}
