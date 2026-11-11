package com.cafesmart.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.ui.auth.AuthField
import com.cafesmart.app.ui.components.AvisoSuperior
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.components.PieDeAccion
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Primary
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.TextMuted
import com.cafesmart.app.viewmodel.purchase.formatoKg
import com.cafesmart.app.viewmodel.settings.CapacidadFormulario
import com.cafesmart.app.viewmodel.settings.CapacidadUiState
import com.cafesmart.app.viewmodel.settings.DIGITOS_MAX_CAPACIDAD
import java.util.Locale

// Pantalla "Ajustes · Capacidad de bodega" (Figma, sección NUEVAS): cuántos kilos caben y cuánto pesa un bulto.
// Arriba se ve cuánto hay hoy y cuánto queda libre con la capacidad escrita, que se actualiza al escribir.
// Sin estado propio: recibe el estado y avisa con callbacks.

@Composable
fun CapacidadBodegaScreen(
    estado: CapacidadUiState,
    onBack: () -> Unit,
    onEditar: ((CapacidadFormulario) -> CapacidadFormulario) -> Unit,
    onGuardar: () -> Unit,
    onAvisoOculto: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(SurfaceScreen).imePadding()) {
            EncabezadoPantalla(titulo = "Capacidad de bodega", onBack = onBack)
            when {
                estado.sinSesion -> Box(Modifier.fillMaxSize().padding(Spacing.s20), contentAlignment = Alignment.Center) {
                    Text("Inicia sesión para configurar tu bodega.", style = AppTypography.bodyLg)
                }
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s16)
                    ) {
                        Text(
                            text = "Escribe cuántos kilos de café caben en tu bodega y cuánto pesa un bulto.",
                            style = AppTypography.bodyMd
                        )
                        Resumen(estado)
                        AuthField(
                            title = "Kilos que caben en tu bodega",
                            value = estado.form.capacidad,
                            // Solo números, con un tope técnico de dígitos; la capacidad la define cada negocio.
                            onValueChange = { valor ->
                                onEditar { it.copy(capacidad = valor.filter(Char::isDigit).take(DIGITOS_MAX_CAPACIDAD)) }
                            },
                            hint = "Ej: 3000",
                            help = "Depende del tamaño de tu bodega.",
                            error = estado.errores["capacidad"],
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AuthField(
                            title = "Peso de un bulto (kg)",
                            value = estado.form.pesoBulto,
                            onValueChange = { valor ->
                                onEditar { it.copy(pesoBulto = valor.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6)) }
                            },
                            hint = "Ej: 40,7",
                            help = "Se usa para pasar de kilos a bultos.",
                            error = estado.errores["bulto"],
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    PieDeAccion(
                        textoBoton = "Guardar cambios",
                        botonActivo = !estado.guardando,
                        cargando = estado.guardando,
                        onBoton = onGuardar,
                    )
                }
            }
        }
        // Aviso rojo si no se pudo guardar.
        AvisoSuperior(aviso = estado.aviso, onOculto = onAvisoOculto, modifier = Modifier.align(Alignment.TopCenter))
    }
}

// Dos tarjetas: lo que hay hoy (en kg y bultos) y lo que queda libre con la capacidad escrita.
@Composable
private fun Resumen(estado: CapacidadUiState) {
    val bulto = estado.pesoBultoKg
    val libres = estado.capacidadKg?.let { it - estado.kgOcupados }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Tarjeta(
            titulo = "En bodega",
            valor = formatoKg(estado.kgOcupados),
            detalle = bulto?.let { bultos(estado.kgOcupados, it) } ?: "",
            modifier = Modifier.weight(1f)
        )
        Tarjeta(
            titulo = "Disponible",
            valor = libres?.let { formatoKg(maxOf(it, 0.0)) } ?: "—",
            detalle = if (libres != null) "libres" else "Falta la capacidad",
            modifier = Modifier.weight(1f),
            destacado = libres != null
        )
    }
}

@Composable
private fun Tarjeta(titulo: String, valor: String, detalle: String, modifier: Modifier, destacado: Boolean = false) {
    Column(
        modifier = modifier
            .background(SurfaceCard, AppShapes.medium)
            .border(BorderStroke(1.dp, BorderDefault), AppShapes.medium)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(titulo, style = AppTypography.labelSection.copy(color = TextMuted))
        Text(valor, style = AppTypography.valueStrong.copy(color = if (destacado) SuccessText else androidx.compose.ui.graphics.Color.Unspecified))
        if (detalle.isNotEmpty()) Text(detalle, style = AppTypography.helper.copy(color = TextMuted))
    }
}

private fun bultos(kg: Double, pesoBulto: Double): String =
    String.format(Locale.US, "%.1f", kg / pesoBulto).replace('.', ',') + " bultos"

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CapacidadBodegaPreview() {
    CafesmartTheme {
        CapacidadBodegaScreen(
            estado = CapacidadUiState(
                form = CapacidadFormulario("3000", "40,7"), kgOcupados = 834.0, cargando = false
            ),
            onBack = {}, onEditar = {}, onGuardar = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CapacidadBodegaErrorPreview() {
    CafesmartTheme {
        CapacidadBodegaScreen(
            estado = CapacidadUiState(
                form = CapacidadFormulario("500", "40,7"), kgOcupados = 834.0, cargando = false,
                errores = mapOf("capacidad" to "La capacidad no puede ser menor al café que ya tienes (834 kg).")
            ),
            onBack = {}, onEditar = {}, onGuardar = {}
        )
    }
}
