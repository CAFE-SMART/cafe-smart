package com.cafesmart.app.ui.finance

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import com.cafesmart.app.ui.components.AvisoSuperior
import com.cafesmart.app.ui.theme.ButtonSoft
import com.cafesmart.app.ui.theme.BorderSelectedSoft
import com.cafesmart.app.ui.theme.SurfaceSelected
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.finanzas.PeriodoFinanciero
import com.cafesmart.app.data.finanzas.ResumenFinanciero
import com.cafesmart.app.ui.components.AppIcons
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.SheetShadow
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.SurfaceSoft
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.ui.theme.TextPrimary
import com.cafesmart.app.ui.theme.TextSecondary
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.finance.ResumenUiState
import com.cafesmart.app.viewmodel.finance.mensajeDeUtilidad
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.purchase.formatoMiles

// Pantalla "Resumen financiero · Panel" (Figma, sección NUEVAS de Flujos), en lenguaje de todos los días:
//   1. Tu ganancia (verde si ganas, rojo si pierdes) y lo que salió de tu bolsillo (ámbar).
//   2. "Así se calculó tu ganancia": ventas, costo del café y gastos, con la fórmula.
//   3. La merma. Sin estado propio: recibe el estado y avisa con callbacks.
// Los botones "Ver historial" y "Ver gráficas" del Figma se agregan con sus pantallas.

@Composable
fun ResumenFinancieroScreen(
    estado: ResumenUiState,
    onBack: () -> Unit,
    onPeriodo: (PeriodoFinanciero) -> Unit,
    onRecargar: () -> Unit,
    onAvisoOculto: () -> Unit = {},
) {
  Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(SurfaceScreen)) {
        EncabezadoPantalla(
            titulo = "Resumen financiero",
            onBack = onBack,
            accion = { BotonRecargar(girando = estado.actualizando, onClick = onRecargar) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.s20, end = Spacing.s20, top = Spacing.s16, bottom = Spacing.s28),
            verticalArrangement = Arrangement.spacedBy(Spacing.s28)
        ) {
            Text(
                text = "Aquí ves cuánto dinero entró y cuánto salió de tu negocio.",
                style = AppTypography.bodyLg.copy(color = TextSecondary)
            )
            SelectorDePeriodo(estado.periodo, onPeriodo)
            val resumen = estado.resumen
            when {
                resumen != null -> Contenido(resumen, estado.periodo)
                estado.error != null -> Error(estado.error, onRecargar)
                else -> Box(Modifier.fillMaxWidth().padding(Spacing.s28), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }
    }
    // Barra verde "Resumen actualizado" (o roja si falla) tras tocar recargar.
    AvisoSuperior(aviso = estado.aviso, onOculto = onAvisoOculto, modifier = Modifier.align(Alignment.TopCenter))
  }
}

// Botón de recargar: círculo azul claro (se nota que se toca), con ondita al presionar, y el ícono gira mientras
// se actualiza. Mientras gira no se puede volver a tocar.
@Composable
private fun BotonRecargar(girando: Boolean, onClick: () -> Unit) {
    val vuelta = rememberInfiniteTransition(label = "recargar")
    val grados by vuelta.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800, easing = LinearEasing)),
        label = "grados"
    )
    IconButton(
        onClick = onClick,
        enabled = !girando,
        modifier = Modifier.size(44.dp).background(ButtonSoft, CircleShape)
    ) {
        Icon(
            imageVector = IconoRecargar,
            contentDescription = "Actualizar resumen",
            tint = Primary,
            modifier = Modifier.size(22.dp).rotate(if (girando) grados else 0f)
        )
    }
}

