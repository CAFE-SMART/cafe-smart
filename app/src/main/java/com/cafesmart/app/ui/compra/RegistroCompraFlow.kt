package com.cafesmart.app.ui.compra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafesmart.app.data.Fecha
import com.cafesmart.app.data.Productor
import com.cafesmart.app.data.Sublote
import com.cafesmart.app.viewmodel.RegistroCompraViewModel

// Conecta el ViewModel con las pantallas (MVVM):
// - el estado llega del ViewModel (uiState) y se le pasa a la pantalla del paso actual;
// - cada accion de la pantalla llama a una funcion del ViewModel.
// Las pantallas no guardan estado: igual que MainScreen(onThemeToggle) del ejemplo de clase.
@Composable
fun RegistroCompraFlow(
    onSalir: () -> Unit,
    // Entrega productor, cafes y fecha al paso 3 (Finalizar registro), que se hace en otra rama
    onFinalizar: (Productor, List<Sublote>, Fecha) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegistroCompraViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()

    if (estado.paso == 1) {
        RegistroCompraPaso1Screen(
            modo = estado.modo,
            busqueda = estado.busqueda,
            productores = estado.productoresEnPantalla,
            hayMasProductores = estado.hayMasProductores,
            productorElegido = estado.productorElegido,
            nuevoProductor = estado.nuevoProductor,
            documentoError = estado.documentoError,
            nombreError = estado.nombreError,
            telefonoError = estado.telefonoError,
            continuarHabilitado = estado.continuarPaso1Habilitado,
            mensajePendiente = estado.mensajePendientePaso1,
            onModoChange = viewModel::elegirModo,
            onBusquedaChange = viewModel::cambiarBusqueda,
            onProductorClick = viewModel::elegirProductor,
            onVerMas = viewModel::verMasProductores,
            onNuevoProductorChange = viewModel::cambiarNuevoProductor,
            onContinuar = viewModel::continuarPaso1,
            onBack = onSalir,
            modifier = modifier
        )
    } else {
        RegistroCompraPaso2Screen(
            cafes = estado.cafes,
            erroresPeso = estado.erroresPeso,
            erroresPrecio = estado.erroresPrecio,
            fecha = estado.fecha,
            totalKg = estado.totalKg,
            totalEstimado = estado.totalEstimado,
            agregarHabilitado = estado.cafesListos,
            mensajeAgregar = estado.mensajeAgregarCafe,
            continuarHabilitado = estado.cafesListos,
            mensajePendiente = estado.mensajePendientePaso2,
            onCafeChange = viewModel::cambiarCafe,
            onQuitarCafe = viewModel::quitarCafe,
            onAgregarCafe = viewModel::agregarCafe,
            onFechaChange = viewModel::cambiarFecha,
            onContinuar = {
                val productor = estado.productorDeLaCompra
                if (productor != null) {
                    onFinalizar(productor, estado.sublotes, estado.fecha)
                }
            },
            onBack = viewModel::volverAlPaso1,
            modifier = modifier
        )
    }
}
