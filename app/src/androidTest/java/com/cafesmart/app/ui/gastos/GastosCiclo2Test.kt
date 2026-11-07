package com.cafesmart.app.ui.gastos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.GastosViewModel
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Recorre las rutas reales con el ViewModel propiedad de HOME, sin autenticar en Firebase. */
class GastosCiclo2Test {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController

    private fun iniciar() {
        compose.setContent {
            nav = rememberNavController()
            CafesmartTheme { Scaffold { padding -> AppNavHost(Modifier.padding(padding), nav) } }
            LaunchedEffect(Unit) {
                nav.currentBackStackEntryFlow.first()
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
    }

    private fun pulsar(texto: String) = compose.onNodeWithText(texto).performScrollTo().performClick()
    private fun modelo() = ViewModelProvider(nav.getBackStackEntry(Routes.HOME))[GastosViewModel::class.java]

    @Test fun selectorSinInventarioPermiteVolverSinInventarSublotes() {
        var regreso = false
        compose.setContent {
            CafesmartTheme {
                AsociarSublotesScreen(
                    com.cafesmart.app.viewmodel.gastos.GastosUiState(),
                    onAlternar = { error("No debe haber opciones sin inventario") },
                    onVolver = { regreso = true }
                )
            }
        }
        compose.onNodeWithText("No hay sublotes disponibles").assertIsDisplayed()
        compose.onNodeWithText("0 sublote(s) seleccionado(s)").assertIsDisplayed()
        compose.onNodeWithText("Guardar selección").performClick()
        compose.runOnIdle { assertTrue(regreso) }
    }

    @Test fun inicioListaVaciaEjemplosFiltrosYRegistro() {
        iniciar()
        pulsar("Ver gastos")
        compose.runOnIdle { assertEquals(Routes.EXPENSES, nav.currentDestination?.route) }
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Aún no has registrado gastos"))
        compose.onNodeWithText("Aún no has registrado gastos").assertExists()
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Ver datos de ejemplo"))
        pulsar("Ver datos de ejemplo")
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Transporte de café (ejemplo)"))
        compose.onNodeWithText("Transporte de café (ejemplo)").assertExists()
        compose.onNodeWithText("Transporte · 06/11/2026").assertExists()
        compose.onAllNodesWithText(com.cafesmart.app.viewmodel.gastos.dineroGasto(45000))[0].assertExists()
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Cargue en bodega (ejemplo)"))
        compose.onNodeWithText("Cargue · 05/11/2026").assertExists()
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Pagados"))
        pulsar("Pagados")
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Cargue en bodega (ejemplo)"))
        compose.onNodeWithText("Cargue en bodega (ejemplo)").assertExists()
        compose.onNodeWithText("Transporte de café (ejemplo)").assertDoesNotExist()
        compose.onNodeWithTag("gastos_lista").performScrollToNode(hasText("Registrar gasto"))
        pulsar("Registrar gasto")
        compose.onNodeWithText("Registro de Gastos").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Routes.EXPENSE_NEW, nav.currentDestination?.route) }
    }

    @Test fun seleccionMultipleGuardarYVolverConservaBorradorYReparto() {
        iniciar()
        pulsar("Registrar gasto")
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Transporte compartido")
        compose.onAllNodes(hasSetTextAction())[2].performTextInput("12000")
        pulsar("Asociar a sublotes")
        pulsar("Seleccionar…")
        compose.runOnIdle { assertEquals(Routes.EXPENSE_LINK_BATCHES, nav.currentDestination?.route) }
        pulsar("Sublote 1")
        pulsar("Sublote 2")
        compose.onNodeWithText("2 sublote(s) seleccionado(s)").assertIsDisplayed()
        pulsar("Sublote 2")
        compose.onNodeWithText("1 sublote(s) seleccionado(s)").assertIsDisplayed()
        pulsar("Sublote 2")
        compose.onNodeWithText("Guardar selección").performClick()
        compose.runOnIdle {
            assertEquals(Routes.EXPENSE_NEW, nav.currentDestination?.route)
            assertEquals("Transporte compartido", modelo().uiState.value.formulario.concepto)
            assertEquals("12000", modelo().uiState.value.formulario.monto)
            assertEquals(setOf(1L, 2L), modelo().uiState.value.formulario.asignaciones.keys)
            modelo().editar { it.copy(asignaciones = mapOf(1L to "5000", 2L to "7000")) }
        }
        pulsar("2 sublote(s) seleccionado(s)")
        compose.onNodeWithText("Seleccionar sublotes").assertIsDisplayed()
        // La flecha real recorre el mismo retorno que BackHandler sin inyectar KEYCODE_BACK
        // en una ventana que Espresso puede encontrar temporalmente sin foco.
        compose.onNodeWithContentDescription("Volver").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(Routes.EXPENSE_NEW, nav.currentDestination?.route)
            assertEquals("Transporte compartido", modelo().uiState.value.formulario.concepto)
            assertEquals("12000", modelo().uiState.value.formulario.monto)
            assertEquals(mapOf(1L to "5000", 2L to "7000"), modelo().uiState.value.formulario.asignaciones)
        }
        // Salir a Inicio y volver al formulario tampoco debe borrar el borrador.
        compose.runOnIdle { nav.popBackStack(Routes.HOME, false) }
        pulsar("Registrar gasto")
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("Transporte compartido")
        compose.runOnIdle {
            modelo().solicitarConfirmacion()
            assertFalse(modelo().uiState.value.errores.hayErrores)
        }
    }
}
