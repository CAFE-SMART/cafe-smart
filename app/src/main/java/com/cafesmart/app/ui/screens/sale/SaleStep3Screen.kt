package com.cafesmart.app.ui.screens.sale

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.ui.components.AppErrorDialog
import com.cafesmart.app.ui.components.CancelarSheet
import com.cafesmart.app.ui.components.ContinueButton
import com.cafesmart.app.ui.screens.purchase.PurchaseBottomBar
import com.cafesmart.app.ui.screens.purchase.PurchaseDatePickerDialog
import com.cafesmart.app.ui.screens.purchase.PurchaseHeader
import com.cafesmart.app.ui.screens.purchase.ResumenPago
import com.cafesmart.app.ui.screens.purchase.TarjetaCafeResumen
import com.cafesmart.app.ui.screens.purchase.TarjetaPersonaYFecha
import com.cafesmart.app.ui.theme.AppTypography
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.theme.Spacing
import com.cafesmart.app.ui.theme.SurfaceScreen
import com.cafesmart.app.viewmodel.sale.SaleUiState

// Paso 3 de Ventas: Confirma tu venta.
// Vista (MVVM): no tiene logica. Reutiliza las tarjetas del paso 3 de Compras.
// Solo recuerda algo visual: si el calendario o la hoja de cancelar estan abiertos.
@Composable
fun SaleStep3Screen(
    estado: SaleUiState,
    onCambiarCliente: () -> Unit,
    onFechaChange: (Long) -> Unit,
    onEditarCafe: () -> Unit,
    onConfirmar: () -> Unit,
    // "Regresar" en el aviso de error: lo cierra sin registrar nada.
    onDescartarError: () -> Unit,
    onCancelarVenta: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var mostrarCancelar by rememberSaveable { mutableStateOf(false) }

    // Mientras se guarda la venta, el "atras" del sistema no sale de la pantalla.
    BackHandler(enabled = estado.guardando) {}

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
            // La flecha no responde mientras se guarda la venta.
            PurchaseHeader(
                paso = 3,
                pregunta = "Confirma tu venta",
                onBack = { if (!estado.guardando) onBack() },
                titulo = "Registro de venta"
            )

            Column(
                modifier = Modifier.padding(horizontal = Spacing.s20, vertical = Spacing.s16),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12)
            ) {
                TarjetaPersonaYFecha(
                    etiquetaPersona = "Cliente",
                    persona = estado.clienteTexto,
                    detallePersona = estado.clienteDocumentoTexto,
                    fecha = estado.fechaResumenTexto,
                    errorFecha = estado.errorDeFecha,
                    // "Cambiar" cliente vuelve al paso 1 sin perder los datos.
                    onCambiarPersona = onCambiarCliente,
                    // "Cambiar" fecha abre el calendario.
                    onCambiarFecha = { mostrarCalendario = true }
                )

                Text(
                    text = "Café que vendes",
                    style = AppTypography.labelSection,
                    modifier = Modifier.padding(top = Spacing.s4)
                )

                // Una tarjeta por cada sublote de la venta; "Editar" vuelve al paso 2.
                estado.cafesEnResumen.forEach { cafe ->
                    TarjetaCafeResumen(cafe = cafe, onEditar = onEditarCafe)
                }

                ResumenPago(
                    totalKg = estado.totalKgTexto,
                    totalPagar = estado.totalARecibirTexto,
                    etiquetaTotal = "Total a recibir"
                )
            }
        }

        PurchaseBottomBar {
            // Solo se activa cuando el ViewModel dice que la venta esta completa y la fecha sirve.
            // Mientras se guarda queda bloqueado (con la ruedita) para no guardar dos veces.
            ContinueButton(
                text = "Confirmar venta",
                enabled = estado.puedeConfirmar && !estado.guardando,
                cargando = estado.guardando,
                onClick = onConfirmar
            )
            TextButton(
                // "Cancelar" no borra nada todavia: primero pregunta con la hoja.
                onClick = { mostrarCancelar = true },
                // No se puede cancelar mientras se guarda la venta.
                enabled = !estado.guardando,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
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

    // Si no se pudo guardar: "Reintentar" vuelve a confirmar y "Regresar" (o la x) cierra el aviso.
    estado.errorAlGuardar?.let {
        AppErrorDialog(
            title = "No se pudo registrar",
            message = it,
            onRetry = onConfirmar,
            onBack = onDescartarError
        )
    }

    if (mostrarCancelar) {
        CancelarSheet(
            titulo = "¿Cancelar venta?",
            onCancelar = onCancelarVenta,
            // "Seguir editando" solo cierra la hoja.
            onSeguir = { mostrarCancelar = false }
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
// Preview con la venta del Figma: 150 kg del sublote 5 a $ 20.000.
private fun SaleStep3Preview() {
    val base = SaleUiState()
    val sublote5 = base.inventario.first { it.sublote.id == 5L }
    CafesmartTheme {
        SaleStep3Screen(
            estado = base.copy(
                modo = ModoCliente.REGISTRADO,
                cliente = Cliente(nombre = "Exportadora del Sur S.A.", documento = "9003421215"),
                elegidos = listOf(SubloteElegido(sublote5, 150.0)),
                precios = mapOf(Calidad.BUENA to "20000")
            ),
            onCambiarCliente = {},
            onFechaChange = {},
            onEditarCafe = {},
            onConfirmar = {},
            onDescartarError = {},
            onCancelarVenta = {},
            onBack = {}
        )
    }
}
