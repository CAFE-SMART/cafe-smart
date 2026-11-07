package com.cafesmart.app.ui.gastos

// ============================================================
// 1. IMPORTACIONES POR RESPONSABILIDAD
// ============================================================
// Interacción con el botón Atrás del sistema.
import androidx.activity.compose.BackHandler
// Layouts, distribución del espacio y listas desplazables.
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBars
// Controles de Material 3: tarjetas, textos, botones o superficies.
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
// Composable, estados observables y efectos del ciclo de composición.
import androidx.compose.runtime.*
// Modificadores, alineación, dibujo, tipografía, unidades y accesibilidad.
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// ViewModel y corrutinas ligadas a su ciclo de vida.
import androidx.lifecycle.viewmodel.compose.viewModel
// Rutas y navegación de la aplicación.
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.cafesmart.app.navigation.Routes
// Modelos oficiales que transportan los datos del módulo.
import com.cafesmart.app.data.model.AplicaGasto
// Componentes reutilizables del proyecto.
import com.cafesmart.app.ui.components.*
// Colores, formas, tipografía y espaciados del tema compartido.
import com.cafesmart.app.ui.theme.*
// Estado, eventos y lógica de presentación del ViewModel.
import com.cafesmart.app.viewmodel.gastos.*

// ============================================================
// 2. CONEXIÓN ENTRE NAVEGACIÓN Y VIEWMODEL
// ============================================================
/**
 * Adaptador MVVM y de navegación de Gastos; sustituye al archivo monolítico original.
 * El ViewModel pertenece a HOME: las tres rutas comparten sesión y se elimina al cerrar
 * sesión. No modifica el estado ni la persistencia de otros módulos.
 * [registro] distingue lista y formulario; [asociar] abre la selección múltiple de sublotes.
 *
 * Esta función une MVVM y Navigation Compose: recibe estado del ViewModel y entrega
 * eventos a las pantallas. Una recomposición vuelve a describir la interfaz, sin guardar por sí misma.
 *
 * @param navController Controlador de la pila de rutas que pertenece al contenedor de la aplicación.
 * @param backStackEntry Entrada del destino actual, recibida desde composable en AppNavHost.
 * @param registro Si es true, muestra RegistrarGastoScreen; si es false, muestra ListaGastosScreen.
 * @param asociar Muestra el selector que conserva el borrador compartido al regresar.
 */
@Composable
fun GastosRoute(navController: NavHostController, backStackEntry: NavBackStackEntry,
    registro: Boolean, asociar: Boolean = false) {
    // La entrada del destino es la clave de remember. Una entrada nueva vuelve a buscar HOME,
    // por ejemplo, tras cerrar sesión e ingresar otra vez con el mismo NavController.
    // HOME sigue siendo el propietario: Lista y Registro comparten su ViewModel durante la sesión.
    val propietario = remember(backStackEntry) { navController.getBackStackEntry(Routes.HOME) }
    val modelo: GastosViewModel = viewModel(viewModelStoreOwner = propietario)
    // collectAsState convierte StateFlow en un estado que Compose observa. by permite leer
    // el valor directamente; sus cambios actualizan las partes de la interfaz que lo utilizan.
    val estado by modelo.uiState.collectAsState()
    // MainActivity ya aplica el padding del Scaffold. Consumir solo barras evita duplicarlo
    // en estas rutas y conserva los insets del teclado para el formulario desplazable.
    val contenido = Modifier.consumeWindowInsets(WindowInsets.systemBars)
    // LaunchedEffect ejecuta esta acción al entrar o cambiar su clave, no en cada dibujo de pantalla.
    LaunchedEffect(asociar) {
        if (asociar) modelo.editar { it.copy(aplicaA = AplicaGasto.SUBLOTES) }
    }
    if (asociar) {
        val volverAlFormulario = {
            if (navController.previousBackStackEntry?.destination?.route == Routes.EXPENSE_NEW) {
                navController.popBackStack()
            } else {
                // También admite el acceso desde el detalle de inventario.
                navController.navigate(Routes.EXPENSE_NEW) {
                    popUpTo(Routes.EXPENSE_LINK_BATCHES) { inclusive = true }
                    launchSingleTop = true
                }
            }
            Unit
        }
        BackHandler(onBack = volverAlFormulario)
        AsociarSublotesScreen(estado, modelo::alternarSublote, volverAlFormulario, contenido)
        return
    }
    // El resultado exitoso se muestra sobre el listado ya actualizado, como en Figma.
    LaunchedEffect(estado.resultado, registro) {
        if (registro && estado.resultado == ResultadoGasto.EXITO) {
            if (!navController.popBackStack(Routes.EXPENSES, false)) {
                navController.navigate(Routes.EXPENSES) { popUpTo(Routes.HOME); launchSingleTop = true }
            }
        }
    }
    // Estas lambdas son callbacks. nuevoRegistro limpia el borrador, no el listado guardado en memoria.
    // Unit expresa que la acción no devuelve un resultado al botón.
    val salir = { navController.popBackStack(); Unit }
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
    // BackHandler coordina Atrás. Durante GUARDANDO el ViewModel impide volver a editar.
    // Las referencias modelo::editar y similares pasan funciones existentes como callbacks.
    if (registro) {
        BackHandler { if (estado.resultado == ResultadoGasto.EDICION) salir() else modelo.volverAEditar() }
        RegistrarGastoScreen(estado, modelo::editar, modelo::solicitarConfirmacion, salir,
            modifier = contenido, onSeleccionarSublotes = {
                navController.navigate(Routes.EXPENSE_LINK_BATCHES) { launchSingleTop = true }
            })
    } else {
        ListaGastosScreen(estado, { navController.popBackStack() }, {
            navController.navigate(Routes.EXPENSE_NEW) { launchSingleTop = true }
        }, modelo::filtrar, modifier = contenido, onActualizar = modelo::actualizar,
            onCargarEjemplos = modelo::cargarEjemplos)
    }
    GastosResultado(estado, modelo::confirmar, modelo::volverAEditar, listado) {
        modelo.nuevoRegistro()
        if (!registro) navController.navigate(Routes.EXPENSE_NEW) { launchSingleTop = true }
    }
}

// ============================================================
// 3. CONFIRMACIÓN, GUARDADO Y RESULTADO
// ============================================================
/**
 * Hojas de confirmación/resultado del equipo. Durante el guardado no se permite cerrar ni reenviar.
 *
 * La expresión when elige la hoja a partir de ResultadoGasto. ConfirmSheet reutiliza el patrón del proyecto;
 * el indicador circular solo informa que se está guardando, no representa un porcentaje real.
 *
 * @param estado Estado actual del formulario y de la operación.
 * @param onConfirmar Solicita guardar o reintentar en el ViewModel.
 * @param onEditar Vuelve al borrador sin borrar sus campos.
 * @param onVerGastos Permite consultar el listado.
 * @param onOtro Inicia un borrador nuevo para otro gasto.
 */
@Composable
fun GastosResultado(estado: GastosUiState, onConfirmar: () -> Unit, onEditar: () -> Unit,
    onVerGastos: () -> Unit, onOtro: () -> Unit) {
    // CONFIRMACION pide permiso al usuario para guardar; GUARDANDO bloquea el cierre.
    // EXITO solo se muestra cuando el repositorio termina y ERROR permite conservar datos y reintentar.
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
