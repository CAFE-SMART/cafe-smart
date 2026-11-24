package com.cafesmart.app.ui.screens.drying

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cafesmart.app.viewmodel.drying.FinalizarSecadoViewModel
import com.cafesmart.app.viewmodel.drying.SecadosEnCursoViewModel
import com.cafesmart.app.viewmodel.purchase.milisAFecha

// Conectan los ViewModels de los pasos 4 a 6 con sus pantallas (MVVM), igual que SecadoFlow para los pasos 1 a 3:
// el estado llega del ViewModel y cada accion de la pantalla llama a una funcion del ViewModel.

@Composable
fun SecadoStep4Flow(
    viewModel: SecadosEnCursoViewModel,
    // "Finalizar secado": pasa al paso 5 con el secado elegido.
    onFinalizar: () -> Unit,
    // Se cancelo el ultimo secado en curso: no queda nada que mostrar, se sale del flujo de secado.
    onSinSecados: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()
    // Al entrar (o al volver a esta pantalla) se lee otra vez la lista del repositorio.
    LaunchedEffect(Unit) { viewModel.refrescar() }

    SecadoStep4Screen(
        estado = estado,
        onElegir = { viewModel.elegirSecado(it) },
        onFinalizar = onFinalizar,
        onPedirCancelar = { viewModel.pedirCancelar() },
        onConfirmarCancelar = {
            viewModel.confirmarCancelar()
            if (viewModel.uiState.value.secados.isEmpty()) onSinSecados()
        },
        onDescartarCancelar = { viewModel.descartarCancelar() },
        onBack = onVolver,
        modifier = modifier
    )
}

@Composable
fun SecadoStep5Flow(
    viewModel: FinalizarSecadoViewModel,
    // Solo se usa para mostrar la tarjeta del secado (titulo, sublotes y calidades), que el estado del paso 5 no trae.
    enCurso: SecadosEnCursoViewModel,
    // Se llama al confirmar con exito; huboGastos decide a donde se navega (Si: paso 7, No: paso 6).
    onConfirmado: (huboGastos: Boolean) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()
    val estadoEnCurso by enCurso.uiState.collectAsState()
    val idDelSecado = estado.secado?.secado?.id

    SecadoStep5Screen(
        estado = estado,
        tarjeta = estadoEnCurso.tarjetas.firstOrNull { it.detalle.secado.id == idDelSecado },
        onKilosChange = { calidad, texto -> viewModel.escribirKilos(calidad, texto) },
        onFactorChange = { viewModel.escribirFactor(it) },
        // El calendario entrega milisegundos; el ViewModel recibe una Fecha.
        onFechaChange = { viewModel.cambiarFecha(milisAFecha(it)) },
        onGastosChange = { viewModel.elegirGastos(it) },
        onConfirmar = {
            viewModel.confirmar()
            // Solo se avanza si el ViewModel finalizo el secado.
            val nuevo = viewModel.uiState.value
            if (nuevo.finalizado) onConfirmado(nuevo.huboGastos)
        },
        onBack = onVolver,
        modifier = modifier
    )
}

@Composable
fun SecadoStep6Flow(
    viewModel: FinalizarSecadoViewModel,
    onRegistrarNuevo: () -> Unit,
    onIrAInventario: () -> Unit,
    // Si se llega aqui sin resultado (por ejemplo, la app se recreo), no hay nada que mostrar.
    onSinResultado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.uiState.collectAsState()
    LaunchedEffect(estado.finalizado) { if (!estado.finalizado) onSinResultado() }

    SecadoStep6Screen(
        estado = estado,
        onRegistrarNuevo = onRegistrarNuevo,
        onIrAInventario = onIrAInventario,
        modifier = modifier
    )
}
