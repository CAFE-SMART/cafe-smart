package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.secado.EnvioASecar
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.theme.AppShapes
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.BorderDefault
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorBg
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SuccessBg
import com.cafesmart.app.ui.theme.SuccessText
import com.cafesmart.app.ui.theme.SurfaceCard
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.ui.theme.WarningBg
import com.cafesmart.app.ui.theme.WarningText
import com.cafesmart.app.viewmodel.drying.CalidadSecada
import com.cafesmart.app.viewmodel.drying.FinalizarSecadoUiState

// Paso 6 de Secado: Secado registrado (Figma "Secado · Paso 6 · Registrado").
// Vista (MVVM): sin logica ni estado propio. Muestra el resultado que guardo el ViewModel al confirmar el paso 5.
@Composable
fun SecadoStep6Screen(
    estado: FinalizarSecadoUiState,
    onRegistrarNuevo: () -> Unit,
    onIrAInventario: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
            .padding(start = Spacing.s20, end = Spacing.s20, bottom = Spacing.s28)
    ) {
        Column(
            modifier = Modifier
                // weight(1f) y scroll: si el resumen no cabe se desplaza y los botones quedan abajo
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = Spacing.s48),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CirculoCheck()
            Text(
                text = "Secado registrado",
                style = AppTypography.titleExito,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.s20)
            )
            Text(
                text = "El café seco ya está en tu inventario.",
                style = AppTypography.bodyMd,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s20)
            )
            Text(
                text = "Resumen del secado",
                style = AppTypography.labelSection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.s8)
            )
            val resumen = estado.resumenFinal
            if (resumen != null) {
                Surface(
                    shape = AppShapes.medium,
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        FilaDeResumen(etiqueta = "Tipo de café") { Text(text = "Verde a seco", style = AppTypography.valueStrongBold) }
                        HorizontalDivider(color = BorderDefault)
                        Column(
                            modifier = Modifier.padding(Spacing.s16),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
                        ) {
                            Text(text = "Calidad resultante", style = AppTypography.bodyMd)
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                                estado.calidadesSecadas.forEach { CalidadResultante(it, Modifier.weight(1f)) }
                            }
                        }
                        HorizontalDivider(color = BorderDefault)
                        FilaDeResumen(etiqueta = "Fecha de finalización") {
                            Text(text = estado.fechaFinResumenTexto.orEmpty(), style = AppTypography.valueStrongBold)
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
        }

        Column(
            modifier = Modifier.padding(top = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12)
        ) {
            ContinueButton(text = "Registrar nuevo secado", onClick = onRegistrarNuevo)
            BotonSuave(texto = "Ir a inventario", onClick = onIrAInventario)
        }
    }
}

// Una calidad que resulto: nombre y kg, con el color de su calidad
@Composable
private fun CalidadResultante(item: CalidadSecada, modifier: Modifier = Modifier) {
    val (fondo, color) = when (item.calidad) {
        Calidad.BUENA -> SuccessBg to SuccessText
        Calidad.REGULAR -> WarningBg to WarningText
        Calidad.MALA -> ErrorBg to ErrorText
    }
    Column(
        modifier = modifier
            .background(fondo, AppShapes.medium)
            .padding(vertical = Spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = item.calidad.etiqueta, style = AppTypography.pill.copy(color = color))
        Text(text = item.kgTexto, style = AppTypography.valueStrongBold.copy(color = color))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso6Preview() {
    val repositorio = SecadoRepositoryEjemplo()
    val verde = repositorio.inventario().first { it.tipoCafe == TipoCafe.VERDE }
    val id = repositorio.iniciarSecado(Fecha(3, 6, 2026), listOf(EnvioASecar(verde.sublote.id, 1.0))).id
    val resultado = repositorio.finalizarSecado(id, Fecha(6, 6, 2026), mapOf(Calidad.BUENA to 0.6, Calidad.MALA to 0.2), 92)
    CafesmartTheme {
        SecadoStep6Screen(
            estado = FinalizarSecadoUiState(
                resultado = resultado,
                calidadesSecadas = listOf(CalidadSecada(Calidad.BUENA, 0.6), CalidadSecada(Calidad.MALA, 0.2))
            ),
            onRegistrarNuevo = {},
            onIrAInventario = {}
        )
    }
}
