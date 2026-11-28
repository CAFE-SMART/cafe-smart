package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.ui.components.AppTextField
import com.cafesmart.app.ui.components.ChoiceButton
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.components.EncabezadoPantalla
import com.cafesmart.app.ui.screens.purchase.PurchaseBottomBar
import com.cafesmart.app.ui.screens.purchase.PurchaseDatePickerDialog
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.drying.FinalizarSecadoUiState
import com.cafesmart.app.viewmodel.drying.SecadoEnCursoEnPantalla
import com.cafesmart.app.viewmodel.drying.SecadosEnCursoUiState

// Paso 5 de Secado: Finalizar secado (Figma "Secado · Paso 5 · Finalizar").
// Vista (MVVM): no tiene logica. Solo recuerda algo visual: si el calendario esta abierto.
// tarjeta es el secado tal como lo muestra el paso 4 (titulo, sublotes, calidades); el estado de este paso
// no trae esos textos. Si es null (o el estado no tiene secado) el secado ya no esta en curso.
@Composable
fun SecadoStep5Screen(
    estado: FinalizarSecadoUiState,
    tarjeta: SecadoEnCursoEnPantalla?,
    onKilosChange: (Calidad, String) -> Unit,
    onFactorChange: (String) -> Unit,
    onFechaChange: (Long) -> Unit,
    onGastosChange: (Boolean) -> Unit,
    onConfirmar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable: el calendario abierto sobrevive a girar la pantalla.
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            // El teclado sube toda la pantalla: la barra con "Confirmar resultado" queda encima de el
            // y el contenido (que se desplaza) ocupa el espacio que sobra.
            .imePadding()
    ) {
        EncabezadoPantalla(titulo = "Finalizar secado", onBack = onBack)

        if (estado.secado == null || tarjeta == null) {
            // Estado vacio: el secado ya no esta en curso (por ejemplo, ya se finalizo o se cancelo).
            Text(
                text = "Este secado ya no está en curso.",
                style = AppTypography.bodyMd,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.s20, vertical = Spacing.s48)
            )
            PurchaseBottomBar {
                ContinueButton(text = "Volver", onClick = onBack)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            Text(text = "¿Cuánto café seco salió?", style = AppTypography.titleScreen)

            TarjetaDelSecado(tarjeta)

            TarjetaFecha(
                etiqueta = "Fecha de finalización",
                fecha = estado.fechaFinTexto,
                onCambiar = { mostrarCalendario = true }
            )
            estado.errorFecha?.let { Text(text = it, style = AppTypography.helper.copy(color = ErrorText)) }

            // Kilos secos por calidad y factor de rendimiento
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, AppShapes.medium)
                    .border(1.dp, BorderDefault, AppShapes.medium)
                    .padding(Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                Text(text = "Kilos secos por calidad", style = AppTypography.titleCard)
                Calidad.entries.forEach { calidad ->
                    AppTextField(
                        label = "Calidad ${calidad.etiqueta.lowercase()}",
                        value = estado.kilosEnPantalla(calidad),
                        onValueChange = { onKilosChange(calidad, it) },
                        keyboardType = KeyboardType.Decimal,
                        suffixText = "kg"
                    )
                }
                // La ayuda es del grupo (no de una calidad); si la suma no sirve, el mismo lugar muestra el error.
                val errorKilos = estado.errorKilos
                Text(
                    text = errorKilos ?: estado.ayudaKilos.orEmpty(),
                    style = AppTypography.helper.copy(color = if (errorKilos != null) ErrorText else AppTypography.helper.color)
                )
                HorizontalDivider(color = BorderDefault)
                AppTextField(
                    label = "Factor de rendimiento",
                    value = estado.factorEnPantalla,
                    onValueChange = onFactorChange,
                    keyboardType = KeyboardType.Number,
                    helper = estado.ayudaFactor,
                    error = estado.errorFactor
                )
            }

            // Resumen: solo cuando los kilos sirven y se puede calcular la merma
            estado.resumen?.let { resumen ->
                Text(text = "Resumen", style = AppTypography.labelSection, modifier = Modifier.padding(top = Spacing.s4))
                Surface(
                    shape = AppShapes.medium,
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        FilaDeResumen(etiqueta = "Tipo de café") { Text(text = "Verde", style = AppTypography.valueStrongBold) }
                        HorizontalDivider(color = BorderDefault)
                        FilaDeResumen(etiqueta = "Calidad") {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                                tarjeta.calidades.forEach { EtiquetaCalidad(calidad = it, texto = it.etiqueta) }
                            }
                        }
                        HorizontalDivider(color = BorderDefault)
                        FilaDeResumen(etiqueta = "Entrada") { Text(text = resumen.entradaTexto, style = AppTypography.valueStrongBold) }
                        HorizontalDivider(color = BorderDefault)
                        FilaDeResumen(etiqueta = "Salida seca") { Text(text = resumen.salidaSecaTexto, style = AppTypography.valueStrongBold) }
                        HorizontalDivider(color = BorderDefault)
                        FilaDeResumen(etiqueta = "Merma") {
                            Text(text = "${resumen.mermaKgTexto} · ${resumen.mermaPorcentajeTexto}", style = AppTypography.valueStrongBold)
                        }
                        FilaFinalAzul(etiqueta = "Valor de la merma", valor = resumen.valorMermaTexto)
                    }
                }
            }

            // ¿Hubo gastos? Solo decide a donde se va despues de confirmar.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, AppShapes.medium)
                    .border(1.dp, BorderDefault, AppShapes.medium)
                    .padding(Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                Text(text = "¿Hubo gastos en el secado?", style = AppTypography.titleCard)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                    ChoiceButton(text = "No", selected = !estado.huboGastos, onClick = { onGastosChange(false) }, modifier = Modifier.weight(1f))
                    ChoiceButton(text = "Sí", selected = estado.huboGastos, onClick = { onGastosChange(true) }, modifier = Modifier.weight(1f))
                }
            }
        }

        PurchaseBottomBar {
            ContinueButton(text = "Confirmar resultado", enabled = estado.puedeConfirmar, onClick = onConfirmar)
            // Texto propuesto (no esta en el Figma): lo que dijo el repositorio si rechazo la finalizacion.
            estado.errorAlConfirmar?.let {
                Text(
                    text = it,
                    style = AppTypography.helper.copy(color = ErrorText),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (mostrarCalendario) {
        PurchaseDatePickerDialog(
            fechaMilis = estado.fechaMilis,
            onFechaChange = onFechaChange,
            onCerrar = { mostrarCalendario = false }
        )
    }
}

// Tarjeta del secado que se finaliza: icono, titulo, "4 sublotes · 290 kg enviados" y calidades.
@Composable
private fun TarjetaDelSecado(tarjeta: SecadoEnCursoEnPantalla) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, AppShapes.medium)
            .border(1.dp, BorderDefault, AppShapes.medium)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            IconoDeSecado()
            Column {
                Text(text = tarjeta.titulo, style = AppTypography.titleCard)
                Text(text = "${tarjeta.sublotesTexto} · ${tarjeta.kgEnviadosTexto} enviados", style = AppTypography.bodyMd)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            tarjeta.calidades.forEach { EtiquetaCalidad(calidad = it, texto = it.etiqueta) }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun Paso5Preview() {
    val repositorio = SecadoRepositoryEjemplo()
    val verde = repositorio.inventario().first { it.tipoCafe == TipoCafe.VERDE }
    val id = repositorio.iniciarSecado(Fecha(3, 6, 2026), listOf(EnvioASecar(verde.sublote.id, 1.0))).id
    val hoy = Fecha(6, 6, 2026)
    val enCurso = SecadosEnCursoUiState(
        secados = repositorio.secadosEnCurso(), inventario = repositorio.inventario(), hoy = hoy
    )
    CafesmartTheme {
        SecadoStep5Screen(
            estado = FinalizarSecadoUiState(
                secado = repositorio.secadosEnCurso().first { it.secado.id == id },
                kilosBuena = "0.5", factorTexto = "92", hoy = hoy
            ),
            tarjeta = enCurso.tarjetaElegida,
            onKilosChange = { _, _ -> }, onFactorChange = {}, onFechaChange = {},
            onGastosChange = {}, onConfirmar = {}, onBack = {}
        )
    }
}
