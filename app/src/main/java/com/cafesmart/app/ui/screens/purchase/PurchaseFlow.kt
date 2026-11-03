package com.cafesmart.app.ui.screens.purchase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    onIrAInventario: () -> Unit,
    modifier: Modifier = Modifier
) {
    // collectAsState observa el StateFlow: cada cambio del estado redibuja la pantalla.
    val estado by viewModel.uiState.collectAsState()

    // Decide que pantalla mostrar segun el estado.
    when {
        estado.compraRegistrada -> PurchaseSuccessScreen(
            estado = estado,
            onRegistrarNueva = { viewModel.reiniciarCompra() },
            // Primero limpia la compra y luego avisa para navegar a inventario.
            onIrAInventario = {
                viewModel.reiniciarCompra()
                onIrAInventario()
            },
            modifier = modifier
        )

        estado.paso == 1 -> PurchaseStep1Screen(
            estado = estado,
            onModoChange = { viewModel.elegirModo(it) },
            onBusquedaChange = { viewModel.cambiarBusqueda(it) },
            onProductorClick = { viewModel.elegirProductor(it) },
            onBusquedaListaChange = { viewModel.cambiarBusquedaLista(it) },
            onEditarProductor = { viewModel.editarProductor(it) },
            onTipoEdicionChange = { viewModel.cambiarTipoEdicion(it) },
            onDocumentoEdicionChange = { viewModel.cambiarDocumentoEdicion(it) },
            onNombreEdicionChange = { viewModel.cambiarNombreEdicion(it) },
            onTelefonoEdicionChange = { viewModel.cambiarTelefonoEdicion(it) },
            onGuardarEdicion = { viewModel.guardarEdicion() },
            onCancelarEdicion = { viewModel.cancelarEdicion() },
            onCerrarLista = { viewModel.cerrarLista() },
            onGuardarNuevo = { viewModel.guardarNuevoProductor() },
            onCancelarNuevo = { viewModel.cancelarNuevoProductor() },
            onTipoDocumentoChange = { viewModel.cambiarTipoDocumento(it) },
            onDocumentoChange = { viewModel.cambiarDocumento(it) },
            onNombreChange = { viewModel.cambiarNombre(it) },
            onTelefonoChange = { viewModel.cambiarTelefono(it) },
            onContinuar = { viewModel.continuarPaso1() },
            // La flecha del paso 1 sale del registro de compra.
            onBack = onSalir,
            modifier = modifier
        )

        estado.paso == 2 -> PurchaseStep2Screen(
            estado = estado,
            onTipoCafeChange = { indice, tipo -> viewModel.cambiarTipoCafe(indice, tipo) },
            onCalidadChange = { indice, calidad -> viewModel.cambiarCalidad(indice, calidad) },
            onPesoChange = { indice, texto -> viewModel.cambiarPeso(indice, texto) },
            onPrecioChange = { indice, texto -> viewModel.cambiarPrecio(indice, texto) },
            onQuitarCafe = { viewModel.quitarCafe(it) },
            onAbrirCafe = { viewModel.abrirCafe(it) },
            onAgregarCafe = { viewModel.agregarCafe() },
            onFechaChange = { viewModel.cambiarFecha(it) },
            onContinuar = { viewModel.continuarPaso2() },
            onBack = { viewModel.volverAlPaso1() },
            modifier = modifier
        )

        else -> PurchaseStep3Screen(
            estado = estado,
            // "Cambiar" productor vuelve al paso 1 sin perder los datos.
            onCambiarProductor = { viewModel.volverAlPaso1() },
            onFechaChange = { viewModel.cambiarFecha(it) },
            // "Editar" vuelve al paso 2 con los cafes ya escritos.
            onEditarCafe = { viewModel.editarCafe(it) },
            onConfirmar = { viewModel.confirmarCompra() },
            // "Sí, cancelar" borra la compra y sale del registro.
            onCancelarCompra = {
                viewModel.reiniciarCompra()
                onSalir()
            },
            onBack = { viewModel.volverAlPaso2() },
            modifier = modifier
        )
    }
}
