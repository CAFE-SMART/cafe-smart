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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cafesmart.app.data.model.AplicaGasto
import com.cafesmart.app.data.auth.AuthRepository
import com.cafesmart.app.data.auth.AuthResult
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.theme.CafesmartTheme
import com.cafesmart.app.ui.auth.AuthViewModel
import com.cafesmart.app.viewmodel.gastos.GastosViewModel
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Regresión del propietario de Gastos: una instancia por HOME y otra al iniciar una sesión nueva. */
class GastosNavigationScopeTest {
    @get:Rule val compose = createComposeRule()

    /** Rutas, cierre de sesión y reingreso reales; solo la fuente de autenticación es local. */
    @Test fun N01_comparteViewModelYRenuevaPropietarioAlReingresar() {
        lateinit var nav: NavHostController
        lateinit var entradaAnterior: NavBackStackEntry
        lateinit var modeloAnterior: GastosViewModel
        lateinit var modeloAuth: AuthViewModel
        // Producción usa MODO_DEMOSTRACION=false. Las credenciales sintéticas deben pasar
        // por AuthViewModel y emitir LoggedIn sin conectarse a Firebase ni alterar sus claves.
        val repoAuth = object : AuthRepository {
            var ingresos = 0
            var salidas = 0
            var credencialesRecibidas: Pair<String, String>? = null

            override suspend fun signIn(email: String, password: String): AuthResult {
                ingresos++
                credencialesRecibidas = email to password
                return AuthResult.Success
            }

            override suspend fun register(email: String, password: String, displayName: String): AuthResult =
                error("Esta prueba no debe registrar cuentas")

            override fun signOut() { salidas++ }
        }
        val authFactory = viewModelFactory { initializer { AuthViewModel(repoAuth) } }
        compose.setContent {
            nav = rememberNavController()
            CafesmartTheme {
                Scaffold { padding ->
                    // Mismo propietario y clave predeterminada que usa AppNavHost antes del NavHost.
                    // Su viewModel() recupera esta instancia; las rutas de producción no cambian.
                    modeloAuth = viewModel(factory = authFactory)
                    AppNavHost(Modifier.padding(padding), nav)
                }
            }
            // Solo el arranque omite el login; el reingreso recorre su formulario y evento LoggedIn reales.
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
        compose.onNodeWithText("Seleccionar sublotes").assertIsDisplayed()
        compose.runOnIdle {
            assertSame(modeloAnterior, ViewModelProvider(nav.getBackStackEntry(Routes.HOME))[GastosViewModel::class.java])
            assertEquals(AplicaGasto.SUBLOTES, modeloAnterior.uiState.value.formulario.aplicaA)
            assertEquals("Borrador compartido", modeloAnterior.uiState.value.formulario.concepto)
            nav.popBackStack(Routes.HOME, false)
        }
        // Regresar a Inicio no termina la sesión: abrir Gastos otra vez conserva propietario y borrador.
        compose.runOnIdle { assertEquals(Routes.HOME, nav.currentDestination?.route) }
        compose.onNodeWithText("Registrar gasto").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(Routes.EXPENSE_NEW, nav.currentDestination?.route)
            assertSame(entradaAnterior, nav.getBackStackEntry(Routes.HOME))
            assertSame(modeloAnterior, ViewModelProvider(nav.getBackStackEntry(Routes.HOME))[GastosViewModel::class.java])
            assertEquals("Borrador compartido", modeloAnterior.uiState.value.formulario.concepto)
            assertEquals(AplicaGasto.SUBLOTES, modeloAnterior.uiState.value.formulario.aplicaA)
        }
        compose.onAllNodes(hasSetTextAction())[0].assertTextEquals("Borrador compartido")
        compose.onNodeWithContentDescription("Volver").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(Routes.HOME, nav.currentDestination?.route) }
        compose.onNodeWithText("Salir").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Cerrar sesión").assertIsDisplayed()
        compose.onNode(hasText("Salir") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Entrar").assertExists()
        compose.runOnIdle {
            assertEquals(Routes.LOGIN, nav.currentDestination?.route)
            assertEquals(Lifecycle.State.DESTROYED, entradaAnterior.lifecycle.currentState)
            assertEquals(1, repoAuth.salidas)
            assertEquals("", modeloAuth.uiState.loginEmail)
            assertEquals("", modeloAuth.uiState.loginPassword)
        }
        // La UI valida las credenciales y el repositorio inyectado responde sin red.
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("prueba@ejemplo.com")
        compose.onAllNodes(hasSetTextAction())[1].performScrollTo().performTextInput("Prueba123")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        try {
            // El repositorio local responde en el hilo principal: no hay una operación de red
            // que sondear. Sincronizar Compose drena recomposiciones, layout y la transición
            // del NavHost; runOnUiThread dentro de waitUntil solo leía un estado intermedio.
            compose.waitForIdle()
            // Una acción visible de Home comprueba la pantalla real y sincroniza el scroll.
            compose.onNodeWithText("Salir").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Bienvenido a").performScrollTo().assertIsDisplayed()
            compose.runOnIdle {
                assertEquals("El reingreso debe terminar en Inicio", Routes.HOME, nav.currentDestination?.route)
                assertEquals("Inicio debe estar reanudado al terminar la transición",
                    Lifecycle.State.RESUMED, nav.currentBackStackEntry?.lifecycle?.currentState)
                assertEquals(1, repoAuth.ingresos)
                assertEquals("prueba@ejemplo.com" to "Prueba123", repoAuth.credencialesRecibidas)
            }
        } catch (fallo: Throwable) {
            // El timeout anterior ocultaba cuál condición fallaba. Conservar la causa y
            // adjuntar todas las raíces incluye también un posible diálogo sobre Login.
            val estado = runCatching {
                compose.runOnUiThread {
                    "ruta=${nav.currentDestination?.route}, " +
                        "ciclo=${nav.currentBackStackEntry?.lifecycle?.currentState}, " +
                        "ingresos=${repoAuth.ingresos}, salidas=${repoAuth.salidas}, " +
                        "enviando=${modeloAuth.uiState.isSubmitting}, errores=${modeloAuth.uiState.errors}"
                }
            }.getOrElse { "No se pudo leer el estado: $it" }
            val semantica = runCatching {
                val raices = compose.onAllNodes(isRoot(), useUnmergedTree = true)
                (0 until raices.fetchSemanticsNodes().size).joinToString("\n") { indice ->
                    raices[indice].printToString()
                }
            }.getOrElse { "No se pudo leer la semántica: $it" }
            throw AssertionError("Fallo al reingresar a Home: $estado\n$semantica", fallo)
        }
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
