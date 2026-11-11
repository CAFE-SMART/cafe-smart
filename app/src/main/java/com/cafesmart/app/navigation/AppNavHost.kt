package com.cafesmart.app.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.cafesmart.app.data.finanzas.AlmacenClaveFinancieraLocal
import com.cafesmart.app.data.finanzas.FinanzasLocal
import com.cafesmart.app.data.finanzas.SinDatosFinancieros
import com.cafesmart.app.data.finanzas.FirebaseConfirmacionCuenta
import com.cafesmart.app.data.finanzas.fechaDeHoy
import com.cafesmart.app.ui.finance.AccesoFinancieroScreen
import com.cafesmart.app.data.inventario.OcupacionDeEjemplo
import com.cafesmart.app.ui.finance.HistorialFinancieroScreen
import com.cafesmart.app.ui.settings.CapacidadBodegaScreen
import com.cafesmart.app.viewmodel.settings.CapacidadBodegaViewModel
import com.cafesmart.app.ui.finance.ResumenFinancieroScreen
import com.cafesmart.app.viewmodel.finance.AccesoFinancieroViewModel
import com.cafesmart.app.viewmodel.finance.HistorialFinancieroViewModel
import com.cafesmart.app.viewmodel.finance.ResumenFinancieroViewModel
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.negocio.negocioDesdeRegistro
import com.cafesmart.app.data.session.FirebaseSessionGateway
import com.cafesmart.app.data.session.DestinoInicial
import com.cafesmart.app.data.session.PreferenciaNotificacionesLocal
import com.cafesmart.app.data.session.PreferenciaSesionLocal
import com.cafesmart.app.data.session.decidirInicio
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
import com.cafesmart.app.ui.auth.AuthErrorAction
import com.cafesmart.app.ui.auth.AuthEvent
import com.cafesmart.app.ui.auth.AuthOrigin
import com.cafesmart.app.ui.auth.AuthDialogsState
import com.cafesmart.app.ui.auth.AuthHelpContext
import com.cafesmart.app.ui.auth.AuthPage
import com.cafesmart.app.ui.auth.AuthViewModel
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.auth.ConfirmDestructiveDialog
import com.cafesmart.app.ui.auth.LoginScreen
import com.cafesmart.app.ui.auth.MODO_DEMOSTRACION
import com.cafesmart.app.ui.auth.RecuperarContrasenaScreen
import com.cafesmart.app.ui.auth.RecuperarContrasenaViewModel
import com.cafesmart.app.ui.auth.RegisterStep1Screen
import com.cafesmart.app.ui.auth.RegisterStep2Screen
import com.cafesmart.app.ui.auth.RegistrationData
import com.cafesmart.app.ui.auth.RegistrationFailure
import com.cafesmart.app.ui.auth.RegistrationSuccessScreen
import com.cafesmart.app.ui.auth.authErrorDialogData
import com.cafesmart.app.ui.auth.SIMULAR_ERROR_REGISTRO
import com.cafesmart.app.ui.home.HomeScreen
import com.cafesmart.app.ui.gastos.GastosRoute
import com.cafesmart.app.ui.screens.PlaceholderAction
import com.cafesmart.app.ui.screens.PlaceholderScreen
import com.cafesmart.app.ui.screens.SplashScreen
import com.cafesmart.app.ui.screens.purchase.PurchaseFlow
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.ui.inventory.InventoryScreen
import com.cafesmart.app.ui.settings.NegocioInfoScreen
import com.cafesmart.app.ui.settings.SettingsScreen
import com.cafesmart.app.viewmodel.inventory.InventoryViewModel
import com.cafesmart.app.viewmodel.inventory.tipoCafeDesdeRuta
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import com.cafesmart.app.viewmodel.settings.NegocioInfoViewModel
import com.cafesmart.app.viewmodel.settings.SettingsViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    // Un solo AuthViewModel compartido por login y registro (paso 1 y 2).
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()
    val dialogs = remember { AuthDialogsState() }
    // "Recordar mi cuenta": se guarda al iniciar sesión y el Splash la consulta al abrir la app.
    val preferenciaSesion = remember { PreferenciaSesionLocal(context) }

    var registrationSuccess by remember { mutableStateOf<RegistrationData?>(null) }
    var pendingRegistration by remember { mutableStateOf<RegistrationData?>(null) }
    var simulateErrorNext by remember { mutableStateOf(SIMULAR_ERROR_REGISTRO) }

    // Procesa el registro en modo demostración (sin backend).
    lateinit var processRegistration: (RegistrationData) -> Unit
    processRegistration = { data ->
        pendingRegistration = data
        when {
            // Registro real: el resultado llega por authViewModel.events (ver LaunchedEffect de abajo).
            !MODO_DEMOSTRACION -> authViewModel.register(data)

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
        authViewModel.signOut()
        preferenciaSesion.olvidar()
        registrationSuccess = null
        pendingRegistration = null
        dialogs.clear()
        navController.logout()
    }

    // Lo que pide cada botón del diálogo de error de autenticación.
    val onAuthErrorAction: (AuthErrorAction, AuthOrigin) -> Unit = { accion, origen ->
        when (accion) {
            AuthErrorAction.CERRAR -> Unit

            // Repite la solicitud con los mismos datos (siguen en el ViewModel).
            AuthErrorAction.REINTENTAR -> when (origen) {
                AuthOrigin.LOGIN -> authViewModel.submitLogin { authViewModel.login(it) }
                AuthOrigin.REGISTRO -> pendingRegistration?.let(processRegistration)
            }

            // Vuelve al login (que quedó debajo en la pila) con el correo ya escrito.
            AuthErrorAction.IR_A_LOGIN -> {
                val correo = pendingRegistration?.email ?: authViewModel.uiState.email.trim()
                authViewModel.goToLoginWith(correo)
                navController.popBackStack(Routes.LOGIN, inclusive = false)
            }

            AuthErrorAction.IR_A_REGISTRO -> {
                authViewModel.goToRegister()
                navController.navigate(Routes.REGISTER_STEP1) { launchSingleTop = true }
            }
        }
    }

    // Guarda el negocio del registro en Room (nombre, tipo y teléfono). Sin uid (modo demostración) no guarda.
    val negocios = remember { NegocioRepository.de(context) }
    val scope = rememberCoroutineScope()
    val guardarNegocio: (RegistrationData) -> Unit = { data ->
        FirebaseSessionGateway.usuarioActual()?.uid?.let { uid ->
            scope.launch {
                negocios.guardar(
                    negocioDesdeRegistro(
                        uid = uid,
                        nombre = data.businessName,
                        tipo = data.businessType.name,
                        telefono = data.phone,
                        creadoEn = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Atiende los resultados de Firebase (una sola vez cada uno): navegar o mostrar el error.
    LaunchedEffect(Unit) {
        authViewModel.events.collect { evento ->
            when (evento) {
                AuthEvent.LoggedIn -> {
                    // Guarda la casilla antes de que reset() limpie el formulario.
                    preferenciaSesion.guardarRecordar(authViewModel.uiState.rememberMe)
                    authViewModel.reset()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }

                is AuthEvent.Registered -> {
                    // Firebase ya dejó la sesión abierta al crear la cuenta; se conserva como en el login.
                    preferenciaSesion.guardarRecordar(true)
                    // La cuenta ya existe en Firebase; los datos del negocio se guardan en Room con el uid.
                    guardarNegocio(evento.data)
                    dialogs.registrationError = null
                    dialogs.authError = null
                    registrationSuccess = evento.data
                    navController.navigate(Routes.REGISTER_SUCCESS) { launchSingleTop = true }
                }

                is AuthEvent.LoginFailed ->
                    dialogs.authError = authErrorDialogData(evento.error, AuthOrigin.LOGIN)

                is AuthEvent.RegistrationFailed ->
                    dialogs.authError = authErrorDialogData(evento.error, AuthOrigin.REGISTRO)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        // ── Cuenta ──────────────────────────────────────────
        composable(Routes.SPLASH) {
            // Al terminar el splash decide según la sesión de Firebase y la casilla "Recordar mi cuenta":
            // con sesión y "recordar" entra a Inicio; con sesión sin "recordar" la cierra y pide el login.
            SplashScreen(
                onSplashFinished = {
                    val decision = decidirInicio(
                        hayUsuario = FirebaseSessionGateway.usuarioActual() != null,
                        recordar = preferenciaSesion.recordar()
                    )
                    if (decision.cerrarSesion) {
                        FirebaseSessionGateway.cerrarSesion()
                        preferenciaSesion.olvidar()
                    }
                    val ruta = if (decision.destino == DestinoInicial.INICIO) Routes.HOME else Routes.LOGIN
                    navController.navigate(ruta) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            var showExitConfirmation by remember { mutableStateOf(false) }

            LoginScreen(
                state = authState,
                isSubmitting = authState.isSubmitting,
                onChange = authViewModel::update,
                onSubmit = {
                    authViewModel.submitLogin { credenciales ->
                        if (MODO_DEMOSTRACION) {
                            // Limpia la contraseña temporal; al entrar, el login sale de la pila.
                            authViewModel.reset()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            authViewModel.login(credenciales)
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
                onForgotPassword = { navController.navigateTo(Routes.FORGOT_PASSWORD) },
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
        composable(Routes.FORGOT_PASSWORD) {
            val recuperarViewModel: RecuperarContrasenaViewModel = viewModel()
            val recuperarState by recuperarViewModel.state.collectAsState()
            RecuperarContrasenaScreen(
                estado = recuperarState,
                onBack = { navController.popBackStack() },
                onCorreoChange = recuperarViewModel::cambiarCorreo,
                onEnviar = recuperarViewModel::enviar,
                onReenviar = recuperarViewModel::reenviar,
                onVolverAlLogin = { navController.popBackStack(Routes.LOGIN, inclusive = false) }
            )
        }
        composable(Routes.REGISTER_STEP1) {
            // "Atrás" del sistema también retrocede el estado del ViewModel.
            BackHandler {
                authViewModel.back()
                navController.popBackStack()
            }
            RegisterStep1Screen(
                state = authState,
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
                state = authState,
                isSubmitting = authState.isSubmitting,
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
                onVerGastos = { navController.navigateTo(Routes.EXPENSES) },
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
                onAjustarBodega = { navController.navigateTo(Routes.SETTINGS_WAREHOUSE) },
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
            val inventoryViewModel: InventoryViewModel = viewModel {
                InventoryViewModel(negocios = negocios, uid = FirebaseSessionGateway.usuarioActual()?.uid)
            }
            val inventoryState by inventoryViewModel.uiState.collectAsState()

            InventoryScreen(
                estado = inventoryState,
                // Figma: cada tipo abre su lista por calidad. Esa pantalla aún es placeholder.
                onTipoClick = { tipo -> navController.navigateTo(Routes.inventoryType(tipo.name)) },
                onOrdenChange = inventoryViewModel::cambiarOrden,
                onAjustarBodega = { navController.navigateTo(Routes.SETTINGS_WAREHOUSE) },
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
            val tipo = tipoCafeDesdeRuta(entry.arguments?.getString(Routes.ARG_TIPO))
            if (tipo == null) {
                // Tipo desconocido en la ruta: se vuelve a la pantalla anterior en vez de mostrar otro tipo.
                androidx.compose.runtime.LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
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

            // La capacidad y la ocupacion de la bodega alimentan los avisos de capacidad; se actualizan solas.
            val uidActual = FirebaseSessionGateway.usuarioActual()?.uid
            LaunchedEffect(uidActual) {
                val kgOcupados = OcupacionDeEjemplo.kgOcupados()
                if (uidActual == null) {
                    purchaseViewModel.actualizarBodega(0.0, kgOcupados)
                } else {
                    negocios.observar(uidActual).collect {
                        purchaseViewModel.actualizarBodega(it?.capacidadBodegaKg ?: 0.0, kgOcupados)
                    }
                }
            }

            // "Atrás" del sistema en la pantalla final lleva al inicio, no al paso 3.
            BackHandler(enabled = estado.compraRegistrada) {
                purchaseViewModel.reiniciarCompra()
                navController.popBackStack(Routes.HOME, inclusive = false)
            }

            PurchaseFlow(
                viewModel = purchaseViewModel,
                onConfigurarCapacidad = { navController.navigateTo(Routes.SETTINGS_WAREHOUSE) },
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
        // Selector de sublotes que comparte el borrador con el formulario de registro.
        composable(Routes.EXPENSE_LINK_BATCHES) { backStackEntry ->
            GastosRoute(navController, backStackEntry, registro = true, asociar = true)
        }

        // ── Ajustes ─────────────────────────────────────────
        composable(Routes.SETTINGS) { entrada ->
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(FirebaseSessionGateway, negocios, PreferenciaNotificacionesLocal(context))
            }
            val settingsState by settingsViewModel.uiState.collectAsState()
            // Avisos que dejan otras pantallas al volver a Ajustes (por ejemplo "Información de empresa"):
            // se pasan al ViewModel y se borran del handle enseguida para que no reaparezcan.
            val avisoDeOtraPantalla by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_AVISO_AJUSTES, null).collectAsState()
            LaunchedEffect(avisoDeOtraPantalla) {
                avisoDeOtraPantalla?.let {
                    settingsViewModel.mostrarAviso(AvisoUi(it))
                    entrada.savedStateHandle[CLAVE_AVISO_AJUSTES] = null
                }
            }

            SettingsScreen(
                perfil = settingsState.perfil,
                mostrarConfirmacion = settingsState.mostrarConfirmacion,
                onBack = { navController.popBackStack() },
                onGastosOperativos = { navController.navigateTo(Routes.EXPENSES) },
                onInformacionEmpresa = { navController.navigateTo(Routes.SETTINGS_BUSINESS) },
                onResumenFinanciero = { navController.navigateTo(Routes.FINANCE_SUMMARY) },
                onCapacidadBodega = { navController.navigateTo(Routes.SETTINGS_WAREHOUSE) },
                aviso = settingsState.aviso,
                onAvisoOculto = settingsViewModel::ocultarAviso,
                pestana = settingsState.pestana,
                onPestana = settingsViewModel::seleccionarPestana,
                notificaciones = settingsState.notificaciones,
                onNotificaciones = settingsViewModel::cambiarNotificaciones,
                edicion = settingsState.edicion,
                onEditarPerfil = settingsViewModel::abrirEditarPerfil,
                onNombreChange = settingsViewModel::cambiarNombre,
                onGuardarNombre = settingsViewModel::guardarNombre,
                onCancelarEdicion = settingsViewModel::cancelarEdicion,
                onPedirCerrarSesion = settingsViewModel::pedirCerrarSesion,
                onCancelarCierre = settingsViewModel::cancelarCierre,
                onConfirmarCierre = {
                    // Oculta la hoja; cerrarSesion() hace el signOut y vuelve al login borrando la pila.
                    settingsViewModel.confirmarCierre()
                    cerrarSesion()
                }
            )
        }

        // Finanzas · Resumen financiero: pide la clave propia (o la crea en el primer acceso) y solo con ella
        // muestra el resumen. El estado del acceso vive en esta entrada: al salir se pierde y se vuelve a pedir la clave.
        composable(Routes.FINANCE_SUMMARY) {
            val uid = FirebaseSessionGateway.usuarioActual()?.uid ?: "demo"
            val accesoViewModel: AccesoFinancieroViewModel = viewModel {
                AccesoFinancieroViewModel(uid, AlmacenClaveFinancieraLocal(context), FirebaseConfirmacionCuenta)
            }
            val acceso by accesoViewModel.estado.collectAsState()
            if (acceso.autorizado) {
                val resumenViewModel: ResumenFinancieroViewModel = viewModel {
                    ResumenFinancieroViewModel(FinanzasLocal(SinDatosFinancieros, ::fechaDeHoy))
                }
                val resumen by resumenViewModel.estado.collectAsState()
                ResumenFinancieroScreen(
                    estado = resumen,
                    onBack = { navController.popBackStack() },
                    onPeriodo = resumenViewModel::cambiarPeriodo,
                    onRecargar = resumenViewModel::recargar,
                    onAvisoOculto = resumenViewModel::ocultarAviso,
                    onHistorial = { navController.navigateTo(Routes.FINANCE_HISTORY) }
                )
            } else {
                AccesoFinancieroScreen(
                    estado = acceso,
                    onBack = { navController.popBackStack() },
                    onClave = accesoViewModel::cambiarClave,
                    onConfirmacion = accesoViewModel::cambiarConfirmacion,
                    onContrasenaCuenta = accesoViewModel::cambiarContrasenaCuenta,
                    onGuardarClave = accesoViewModel::guardarClave,
                    onIngresar = accesoViewModel::ingresar,
                    onOlvide = accesoViewModel::olvideMiClave,
                    onCancelarOlvido = accesoViewModel::cancelarOlvido,
                    onConfirmarContrasena = accesoViewModel::confirmarConContrasena,
                    onConfirmarGoogle = accesoViewModel::confirmarConGoogle
                )
            }
        }

        // Finanzas · Historial financiero: compras, ventas y gastos juntos. Solo se llega desde el Resumen, que ya
        // pidió la clave propia; no hay otra entrada.
        composable(Routes.FINANCE_HISTORY) {
            val historialViewModel: HistorialFinancieroViewModel = viewModel {
                HistorialFinancieroViewModel(FinanzasLocal(SinDatosFinancieros, ::fechaDeHoy), ::fechaDeHoy)
            }
            val historial by historialViewModel.estado.collectAsState()
            HistorialFinancieroScreen(
                estado = historial,
                onBack = { navController.popBackStack() },
                onFiltro = historialViewModel::cambiarFiltro,
                onVerMas = historialViewModel::verMas,
                onReintentar = historialViewModel::reintentar
            )
        }

        // Ajustes · Capacidad de bodega: kilos que caben y peso del bulto (Negocio). Se abre desde Ajustes, Inicio e Inventario;
        // al guardar vuelve a donde estaba. Los kilos ocupados hoy son de ejemplo hasta que Compras esté en Room.
        composable(Routes.SETTINGS_WAREHOUSE) {
            val capacidadViewModel: CapacidadBodegaViewModel = viewModel {
                CapacidadBodegaViewModel(FirebaseSessionGateway, negocios, OcupacionDeEjemplo)
            }
            val capacidadState by capacidadViewModel.uiState.collectAsState()
            LaunchedEffect(capacidadState.guardado) {
                if (capacidadState.guardado) {
                    navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_AVISO_AJUSTES, AVISO_CAPACIDAD_GUARDADA)
                    navController.popBackStack()
                }
            }
            CapacidadBodegaScreen(
                estado = capacidadState,
                onBack = { navController.popBackStack() },
                onEditar = capacidadViewModel::editar,
                onGuardar = capacidadViewModel::guardar,
                onAvisoOculto = capacidadViewModel::ocultarAviso
            )
        }

        // Ajustes · Información de empresa: completa o edita el Negocio (Room). Al guardar vuelve a Ajustes,
        // que se actualiza solo porque observa el Negocio.
        composable(Routes.SETTINGS_BUSINESS) {
            val negocioViewModel: NegocioInfoViewModel = viewModel {
                NegocioInfoViewModel(FirebaseSessionGateway, negocios)
            }
            val negocioState by negocioViewModel.uiState.collectAsState()
            LaunchedEffect(negocioState.guardado) {
                if (negocioState.guardado) {
                    // Deja el aviso para Ajustes, que lo muestra al aparecer.
                    navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_AVISO_AJUSTES, AVISO_NEGOCIO_GUARDADO)
                    navController.popBackStack()
                }
            }
            NegocioInfoScreen(
                estado = negocioState,
                onBack = { navController.popBackStack() },
                onEditar = negocioViewModel::editar,
                onGuardar = negocioViewModel::guardar,
                onAvisoOculto = negocioViewModel::ocultarAviso
            )
        }
    }

    // Diálogos de ayuda, recuperación, soporte, avisos y error de registro.
    AuthDialogs(
        state = dialogs,
        onRetryRegistration = { pendingRegistration?.let(processRegistration) },
        onAuthErrorAction = onAuthErrorAction,
        onRecovery = { navController.navigateTo(Routes.FORGOT_PASSWORD) }
    )
}

// Aviso de éxito que otra pantalla deja a Ajustes (se lee en su SavedStateHandle).
private const val CLAVE_AVISO_AJUSTES = "aviso_ajustes"
private const val AVISO_NEGOCIO_GUARDADO = "Datos del negocio guardados"
private const val AVISO_CAPACIDAD_GUARDADA = "Capacidad de bodega guardada"

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
