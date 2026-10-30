package com.cafesmart.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cafesmart.app.ui.screens.PlaceholderAction
import com.cafesmart.app.ui.screens.PlaceholderScreen
import kotlinx.coroutines.delay

private const val SPLASH_DELAY_MS = 1500L

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        // ── Cuenta ──────────────────────────────────────────
        composable(Routes.SPLASH) {
            // Tras un momento pasa al login y saca el splash de la pila.
            LaunchedEffect(Unit) {
                delay(SPLASH_DELAY_MS)
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            PlaceholderScreen(title = "Café Smart", route = Routes.SPLASH)
        }
        composable(Routes.LOGIN) {
            PlaceholderScreen(
                title = "Iniciar sesión",
                route = Routes.LOGIN,
                actions = listOf(
                    PlaceholderAction("Iniciar sesión") {
                        // Al entrar, el login sale de la pila: "atrás" no vuelve a él.
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    PlaceholderAction("Crear cuenta") {
                        navController.navigate(Routes.REGISTER_STEP1) { launchSingleTop = true }
                    }
                )
            )
        }
        composable(Routes.REGISTER_STEP1) {
            NavPlaceholder(navController, "Crear cuenta · Paso 1", Routes.REGISTER_STEP1, true, "Continuar" to Routes.REGISTER_STEP2)
        }
        composable(Routes.REGISTER_STEP2) {
            PlaceholderScreen(
                title = "Crear cuenta · Paso 2",
                route = Routes.REGISTER_STEP2,
                onBack = { navController.popBackStack() },
                actions = listOf(
                    PlaceholderAction("Crear cuenta") {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            )
        }

        // ── Inicio ──────────────────────────────────────────
        composable(Routes.HOME) {
            PlaceholderScreen(
                title = "Inicio",
                route = Routes.HOME,
                actions = listOf(
                    PlaceholderAction("Inventario") { navController.navigateTo(Routes.INVENTORY) },
                    PlaceholderAction("Registrar compra") { navController.navigateTo(Routes.PURCHASE_STEP1) },
                    PlaceholderAction("Registrar venta") { navController.navigateTo(Routes.SALE_STEP1) },
                    PlaceholderAction("Registrar gasto") { navController.navigateTo(Routes.EXPENSE_NEW) },
                    PlaceholderAction("Ajustes") { navController.navigateTo(Routes.SETTINGS) },
                    PlaceholderAction("Salir") { navController.logout() }
                )
            )
        }

        // ── Inventario ──────────────────────────────────────
        composable(Routes.INVENTORY) {
            NavPlaceholder(
                navController,
                "Inventario · Resumen", Routes.INVENTORY, true,
                "Café verde (ver por calidad)" to Routes.INVENTORY_TYPE
            )
        }
        composable(Routes.INVENTORY_TYPE) {
            NavPlaceholder(
                navController,
                "Inventario · Café verde por calidad", Routes.INVENTORY_TYPE, true,
                "Ver sublotes de una calidad" to Routes.INVENTORY_LIST,
                "Iniciar secado" to Routes.DRYING_STEP1
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
        composable(Routes.PURCHASE_STEP1) { NavPlaceholder(navController, "Compra · Paso 1 · Productor", Routes.PURCHASE_STEP1, true, "Siguiente" to Routes.PURCHASE_STEP2) }
        composable(Routes.PURCHASE_STEP2) { NavPlaceholder(navController, "Compra · Paso 2 · Café", Routes.PURCHASE_STEP2, true, "Siguiente" to Routes.PURCHASE_STEP3) }
        composable(Routes.PURCHASE_STEP3) { NavPlaceholder(navController, "Compra · Paso 3 · Confirmar", Routes.PURCHASE_STEP3, true, "Registrar compra" to Routes.PURCHASE_DONE) }
        composable(Routes.PURCHASE_DONE) {
            // "Atrás" del sistema lleva al inicio, no al paso 3 del formulario.
            BackHandler { navController.popBackStack(Routes.HOME, inclusive = false) }
            PlaceholderScreen(
                title = "Compra registrada",
                route = Routes.PURCHASE_DONE,
                actions = listOf(
                    PlaceholderAction("Registrar nueva compra") {
                        navController.navigate(Routes.PURCHASE_STEP1) {
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
        composable(Routes.EXPENSES) { NavPlaceholder(navController, "Gastos · Lista general", Routes.EXPENSES, true, "Registrar gasto" to Routes.EXPENSE_NEW) }
        composable(Routes.EXPENSE_NEW) { NavPlaceholder(navController, "Gastos · Registrar gasto", Routes.EXPENSE_NEW, true, "Asociar a sublotes" to Routes.EXPENSE_LINK_BATCHES) }
        composable(Routes.EXPENSE_LINK_BATCHES) { NavPlaceholder(navController, "Gastos · Asociar a sublotes", Routes.EXPENSE_LINK_BATCHES, true) }

        // ── Ajustes ─────────────────────────────────────────
        composable(Routes.SETTINGS) {
            PlaceholderScreen(
                title = "Ajustes",
                route = Routes.SETTINGS,
                onBack = { navController.popBackStack() },
                actions = listOf(
                    PlaceholderAction("Proceso de secado") { navController.navigateTo(Routes.DRYING_STEP1) },
                    PlaceholderAction("Gastos operativos") { navController.navigateTo(Routes.EXPENSES) },
                    PlaceholderAction("Cerrar sesión") { navController.logout() }
                )
            )
        }
    }
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