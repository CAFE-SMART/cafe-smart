package com.cafesmart.app.ui.gastos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.viewmodel.gastos.GastosViewModel
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Regresión del propietario de Gastos: una instancia por HOME y otra al iniciar una sesión nueva. */
class GastosNavigationScopeTest {
    @get:Rule val compose = createComposeRule()

    /** Ejercita las tres rutas reales, el cierre de sesión y el acceso de demostración, sin servicios externos. */
    @Test fun N01_comparteViewModelYRenuevaPropietarioAlReingresar() {
        lateinit var nav: NavHostController
        lateinit var entradaAnterior: NavBackStackEntry
        lateinit var modeloAnterior: GastosViewModel
        compose.setContent {
            nav = rememberNavController()
            CafesmartTheme {
                Scaffold { padding -> AppNavHost(Modifier.padding(padding), nav) }
            }
            // Solo el arranque omite el login, igual que G01. El reingreso posterior usa su formulario real de demo.
            LaunchedEffect(Unit) {
                nav.currentBackStackEntryFlow.first()
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
        compose.onNodeWithText("Registrar gasto").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Borrador compartido")
        compose.runOnIdle {
            entradaAnterior = nav.getBackStackEntry(Routes.HOME)
            modeloAnterior = ViewModelProvider(entradaAnterior)[GastosViewModel::class.java]
            assertEquals("Borrador compartido", modeloAnterior.uiState.value.formulario.concepto)
            nav.navigate(Routes.EXPENSES)
        }
        compose.onNodeWithText("Gastos generales").assertIsDisplayed()
        compose.runOnIdle {
            assertSame(modeloAnterior, ViewModelProvider(nav.getBackStackEntry(Routes.HOME))[GastosViewModel::class.java])
            nav.popBackStack()
        }
        // Volver a la entrada anterior conserva el borrador; no se pulsa Registrar otro gasto.
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("Borrador compartido")
        compose.runOnIdle { nav.navigate(Routes.EXPENSE_LINK_BATCHES) }
        compose.onNodeWithText("Seleccionar sublotes").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertSame(modeloAnterior, ViewModelProvider(nav.getBackStackEntry(Routes.HOME))[GastosViewModel::class.java])
            assertEquals(AplicaGasto.SUBLOTES, modeloAnterior.uiState.value.formulario.aplicaA)
            assertEquals("Borrador compartido", modeloAnterior.uiState.value.formulario.concepto)
            nav.popBackStack(Routes.HOME, false)
        }
        compose.onNodeWithText("Salir").performClick()
        compose.onNodeWithText("Cerrar sesión").assertIsDisplayed()
        compose.onNode(hasText("Salir") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Entrar").assertExists()
        compose.runOnIdle {
            assertEquals(Routes.LOGIN, nav.currentDestination?.route)
            assertEquals(Lifecycle.State.DESTROYED, entradaAnterior.lifecycle.currentState)
        }
        // Credenciales sintéticas: MODO_DEMOSTRACION valida el formato sin autenticar contra un servidor.
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("prueba@ejemplo.com")
        compose.onAllNodes(hasSetTextAction())[1].performScrollTo().performTextInput("Prueba123")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.onNodeWithText("Bienvenido a").assertIsDisplayed()
        compose.onNodeWithText("Registrar gasto").performScrollTo().performClick()
        compose.runOnIdle {
            val nuevaEntrada = nav.getBackStackEntry(Routes.HOME)
            val nuevoModelo = ViewModelProvider(nuevaEntrada)[GastosViewModel::class.java]
            assertNotSame(entradaAnterior, nuevaEntrada)
            assertNotSame(modeloAnterior, nuevoModelo)
            assertEquals("", nuevoModelo.uiState.value.formulario.concepto)
            assertTrue(nuevoModelo.uiState.value.gastos.isEmpty())
            assertEquals(AplicaGasto.GENERAL, nuevoModelo.uiState.value.formulario.aplicaA)
        }
        // El placeholder también aporta texto accesible; aquí se comprueba solo el valor editable.
        compose.onAllNodes(hasSetTextAction())[0].assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
    }
}
