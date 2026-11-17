package com.cafesmart.app.ui.auth

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cafesmart.app.data.auth.AuthRepository
import com.cafesmart.app.data.auth.AuthResult
import com.cafesmart.app.navigation.AppNavHost
import com.cafesmart.app.navigation.Routes
import com.cafesmart.app.ui.theme.CafesmartTheme
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Regresión funcional del Login de main, sin expectativas del diseño descartado ni red. */
class LoginMainRegressionTest {
    @get:Rule val compose = createComposeRule()
    private class AuthLocal : AuthRepository {
        var ingresos = 0
        override suspend fun signIn(email: String, password: String): AuthResult {
            ingresos++
            assertEquals("prueba@ejemplo.com", email)
            assertEquals("Prueba123", password)
            return AuthResult.Success
        }
        override suspend fun register(email: String, password: String, displayName: String): AuthResult = error("Sin registro en esta prueba")
        override suspend fun sendPasswordReset(email: String): AuthResult = error("Sin envío en esta prueba")
        override fun signOut() = Unit
    }

    @Test fun validaCredencialesYConservaRecordarYAccesibilidadBasica() {
        val repo = AuthLocal()
        val vm = AuthViewModel(repo)
        compose.setContent {
            val estado by vm.state.collectAsState()
            CafesmartTheme {
                LoginScreen(estado, vm::update, { vm.submitLogin { vm.login(it) } }, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, repo.ingresos); assertFalse(vm.uiState.errors.isEmpty()) }
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("prueba@ejemplo.com")
        compose.onAllNodes(hasSetTextAction())[1].performScrollTo().performTextInput("Prueba123")
        compose.onNodeWithContentDescription("Mostrar contraseña").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ocultar contraseña").assertExists()
        compose.onNodeWithText("Recordar mi cuenta").performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertFalse(vm.uiState.rememberMe) }
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, repo.ingresos) }
    }

    @Test fun recuperarContrasenaNavegaYVuelveSinPerderRecordar() {
        val repo = AuthLocal()
        val factory = viewModelFactory { initializer { AuthViewModel(repo) } }
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController()
            CafesmartTheme {
                val auth: AuthViewModel = viewModel(factory = factory)
                AppNavHost(navController = nav)
                LaunchedEffect(Unit) {
                    nav.currentBackStackEntryFlow.first()
                    auth.update { it.copy(rememberMe = false) }
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            }
        }
        compose.onNodeWithText("¿Olvidaste tu contraseña?").performScrollTo().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(Routes.FORGOT_PASSWORD, nav.currentDestination?.route); nav.popBackStack() }
        compose.onNodeWithText("Recordar mi cuenta").performScrollTo().assertIsOff()
        compose.runOnIdle { assertEquals(Routes.LOGIN, nav.currentDestination?.route); assertEquals(0, repo.ingresos) }
    }
}
