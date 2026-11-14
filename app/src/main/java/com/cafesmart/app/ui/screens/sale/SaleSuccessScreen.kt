package com.cafesmart.app.ui.screens.sale

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.data.sale.SubloteElegido
import com.cafesmart.app.ui.screens.purchase.PantallaRegistroExitoso
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.sale.SaleUiState

// Venta registrada: circulo verde con chulito, resumen y los botones "Registrar nueva venta" / "Ir a inventario".
// Vista (MVVM): no tiene logica ni estado propio; la pantalla es la misma que usa "Compra registrada".
@Composable
fun SaleSuccessScreen(
    estado: SaleUiState,
    onRegistrarNueva: () -> Unit,
    onIrAInventario: () -> Unit,
    modifier: Modifier = Modifier
) {
    PantallaRegistroExitoso(
        titulo = "Venta registrada",
        subtitulo = "La venta se guardó correctamente.",
        tituloResumen = "Resumen de venta",
        filas = listOf(
            "Cliente" to estado.clienteTexto,
            "Fecha" to estado.fechaResumenTexto,
            "Café" to estado.cafeResumenTexto,
            "Total kg" to estado.totalKgTexto
        ),
        etiquetaTotal = "Total recibido",
        total = estado.totalARecibirTexto,
        textoNueva = "Registrar nueva venta",
        onRegistrarNueva = onRegistrarNueva,
        onIrAInventario = onIrAInventario,
        modifier = modifier
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
// Preview con una venta sin registrar de 150 kg a $ 20.000.
private fun SaleSuccessPreview() {
    val base = SaleUiState()
    val sublote5 = base.inventario.first { it.sublote.id == 5L }
    CafesmartTheme {
        SaleSuccessScreen(
            estado = base.copy(
                modo = ModoCliente.SIN_REGISTRAR,
                elegidos = listOf(SubloteElegido(sublote5, 150.0)),
                precios = mapOf(Calidad.BUENA to "20000"),
                ventaRegistrada = true
            ),
            onRegistrarNueva = {},
            onIrAInventario = {}
        )
    }
}