@Composable
private fun Contenido(resumen: ResumenFinanciero, periodo: PeriodoFinanciero) {
    val colorGanancia = when {
        resumen.utilidad > 0 -> SuccessText
        resumen.utilidad < 0 -> ErrorText
        else -> TextPrimary
    }
    if (resumen.ventas == 0) {
        Text(
            text = "Cuando registres una venta, aquí verás cuánto ganaste y de dónde sale. Por ahora todo está en cero.",
            style = AppTypography.bodyMd,
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSelected, AppShapes.medium)
                .border(BorderStroke(1.dp, BorderSelectedSoft), AppShapes.medium)
                .padding(Spacing.s16)
        )
    }
    // Resultado: lo principal que se lee primero.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, AppShapes.large, ambientColor = SheetShadow, spotColor = SheetShadow)
            .background(SurfaceCard, AppShapes.large)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.large)
            .padding(Spacing.s20),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Bloque(
            titulo = "Tu ganancia de ${periodo.etiqueta.lowercase()}",
            valor = pesos(resumen.utilidad),
            colorValor = colorGanancia,
            grande = true,
            explicacion = "Es lo que te queda después de pagar el café y tus gastos.",
            mensaje = mensajeDeUtilidad(resumen.utilidad, periodo, resumen.ventas)
        )
        HorizontalDivider(color = BorderDefault)
        Bloque(
            titulo = "Lo que salió de tu bolsillo",
            valor = pesos(resumen.salio),
            colorValor = WarningText,
            grande = false,
            explicacion = "El costo del café que vendiste más tus gastos."
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Text("Así se calculó tu ganancia", style = AppTypography.labelSection.copy(color = TextMuted))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(SurfaceCard)
                .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
        ) {
            FilaDetalle(IconoDinero, "Ventas", "Lo que cobraste por el café que vendiste", "+ " + pesos(resumen.ingresos), SuccessText)
            HorizontalDivider(color = BorderDefault)
            FilaDetalle(AppIcons.Package, "Costo del café", "Lo que pagaste por el café que vendiste", "− " + pesos(resumen.costoCafe), WarningText)
            HorizontalDivider(color = BorderDefault)
            FilaDetalle(IconoRecibo, "Gastos", "Transporte, cargue y otros pagos", "− " + pesos(resumen.gastos), WarningText)
        }
        Text("Ganancia = ventas − costo del café − gastos", style = AppTypography.bodyMd.copy(color = TextSecondary))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
            .padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(Modifier.size(40.dp).background(WarningBg, AppShapes.medium), contentAlignment = Alignment.Center) {
            Icon(IconoGota, contentDescription = null, tint = WarningText, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Café perdido al secar (merma)", style = AppTypography.labelField)
            Text("Es el peso que baja cuando el café se seca.", style = AppTypography.helper.copy(color = TextMuted))
        }
        Text(formatoKg(resumen.mermaKg), style = AppTypography.valueStrong)
    }
}

@Composable
private fun Bloque(
    titulo: String,
    valor: String,
    colorValor: Color,
    grande: Boolean,
    explicacion: String,
    mensaje: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, style = AppTypography.labelSection.copy(color = TextMuted))
        Text(valor, style = (if (grande) AppTypography.displayValorXl else AppTypography.displayValor).copy(color = colorValor))
        Text(explicacion, style = AppTypography.bodyMd.copy(color = TextSecondary))
        mensaje?.let { Text(it, style = AppTypography.bodyMd.copy(color = colorValor)) }
    }
}

@Composable
private fun FilaDetalle(icono: ImageVector, titulo: String, detalle: String, monto: String, colorMonto: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Box(Modifier.size(40.dp).background(SurfaceSoft, AppShapes.medium), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = AppTypography.labelField)
            Text(detalle, style = AppTypography.helper.copy(color = TextMuted))
        }
        Text(monto, style = AppTypography.valueStrong.copy(color = colorMonto))
    }
}

// Selector "Hoy / Esta semana": gris con la opción activa en blanco (neutro, sin azul).
@Composable
private fun SelectorDePeriodo(actual: PeriodoFinanciero, onPeriodo: (PeriodoFinanciero) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceSoft, AppShapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PeriodoFinanciero.entries.forEach { periodo ->
            val activo = periodo == actual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (activo) Modifier.shadow(1.dp, AppShapes.medium) else Modifier)
                    .background(if (activo) SurfaceCard else Color.Transparent, AppShapes.medium)
                    .selectable(selected = activo, role = Role.Tab, onClick = { onPeriodo(periodo) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = periodo.etiqueta,
                    style = AppTypography.labelField.copy(color = if (activo) TextPrimary else TextMuted)
                )
            }
        }
    }
}

@Composable
private fun Error(mensaje: String, onReintentar: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(mensaje, style = AppTypography.bodyMd.copy(color = ErrorText))
        TextButton(onClick = onReintentar) { Text("Reintentar", style = AppTypography.buttonLink.copy(color = Primary)) }
    }
}

/** "$ 2.400.000" (con signo menos delante si es negativo). */
private fun pesos(valor: Long): String =
    (if (valor < 0) "− " else "") + "$ " + formatoMiles(kotlin.math.abs(valor))

// Íconos de línea (Lucide), privados para no editar AppIcons.kt, que comparte todo el equipo.
private fun icono(nombre: String, vararg trazos: String): ImageVector =
    ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        trazos.forEach {
            addPath(
                pathData = addPathNodes(it), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

private val IconoRecargar by lazy {
    icono("Recargar", "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5", "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5")
}
private val IconoDinero by lazy {
    icono("Dinero", "M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z", "M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6Z", "M6 12h.01", "M18 12h.01")
}
private val IconoRecibo by lazy {
    icono("Recibo", "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z", "M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8", "M12 17.5v-11")
}
private val IconoGota by lazy {
    icono("Gota", "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z")
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ResumenGananciaPreview() {
    CafesmartTheme {
        ResumenFinancieroScreen(
            ResumenUiState(resumen = ResumenFinanciero(2_400_000, 1_800_000, 45_000, 555_000, 12.0), cargando = false),
            {}, {}, {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ResumenPerdidaPreview() {
    CafesmartTheme {
        ResumenFinancieroScreen(
            ResumenUiState(
                periodo = PeriodoFinanciero.SEMANA,
                resumen = ResumenFinanciero(900_000, 1_200_000, 60_000, -360_000, 5.0), cargando = false
            ),
            {}, {}, {}
        )
    }
}
