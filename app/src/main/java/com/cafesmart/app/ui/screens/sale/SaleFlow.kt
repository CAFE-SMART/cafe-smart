package com.cafesmart.app.ui.screens.sale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cafesmart.app.viewmodel.sale.SaleViewModel

// Conectan el SaleViewModel con cada pantalla de Ventas (MVVM), igual que PurchaseFlow en Compras:
// - el estado llega del ViewModel (uiState) y se le pasa a la pantalla;
// - cada accion de la pantalla llama a una funcion del ViewModel.
// En Compras todos los pasos son una sola ruta; en Ventas cada paso tiene la suya (Routes.SALE_STEP1...),
// por eso hay una funcion por paso. Las rutas comparten el mismo SaleViewModel (ver AppNavHost).

@Composable
fun SaleStep1Flow(
    viewModel: SaleViewModel,
    // Se avisa despues de que el ViewModel acepto el paso 1: la navegacion pasa al paso 2.
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    // collectAsState observa el StateFlow: cada cambio del estado redibuja la pantalla.
    val estado by viewModel.uiState.collectAsState()

    SaleStep1Screen(
        estado = estado,
        onModoChange = { viewModel.elegirModo(it) },
        onBusquedaChange = { viewModel.cambiarBusqueda(it) },
        onClienteClick = { viewModel.elegirCliente(it) },
        onBusquedaListaChange = { viewModel.cambiarBusquedaLista(it) },
        onCerrarLista = { viewModel.cerrarLista() },
        onEditarCliente = { viewModel.editarCliente(it) },
        onTipoEdicionChange = { viewModel.cambiarTipoEdicion(it) },
        onDocumentoEdicionChange = { viewModel.cambiarDocumentoEdicion(it) },
        onNombreEdicionChange = { viewModel.cambiarNombreEdicion(it) },
        onTelefonoEdicionChange = { viewModel.cambiarTelefonoEdicion(it) },
        onGuardarEdicion = { viewModel.guardarEdicion() },
        onCancelarEdicion = { viewModel.cancelarEdicion() },
        onTipoDocumentoChange = { viewModel.cambiarTipoDocumento(it) },
        onDocumentoChange = { viewModel.cambiarDocumento(it) },
        onNombreChange = { viewModel.cambiarNombre(it) },
        onTelefonoChange = { viewModel.cambiarTelefono(it) },
        // Con "Cliente nuevo", continuarPaso1 lo registra; luego se navega al paso 2.
        onContinuar = {
            viewModel.continuarPaso1()
            onContinuar()
        },
        // La flecha del paso 1 sale del registro de venta.
        onBack = onSalir,
        modifier = modifier
    )
}

@Composable
fun SaleStep2Flow(
    viewModel: SaleViewModel,
    onContinuar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()

    SaleStep2Screen(
        estado = estado,
        onTipoCafeChange = { viewModel.elegirTipoCafe(it) },
        onAlternarTodos = { viewModel.alternarTodos() },
        onAlternarSublote = { viewModel.alternarSublote(it) },
        onCantidadChange = { subloteId, texto -> viewModel.escribirCantidad(subloteId, texto) },
        onPrecioChange = { calidad, texto -> viewModel.cambiarPrecio(calidad, texto) },
        onContinuar = onContinuar,
        onBack = onVolver,
        modifier = modifier
    )
}
