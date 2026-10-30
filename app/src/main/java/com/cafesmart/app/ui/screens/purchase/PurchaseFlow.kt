package com.cafesmart.app.ui.screens.purchase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cafesmart.app.data.purchase.Fecha
import com.cafesmart.app.data.purchase.Productor
import com.cafesmart.app.data.purchase.Sublote
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel

// Conecta el ViewModel con las pantallas (MVVM), igual que MainActivity + MainScreen
// del ejemplo TrainingCompose del profesor:
// - el estado llega del ViewModel (uiState) y se le pasa a la pantalla del paso actual;
// - cada accion de la pantalla llama a una funcion del ViewModel.
// Las pantallas no guardan estado: igual que MainScreen(onThemeToggle) del ejemplo de clase.
@Composable
fun PurchaseFlow(
    viewModel: PurchaseViewModel,
    onSalir: () -> Unit,
    // Entrega productor, cafes y fecha al paso 3 (Finalizar registro), que se hace en otra rama
    onFinalizar: (Productor, List<Sublote>, Fecha) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()

    if (estado.paso == 1) {
        PurchaseStep1Screen(
            estado = estado,
            onModoChange = { viewModel.elegirModo(it) },
            onBusquedaChange = { viewModel.cambiarBusqueda(it) },
            onProductorClick = { viewModel.elegirProductor(it) },
            onVerMas = { viewModel.verMasProductores() },
            onTipoDocumentoChange = { viewModel.cambiarTipoDocumento(it) },
            onDocumentoChange = { viewModel.cambiarDocumento(it) },
            onNombreChange = { viewModel.cambiarNombre(it) },
            onTelefonoChange = { viewModel.cambiarTelefono(it) },
            onContinuar = { viewModel.continuarPaso1() },
            onBack = onSalir,
            modifier = modifier
        )
    } else {
        PurchaseStep2Screen(
            estado = estado,
            onTipoCafeChange = { indice, tipo -> viewModel.cambiarTipoCafe(indice, tipo) },
            onCalidadChange = { indice, calidad -> viewModel.cambiarCalidad(indice, calidad) },
            onPesoChange = { indice, texto -> viewModel.cambiarPeso(indice, texto) },
            onPrecioChange = { indice, texto -> viewModel.cambiarPrecio(indice, texto) },
            onQuitarCafe = { viewModel.quitarCafe(it) },
            onAgregarCafe = { viewModel.agregarCafe() },
            onFechaChange = { viewModel.cambiarFecha(it) },
            onContinuar = {
                val productor = estado.productorDeLaCompra
                if (productor != null) {
                    onFinalizar(productor, estado.sublotes, estado.fecha)
                }
            },
            onBack = { viewModel.volverAlPaso1() },
            modifier = modifier
        )
    }
}
