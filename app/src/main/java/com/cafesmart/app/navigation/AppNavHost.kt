package com.cafesmart.app.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cafesmart.app.ui.auth.AuthAuxDialog
import com.cafesmart.app.ui.auth.AuthDialogs
import com.cafesmart.app.ui.auth.AuthDialogsState
import com.cafesmart.app.ui.auth.AuthHelpContext
import com.cafesmart.app.ui.auth.AuthPage
import com.cafesmart.app.ui.auth.AuthViewModel
import com.cafesmart.app.ui.auth.ConfirmDestructiveDialog
import com.cafesmart.app.ui.auth.LoginScreen
import com.cafesmart.app.ui.auth.MODO_DEMOSTRACION
import com.cafesmart.app.ui.auth.RegisterStep1Screen
import com.cafesmart.app.ui.auth.RegisterStep2Screen
import com.cafesmart.app.ui.auth.RegistrationData
import com.cafesmart.app.ui.auth.RegistrationFailure
import com.cafesmart.app.ui.auth.RegistrationSuccessScreen
import com.cafesmart.app.ui.auth.SIMULAR_ERROR_REGISTRO
import com.cafesmart.app.ui.home.HomeScreen
import com.cafesmart.app.ui.gastos.GastosRoute
import com.cafesmart.app.ui.screens.PlaceholderAction
import com.cafesmart.app.ui.screens.PlaceholderScreen
import com.cafesmart.app.ui.screens.SplashScreen
import com.cafesmart.app.ui.screens.purchase.PurchaseFlow
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.ui.inventory.InventoryScreen
import com.cafesmart.app.ui.settings.SettingsScreen
import com.cafesmart.app.viewmodel.inventory.InventoryViewModel
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import com.cafesmart.app.viewmodel.settings.SettingsViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    // Un solo AuthViewModel compartido por login y registro (paso 1 y 2).
    val authViewModel: AuthViewModel = viewModel()
    val dialogs = remember { AuthDialogsState() }

    var registrationSuccess by remember { mutableStateOf<RegistrationData?>(null) }
    var pendingRegistration by remember { mutableStateOf<RegistrationData?>(null) }
    var simulateErrorNext by remember { mutableStateOf(SIMULAR_ERROR_REGISTRO) }

    // Procesa el registro en modo demostración (sin backend).
    lateinit var processRegistration: (RegistrationData) -> Unit
    processRegistration = { data ->
        pendingRegistration = data
        when {
            !MODO_DEMOSTRACION -> dialogs.registrationError = RegistrationFailure(
                title = "Registro no disponible",
                message = "El servicio de creación de cuentas todavía no está integrado. " +
                        "No se ha creado ningún usuario."
            )

            simulateErrorNext -> {
                simulateErrorNext = false
                dialogs.registrationError = RegistrationFailure(
                    title = "¡Ups! Hubo un problema de conexión",
                    message = "No pudimos completar el registro de prueba. Inténtalo nuevamente."
                )
            }

            else -> {
                dialogs.registrationError = null
                registrationSuccess = data
                navController.navigate(Routes.REGISTER_SUCCESS) { launchSingleTop = true }
            }
        }
    }

    // Cierra sesión: limpia el estado de auth y deja solo LOGIN en la pila.
    val cerrarSesion: () -> Unit = {
        authViewModel.reset()
        registrationSuccess = null
        pendingRegistration = null
        dialogs.clear()
        navController.logout()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        // ── Cuenta ──────────────────────────────────────────
        composable(Routes.SPLASH) {
            // Al terminar el splash pasa al login y lo saca de la pila.
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            var showExitConfirmation by remember { mutableStateOf(false) }

            LoginScreen(
                state = authViewModel.uiState,
                onChange = authViewModel::update,
                onSubmit = {
                    authViewModel.submitLogin { _ ->
                        if (MODO_DEMOSTRACION) {
                            // Limpia la contraseña temporal; al entrar, el login sale de la pila.
                            authViewModel.reset()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            dialogs.info = "El inicio de sesión real está pendiente de integración."
                        }
                    }
                },
                onRegister = {
                    authViewModel.goToRegister()
                    navController.navigate(Routes.REGISTER_STEP1) { launchSingleTop = true }
                },
                onGoogle = {
                    dialogs.info = "El inicio de sesión con Google está pendiente de integración."
                },
                onForgotPassword = { dialogs.active = AuthAuxDialog.RECOVERY },
                onHelp = { dialogs.help = AuthHelpContext.LOGIN },
                onSupport = { dialogs.active = AuthAuxDialog.SUPPORT },
                onExit = { showExitConfirmation = true }
            )

            if (showExitConfirmation) {
                ConfirmDestructiveDialog(
                    title = "Salir de Café Smart",
                    message = "¿Deseas cerrar la aplicación?",
                    onConfirm = {
                        showExitConfirmation = false
                        (context as? Activity)?.finish()
                    },
                    onDismiss = { showExitConfirmation = false }
                )
            }
        }
        composable(Routes.REGISTER_STEP1) {
            // "Atrás" del sistema también retrocede el estado del ViewModel.
            BackHandler {
                authViewModel.back()
                navController.popBackStack()
            }
            RegisterStep1Screen(
                state = authViewModel.uiState,
                onChange = authViewModel::update,
                onBack = {
                    authViewModel.back()
                    navController.popBackStack()
                },
                onContinue = {
                    // nextStep valida; solo avanza si el formulario es correcto.
                    authViewModel.nextStep()
                    if (authViewModel.uiState.page == AuthPage.REGISTER_STEP_2) {
                        navController.navigate(Routes.REGISTER_STEP2) { launchSingleTop = true }
                    }
                },
                onHelp = { dialogs.help = AuthHelpContext.REGISTER_STEP_1 },
                onSupport = { dialogs.active = AuthAuxDialog.SUPPORT }
            )
        }
        composable(Routes.REGISTER_STEP2) {
            BackHandler {
                authViewModel.back()
                navController.popBackStack()
            }
            RegisterStep2Screen(
                state = authViewModel.uiState,
                onChange = authViewModel::update,
                onBack = {
                    authViewModel.back()
                    navController.popBackStack()
                },
                onCreateAccount = { authViewModel.submitRegistration(processRegistration) },
                onGoogle = {
                    dialogs.info = "El inicio de sesión con Google está pendiente de integración."
                },
                onHelp = { dialogs.help = AuthHelpContext.REGISTER_STEP_2 },
                onSupport = { dialogs.active = AuthAuxDialog.SUPPORT }
            )
        }
        composable(Routes.REGISTER_SUCCESS) {
            registrationSuccess?.let { data ->
                RegistrationSuccessScreen(
                    registration = data,
                    isDemo = MODO_DEMOSTRACION,
                    onGoHome = {
                        registrationSuccess = null
                        pendingRegistration = null
                        authViewModel.reset()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    // Vuelve al paso 2 conservando los campos.
                    onBackToRegister = {
                        registrationSuccess = null
                        navController.popBackStack()
                    }
                )
            }
        }

        // ── Inicio ──────────────────────────────────────────
        composable(Routes.HOME) {
            var showLogoutConfirmation by remember { mutableStateOf(false) }

            HomeScreen(
                onSalir = { showLogoutConfirmation = true },
                onRegistrarGasto = { navController.navigateTo(Routes.EXPENSE_NEW) },
                onVerSublotes = { navController.navigateTo(Routes.INVENTORY_LIST) },
                // La barra inferior manda el nombre de la pestaña.
                onNavegar = { destino ->
                    when (destino) {
                        "compras" -> navController.navigateTo(Routes.PURCHASE_STEP1)
                        "inventario" -> navController.navigateTo(Routes.INVENTORY)
                        "ventas" -> navController.navigateTo(Routes.SALE_STEP1)
                        "ajustes" -> navController.navigateTo(Routes.SETTINGS)
                    }
                },
                onActualizar = { /* TODO ciclo 2 */ },
                onAjustarBodega = { /* TODO ciclo 2 */ },
                onAbrirSublote = { /* TODO ciclo 2 */ }
            )

            if (showLogoutConfirmation) {
                ConfirmDestructiveDialog(
                    title = "Cerrar sesión",
                    message = "¿Deseas salir de tu cuenta y volver al inicio de sesión?",
                    onConfirm = {
                        showLogoutConfirmation = false
                        cerrarSesion()
                    },
                    onDismiss = { showLogoutConfirmation = false }
                )
            }
        }

        // ── Inventario ──────────────────────────────────────
        composable(Routes.INVENTORY) {
            // viewModel() ligado a esta entrada del NavHost (por ahora lee datos de ejemplo; luego, Room).
            val inventoryViewModel: InventoryViewModel = viewModel()
            val inventoryState by inventoryViewModel.uiState.collectAsState()

            InventoryScreen(
                estado = inventoryState,
                // Figma: cada tipo abre su lista por calidad. Esa pantalla aún es placeholder.
                onTipoClick = { tipo -> navController.navigateTo(Routes.inventoryType(tipo.name)) },
                onOrdenChange = inventoryViewModel::cambiarOrden,
                onAjustarBodega = { /* TODO ciclo 2: capacidad de bodega en Ajustes (igual que en Inicio) */ },
                onRegistrarCompra = { navController.navigateTo(Routes.PURCHASE_STEP1) },
                // La barra inferior manda el nombre de la pestaña.
                onNavegar = { destino ->
                    when (destino) {
                        "inicio" -> navController.popBackStack(Routes.HOME, inclusive = false)
                        "compras" -> navController.navigateTo(Routes.PURCHASE_STEP1)
                        "ventas" -> navController.navigateTo(Routes.SALE_STEP1)
                        "ajustes" -> navController.navigateTo(Routes.SETTINGS)
                    }
                }
            )
        }
        composable(
            route = Routes.INVENTORY_TYPE,
            arguments = listOf(navArgument(Routes.ARG_TIPO) { type = NavType.StringType })
        ) { entry ->
            val nombre = entry.arguments?.getString(Routes.ARG_TIPO).orEmpty()
            val tipo = TipoCafe.entries.firstOrNull { it.name == nombre } ?: TipoCafe.VERDE
            NavPlaceholder(
                navController,
                "Inventario · Café ${tipo.etiqueta.lowercase()} por calidad", Routes.inventoryType(tipo.name), true,
                *listOfNotNull(
                    "Ver sublotes de una calidad" to Routes.INVENTORY_LIST,
                    // Solo el café verde se manda a secar.
                    if (tipo == TipoCafe.VERDE) "Iniciar secado" to Routes.DRYING_STEP1 else null
                ).toTypedArray()
            )
        }
        composable(Routes.INVENTORY_LIST) {
            NavPlaceholder(
                navController,
                "Inventario · Sublotes del grupo", Routes.INVENTORY_LIST, true,
                "Detalle del sublote (ejemplo)" to Routes.inventoryDetail("1")
            )
        }
        composable(
            route = Routes.INVENTORY_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_BATCH_ID) { type = NavType.StringType })
        ) { entry ->
            val batchId = entry.arguments?.getString(Routes.ARG_BATCH_ID).orEmpty()
            NavPlaceholder(
                navController,
                "Detalle del sublote #$batchId", Routes.inventoryDetail(batchId), true,
                "Gastos del sublote" to Routes.inventoryBatchExpenses(batchId),
                "Vender sublote" to Routes.saleBatchStep1(batchId)
            )
        }
        composable(
            route = Routes.INVENTORY_BATCH_EXPENSES,
            arguments = listOf(navArgument(Routes.ARG_BATCH_ID) { type = NavType.StringType })
        ) { entry ->
            val batchId = entry.arguments?.getString(Routes.ARG_BATCH_ID).orEmpty()
            NavPlaceholder(
                navController,
                "Gastos del sublote #$batchId", Routes.inventoryBatchExpenses(batchId), true,
                "Registrar gasto" to Routes.EXPENSE_LINK_BATCHES
            )
        }

        // ── Venta desde un sublote ──────────────────────────
        composable(
            route = Routes.SALE_BATCH_STEP1,
            arguments = listOf(navArgument(Routes.ARG_BATCH_ID) { type = NavType.StringType })
        ) { entry ->
            val batchId = entry.arguments?.getString(Routes.ARG_BATCH_ID).orEmpty()
            NavPlaceholder(
                navController,
                "Venta del sublote #$batchId · Paso 1 · Cliente", Routes.saleBatchStep1(batchId), true,
                "Siguiente" to Routes.saleBatchStep2(batchId)
            )
        }
        composable(
            route = Routes.SALE_BATCH_STEP2,
            arguments = listOf(navArgument(Routes.ARG_BATCH_ID) { type = NavType.StringType })
        ) { entry ->
            val batchId = entry.arguments?.getString(Routes.ARG_BATCH_ID).orEmpty()
            NavPlaceholder(
                navController,
                "Venta del sublote #$batchId · Paso 2 · Confirmar", Routes.saleBatchStep2(batchId), true,
                "Confirmar venta" to Routes.SALE_DONE
            )
        }

        // ── Compras ─────────────────────────────────────────
        // Los pasos 1-3 y la pantalla final los resuelve PurchaseFlow con el estado del ViewModel.
        composable(Routes.PURCHASE_STEP1) {
            // viewModel() ligado a esta entrada del NavHost: se limpia al salir de la ruta.
            val purchaseViewModel: PurchaseViewModel = viewModel()
            val estado by purchaseViewModel.uiState.collectAsState()

            // "Atrás" del sistema en la pantalla final lleva al inicio, no al paso 3.
            BackHandler(enabled = estado.compraRegistrada) {
                purchaseViewModel.reiniciarCompra()
                navController.popBackStack(Routes.HOME, inclusive = false)
            }

            PurchaseFlow(
                viewModel = purchaseViewModel,
                onSalir = { navController.popBackStack() },
                onIrAInventario = {
                    navController.navigate(Routes.INVENTORY) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        // ── Ventas ──────────────────────────────────────────
        composable(Routes.SALE_STEP1) { NavPlaceholder(navController, "Venta · Paso 1 · Cliente", Routes.SALE_STEP1, true, "Siguiente" to Routes.SALE_STEP2) }
        composable(Routes.SALE_STEP2) { NavPlaceholder(navController, "Venta · Paso 2 · Café", Routes.SALE_STEP2, true, "Siguiente" to Routes.SALE_STEP3) }
        composable(Routes.SALE_STEP3) { NavPlaceholder(navController, "Venta · Paso 3 · Confirmar", Routes.SALE_STEP3, true, "Registrar venta" to Routes.SALE_DONE) }
        composable(Routes.SALE_DONE) {
            // "Atrás" del sistema lleva al inicio, no al paso 3 del formulario.
            BackHandler { navController.popBackStack(Routes.HOME, inclusive = false) }
            PlaceholderScreen(
                title = "Venta registrada",
                route = Routes.SALE_DONE,
                actions = listOf(
                    PlaceholderAction("Registrar nueva venta") {
                        navController.navigate(Routes.SALE_STEP1) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    PlaceholderAction("Ir a inventario") {
                        navController.navigate(Routes.INVENTORY) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            )
        }

        // ── Secado ──────────────────────────────────────────
        // Orden del Figma: 1 → 2 → 3 → 4 → 5 → 6 (sin gastos) o 5 → 7 (gastos) → 6.
        composable(Routes.DRYING_STEP1) { NavPlaceholder(navController, "Secado · Paso 1 · Elegir café", Routes.DRYING_STEP1, true, "Siguiente" to Routes.DRYING_STEP2) }
        composable(Routes.DRYING_STEP2) { NavPlaceholder(navController, "Secado · Paso 2 · Confirmar inicio", Routes.DRYING_STEP2, true, "Iniciar secado" to Routes.DRYING_STEP3) }
        composable(Routes.DRYING_STEP3) {
            NavPlaceholder(
                navController,
                "Secado · Paso 3 · Iniciado", Routes.DRYING_STEP3, true,
                "Ver secados en curso" to Routes.DRYING_STEP4,
                "Ir a inventario" to Routes.INVENTORY
            )
        }
        composable(Routes.DRYING_STEP4) { NavPlaceholder(navController, "Secado · Paso 4 · En curso", Routes.DRYING_STEP4, true, "Finalizar secado" to Routes.DRYING_STEP5) }
        composable(Routes.DRYING_STEP5) {
            NavPlaceholder(
                navController,
                "Secado · Paso 5 · Finalizar", Routes.DRYING_STEP5, true,
                "Confirmar resultado (sin gastos)" to Routes.DRYING_STEP6,
                "Registrar gastos del secado" to Routes.DRYING_STEP7
            )
        }
        composable(Routes.DRYING_STEP6) {
            // "Atrás" del sistema lleva al inicio, no a los pasos anteriores del secado.
            BackHandler { navController.popBackStack(Routes.HOME, inclusive = false) }
            PlaceholderScreen(
                title = "Secado · Paso 6 · Secado registrado",
                route = Routes.DRYING_STEP6,
                actions = listOf(
                    PlaceholderAction("Registrar nuevo secado") {
                        navController.navigate(Routes.DRYING_STEP1) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    PlaceholderAction("Ir a inventario") {
                        navController.navigate(Routes.INVENTORY) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            )
        }
        composable(Routes.DRYING_STEP7) { NavPlaceholder(navController, "Secado · Paso 7 · Gastos del secado", Routes.DRYING_STEP7, true, "Guardar gasto" to Routes.DRYING_STEP6) }

        // ── Gastos ──────────────────────────────────────────
        composable(Routes.EXPENSES) { backStackEntry ->
            GastosRoute(navController, backStackEntry, registro = false)
        }
        composable(Routes.EXPENSE_NEW) { backStackEntry ->
            GastosRoute(navController, backStackEntry, registro = true)
        }
        // Reutiliza el registro con asociación activa, sin crear una tercera pantalla principal.
        composable(Routes.EXPENSE_LINK_BATCHES) { backStackEntry ->
            GastosRoute(navController, backStackEntry, registro = true, asociar = true)
        }

        // ── Ajustes ─────────────────────────────────────────
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            SettingsScreen(
                perfil = settingsState.perfil,
                mostrarConfirmacion = settingsState.mostrarConfirmacion,
                onBack = { navController.popBackStack() },
                onPedirCerrarSesion = settingsViewModel::pedirCerrarSesion,
                onCancelarCierre = settingsViewModel::cancelarCierre,
                onConfirmarCierre = {
                    // Cierra la sesión de Firebase y luego vuelve al login borrando la pila.
                    settingsViewModel.confirmarCierre()
                    cerrarSesion()
                }
            )
        }
    }

    // Diálogos de ayuda, recuperación, soporte, avisos y error de registro.
    AuthDialogs(
        state = dialogs,
        onRetryRegistration = { pendingRegistration?.let(processRegistration) }
    )
}

/** Navega a una ruta sin duplicarla en la cima de la pila. */
private fun NavHostController.navigateTo(route: String) =
    navigate(route) { launchSingleTop = true }

/** Cierra sesión: vuelve al login y borra la pila ("atrás" no regresa a pantallas de la cuenta). */
private fun NavHostController.logout() =
    navigate(Routes.LOGIN) {
        popUpTo(Routes.HOME) { inclusive = true }
    }

/** Placeholder con botón "atrás" opcional y botones que navegan a otras rutas. */
@Composable
private fun NavPlaceholder(
    navController: NavHostController,
    title: String,
    route: String,
    canGoBack: Boolean = true,
    vararg links: Pair<String, String>
) {
    PlaceholderScreen(
        title = title,
        route = route,
        onBack = if (canGoBack) ({ navController.popBackStack() }) else null,
        actions = links.map { (label, target) ->
            PlaceholderAction(label) {
                navController.navigate(target) { launchSingleTop = true }
            }
        }
    )
}
