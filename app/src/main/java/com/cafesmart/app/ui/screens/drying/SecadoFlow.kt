package com.cafesmart.app.ui.screens.drying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cafesmart.app.viewmodel.drying.SecadoViewModel

// Conectan el SecadoViewModel con cada pantalla de Secado (MVVM), igual que SaleFlow en Ventas:
// - el estado llega del ViewModel (uiState) y se le pasa a la pantalla;
// - cada accion de la pantalla llama a una funcion del ViewModel.
// Cada paso tiene su ruta (Routes.DRYING_STEP1...), por eso hay una funcion por paso.
// Las rutas comparten el mismo SecadoViewModel (ver AppNavHost).

@Composable
fun SecadoStep1Flow(
    viewModel: SecadoViewModel,
    onContinuar: () -> Unit,
    // La flecha del paso 1 sale del registro de secado.
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    // collectAsState observa el StateFlow: cada cambio del estado redibuja la pantalla.
    val estado by viewModel.uiState.collectAsState()

    SecadoStep1Screen(
        estado = estado,
        onAlternarTodos = { viewModel.alternarTodos() },
        onAlternarSublote = { viewModel.alternarSublote(it) },
        onCambiar = { viewModel.abrirAjuste(it) },
        onAjusteChange = { viewModel.escribirAjuste(it) },
        onAjusteConfirmar = { viewModel.confirmarAjuste() },
        onAjusteCancelar = { viewModel.cancelarAjuste() },
        onContinuar = onContinuar,
        onBack = onSalir,
        modifier = modifier
    )
}

@Composable
fun SecadoStep2Flow(
    viewModel: SecadoViewModel,
    onIniciado: () -> Unit,
    // "Cancelar": sale del registro de secado.
    onCancelado: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()

    SecadoStep2Screen(
        estado = estado,
        onFechaChange = { viewModel.cambiarFecha(it) },
        // Solo se pasa a la pantalla final cuando el ViewModel inicio el secado (el callback se llama al terminar).
        onIniciar = { viewModel.iniciarSecado(onIniciado) },
        onCancelar = { viewModel.reiniciar(onCancelado) },
        onBack = onVolver,
        modifier = modifier
    )
}

@Composable
fun SecadoStep3Flow(
    viewModel: SecadoViewModel,
    onVerEnCurso: () -> Unit,
    onIrAInventario: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()

    SecadoStep3Screen(
        estado = estado,
        onVerEnCurso = onVerEnCurso,
        onIrAInventario = onIrAInventario,
        modifier = modifier
    )
}
