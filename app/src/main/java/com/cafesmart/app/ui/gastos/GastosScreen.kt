package com.cafesmart.app.ui.gastos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.ui.components.*
import com.cafesmart.app.ui.theme.*
import com.cafesmart.app.viewmodel.gastos.*

/**
 * Adaptador MVVM y de navegación de Gastos; sustituye al archivo monolítico original.
 * El ViewModel pertenece a HOME: las tres rutas comparten sesión y se elimina al cerrar
 * sesión. No modifica el estado ni la persistencia de otros módulos.
 * [registro] distingue las dos pantallas y [asociar] abre la variante de sublotes.
 */
@Composable
fun GastosRoute(navController: NavHostController, registro: Boolean, asociar: Boolean = false) {
    val propietario = remember(navController) { navController.getBackStackEntry(Routes.HOME) }
    val modelo: GastosViewModel = viewModel(viewModelStoreOwner = propietario)
    val estado by modelo.uiState.collectAsState()
    // MainActivity ya aplica el padding del Scaffold. Consumir solo barras evita duplicarlo
    // en estas rutas y conserva los insets del teclado para el formulario desplazable.
    val contenido = Modifier.consumeWindowInsets(WindowInsets.systemBars)
    LaunchedEffect(asociar) {
        if (asociar) modelo.editar { it.copy(aplicaA = AplicaGasto.SUBLOTES) }
    }
    // El resultado exitoso se muestra sobre el listado ya actualizado, como en Figma.
    LaunchedEffect(estado.resultado, registro) {
        if (registro && estado.resultado == ResultadoGasto.EXITO) {
            if (!navController.popBackStack(Routes.EXPENSES, false)) {
                navController.navigate(Routes.EXPENSES) { popUpTo(Routes.HOME); launchSingleTop = true }
            }
        }
    }
    val salir = { modelo.nuevoRegistro(); navController.popBackStack(); Unit }
    val listado = {
        modelo.nuevoRegistro()
        if (registro && !navController.popBackStack(Routes.EXPENSES, false)) {
            navController.navigate(Routes.EXPENSES) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
        Unit
    }
    if (registro) {
        BackHandler { if (estado.resultado == ResultadoGasto.EDICION) salir() else modelo.volverAEditar() }
        RegistrarGastoScreen(estado, modelo::editar, modelo::solicitarConfirmacion, salir, modifier = contenido)
    } else {
        ListaGastosScreen(estado, { navController.popBackStack() }, {
            modelo.nuevoRegistro()
            navController.navigate(Routes.EXPENSE_NEW) { launchSingleTop = true }
        }, modelo::filtrar, modifier = contenido, onActualizar = modelo::actualizar)
    }
    GastosResultado(estado, modelo::confirmar, modelo::volverAEditar, listado, {
        modelo.nuevoRegistro()
        if (!registro) navController.navigate(Routes.EXPENSE_NEW) { launchSingleTop = true }
    })
}

/** Hojas de confirmación/resultado del equipo. Durante el guardado no se permite cerrar ni reenviar. */
@Composable
fun GastosResultado(estado: GastosUiState, onConfirmar: () -> Unit, onEditar: () -> Unit,
    onVerGastos: () -> Unit, onOtro: () -> Unit) {
    when (estado.resultado) {
        ResultadoGasto.CONFIRMACION -> ConfirmSheet(
            icon = AppIcons.Warning, iconBackground = WarningBg, iconTint = WarningText,
            title = "¿Registrar este gasto?",
            message = "${estado.formulario.concepto.trim()} · ${dineroGasto(GastosValidation.pesos(estado.formulario.monto) ?: 0L)}\nSe guardará temporalmente en esta demostración.",
            actionText = "Registrar gasto", onAction = onConfirmar, linkText = "Regresar", onLink = onEditar)
        ResultadoGasto.GUARDANDO -> Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Surface(shape = AppShapes.medium) { Box(Modifier.padding(32.dp)) { CircularProgressIndicator() } }
        }
        ResultadoGasto.EXITO -> ConfirmSheet(
            icon = AppIcons.Check, iconBackground = SuccessBg, iconTint = SuccessIcon,
            title = "Gasto registrado con éxito", message = "El gasto se agregó a la lista temporal. No se guardó en una base de datos.",
            actionText = "Ver gastos", onAction = onVerGastos, linkText = "Registrar otro gasto", onLink = onOtro, onDismiss = onVerGastos)
        ResultadoGasto.ERROR -> ConfirmSheet(
            icon = AppIcons.Warning, iconBackground = ErrorBg, iconTint = ErrorIcon,
            title = "Error al registrar", message = "No se pudo guardar el gasto. Inténtalo de nuevo.",
            actionText = "Reintentar", onAction = onConfirmar, linkText = "Regresar", onLink = onEditar)
        ResultadoGasto.EDICION -> Unit
    }
}
