package com.cafesmart.app.ui.screens.drying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.data.secado.SecadoRepositoryEjemplo
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.screens.purchase.PurchaseBottomBar
import com.cafesmart.app.ui.screens.purchase.PurchaseDatePickerDialog
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.ErrorText
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.drying.SecadoUiState
import kotlinx.coroutines.runBlocking

// Paso 2 de Secado: Confirma el inicio.
// Vista (MVVM): no tiene logica. Solo recuerda algo visual: si el calendario esta abierto.
@Composable
fun SecadoStep2Screen(
    estado: SecadoUiState,
    onFechaChange: (Long) -> Unit,
    onIniciar: () -> Unit,
    onCancelar: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable: el calendario abierto sobrevive a girar la pantalla.
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceScreen)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoSecado(paso = 2, pregunta = "Confirma el inicio", onBack = onBack)

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                TarjetaFecha(etiqueta = "Fecha de inicio", fecha = estado.fechaTexto, onCambiar = { mostrarCalendario = true })

                Text(
                    text = "Café a secar",
                    style = AppTypography.labelSection,
                    modifier = Modifier.padding(top = Spacing.s4)
                )

                ResumenDeSecado(
                    calidades = estado.calidadesElegidas,
                    sublotes = estado.sublotesElegidosTexto,
                    etiquetaKg = "Cantidad a secar",
                    kg = estado.totalKgTexto
                )
            }
        }

        PurchaseBottomBar {
            // Solo se activa cuando el ViewModel dice que el secado se puede iniciar.
            ContinueButton(text = "Iniciar secado", enabled = estado.puedeIniciar, onClick = onIniciar)
            // Mismo estilo de error que el paso 5 ("Confirmar resultado"): texto pequeno en rojo y centrado.
            estado.errorAlIniciar?.let {
                Text(
                    text = it,
                    style = AppTypography.helper.copy(color = ErrorText),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(onClick = onCancelar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = "Cancelar", style = AppTypography.labelSection)
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

// ---------------- Previews ----------------

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun Paso2Preview() {
    val inventario = runBlocking { SecadoRepositoryEjemplo().inventario() }
    val verdes = inventario.filter { it.tipoCafe.name == "VERDE" }
    CafesmartTheme {
        SecadoStep2Screen(
            estado = SecadoUiState(
                inventario = inventario,
                elegidos = verdes.map { SubloteElegido(it, it.sublote.pesoDisponibleKg) }
            ),
            onFechaChange = {},
            onIniciar = {},
            onCancelar = {},
            onBack = {}
        )
    }
}
