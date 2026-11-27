package com.cafesmart.app.ui.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorDot
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessSolid
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.viewmodel.finance.GraficasUiState
import com.cafesmart.app.viewmodel.finance.MesFinanciero
import com.cafesmart.app.viewmodel.finance.etiquetaMes
import com.cafesmart.app.viewmodel.finance.maximoDeLaSerie
import com.cafesmart.app.viewmodel.purchase.formatoDinero
import kotlin.math.abs

// Gráficas financieras dibujadas con Compose Canvas (sin librerías externas): por cada mes, tres barras
// (ingresos, gastos y utilidad). La utilidad negativa baja de la línea del cero. Todo en pesos con el formato de
// la app ("$ 1.500.000"). Se muestran dentro del Resumen financiero (protegido por la clave financiera).
//
// DISEÑO: no hay un Figma APROBADO de "Finanzas · Gráficas financieras"; esta presentación es provisional y se
// ajustará cuando Andrés apruebe el diseño (colores, tipo de gráfica y ubicación).

internal val ColorIngresos = SuccessSolid
internal val ColorGastos = ErrorDot
internal val ColorUtilidad = Primary

const val TEXTO_GRAFICAS_VACIAS = "Cuando registres ventas o gastos, aquí verás cómo cambian mes a mes."

@Composable
fun GraficasFinancieras(estado: GraficasUiState, modifier: Modifier = Modifier, soloGastos: Boolean = false) {
    Surface(
        shape = AppShapes.medium, color = SurfaceCard, border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier.fillMaxWidth().testTag("graficas_financieras"),
    ) {
        Column(Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Text("Últimos ${if (estado.serie.isEmpty()) 6 else estado.serie.size} meses", style = AppTypography.labelField)
            if (soloGastos) Text("Se muestran los gastos registrados. Ingresos y utilidad estarán disponibles cuando se conecten ventas y compras.",
                style = AppTypography.helper.copy(color = TextSecondary))
            when {
                estado.cargando -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                estado.error != null -> Text(estado.error, style = AppTypography.bodyMd.copy(color = ErrorText),
                    modifier = Modifier.testTag("graficas_error"))
                estado.vacia -> Text(if (soloGastos) "Cuando registres gastos, aquí verás cómo cambian mes a mes." else TEXTO_GRAFICAS_VACIAS, style = AppTypography.bodyMd.copy(color = TextSecondary),
                    modifier = Modifier.testTag("graficas_vacias"))
                else -> {
                    Leyenda(soloGastos)
                    Barras(estado.serie, soloGastos)
                    Text("Escala: hasta ${formatoDinero(maximoDeLaSerie(estado.serie).toDouble())}",
                        style = AppTypography.helper.copy(color = TextMuted), modifier = Modifier.testTag("graficas_escala"))
                }
            }
        }
    }
}

@Composable
private fun Leyenda(soloGastos: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12), verticalAlignment = Alignment.CenterVertically) {
        (if (soloGastos) listOf("Gastos" to ColorGastos) else listOf("Ingresos" to ColorIngresos, "Gastos" to ColorGastos, "Utilidad" to ColorUtilidad)).forEach { (texto, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(texto, style = AppTypography.helper)
            }
        }
    }
}

/** Descripción para lectores de pantalla: "Nov: ingresos $ 1.500.000, gastos $ 80.000, utilidad $ 320.000". */
fun descripcionGrafica(serie: List<MesFinanciero>): String = serie.joinToString(". ") {
    "${etiquetaMes(it)}: ingresos ${formatoDinero(it.ingresosPesos.toDouble())}, " +
        "gastos ${formatoDinero(it.gastosPesos.toDouble())}, utilidad ${formatoDinero(it.utilidadPesos.toDouble())}"
}

/** Altura relativa (0..1) de cada barra respecto a la escala; la utilidad negativa da un valor negativo. */
fun alturaRelativa(valor: Long, escala: Long): Float = if (escala <= 0) 0f else valor.toFloat() / escala

@Composable
private fun Barras(serie: List<MesFinanciero>, soloGastos: Boolean) {
    val maximo = maximoDeLaSerie(serie)
    // Si alguna utilidad es negativa, se reserva espacio debajo de la línea del cero.
    val minimo = serie.minOfOrNull { it.utilidadPesos }?.coerceAtMost(0L) ?: 0L
    val descripcion = if (soloGastos) serie.joinToString(". ") { "${etiquetaMes(it)}: gastos ${formatoDinero(it.gastosPesos.toDouble())}" } else descripcionGrafica(serie)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        Canvas(
            Modifier.fillMaxWidth().height(160.dp)
                .semantics { contentDescription = descripcion }
                .testTag("graficas_canvas")
        ) {
            val total = (maximo + abs(minimo)).toFloat().coerceAtLeast(1f)
            val cero = size.height * (maximo / total)
            val anchoGrupo = size.width / serie.size
            val anchoBarra = anchoGrupo / 4.5f
            // Línea del cero.
            drawLine(BorderDefault, Offset(0f, cero), Offset(size.width, cero), strokeWidth = 2f)
            serie.forEachIndexed { i, mes ->
                val inicio = i * anchoGrupo + anchoBarra * 0.5f
                (if (soloGastos) listOf(mes.gastosPesos to ColorGastos) else listOf(mes.ingresosPesos to ColorIngresos, mes.gastosPesos to ColorGastos, mes.utilidadPesos to ColorUtilidad))
                    .forEachIndexed { j, (valor, color) ->
                        val alto = size.height * (abs(valor) / total)
                        if (alto <= 0f) return@forEachIndexed
                        val arriba = if (valor >= 0) cero - alto else cero
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(inicio + j * (anchoBarra + 2f), arriba),
                            size = Size(anchoBarra, alto),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            serie.forEachIndexed { i, mes ->
                // El primer mes y el primero de cada año llevan el año ("Ene 2027").
                val conAnio = i == 0 || mes.mes == 1
                Text(etiquetaMes(mes, conAnio), style = AppTypography.helper.copy(color = TextMuted),
                    textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GraficasFinancierasPreview() {
    CafesmartTheme {
        GraficasFinancieras(
            GraficasUiState(
                cargando = false,
                serie = listOf(
                    MesFinanciero(2026, 6, 0, 120_000, 0),
                    MesFinanciero(2026, 7, 1_800_000, 200_000, 250_000),
                    MesFinanciero(2026, 8, 900_000, 80_000, -60_000),
                    MesFinanciero(2026, 9, 2_400_000, 150_000, 410_000),
                    MesFinanciero(2026, 10, 1_200_000, 300_000, 120_000),
                    MesFinanciero(2026, 11, 3_000_000, 90_000, 520_000),
                ),
            )
        )
    }
}
