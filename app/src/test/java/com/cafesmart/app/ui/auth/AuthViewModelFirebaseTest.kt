package com.cafesmart.app.ui.auth

import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.auth.AuthRepository
import com.cafesmart.app.data.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Repositorio falso: responde lo que le digamos, sin Firebase. */
private class RepositorioFalso(var resultado: AuthResult = AuthResult.Success) : AuthRepository {
    var llamadasSignIn = 0
    var llamadasRegister = 0
    var cerrada = false
    var ultimoNombre: String? = null

    override suspend fun signIn(email: String, password: String): AuthResult {
        llamadasSignIn++
        return resultado
    }

    override suspend fun register(email: String, password: String, displayName: String): AuthResult {
        llamadasRegister++
        ultimoNombre = displayName
        return resultado
    }

    override fun signOut() {
        cerrada = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelFirebaseTest {

    private val credenciales = LoginCredentials("ana@cafe.com", "Secreto123", rememberMe = true)
    private val registro = RegistrationData(
        businessName = "Café del Cauca", businessType = BusinessType.COMPRAVENTA,
        firstName = "Ana", lastName = "Pérez", phone = "3001234567",
        email = "ana@cafe.com", password = "Secreto123"
    )

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginExitosoEmiteLoggedInYQuitaElCargando() = runTest {
        val repo = RepositorioFalso()
        val vm = AuthViewModel(repo)
        vm.login(credenciales)
        assertEquals(AuthEvent.LoggedIn, vm.events.first())
        assertFalse(vm.state.value.isSubmitting)
        assertEquals(1, repo.llamadasSignIn)
    }

    @Test
    fun loginFallidoEmiteElMensajeClaro() = runTest {
        val repo = RepositorioFalso(AuthResult.Failure(AuthError.CREDENCIALES_INCORRECTAS))
        val vm = AuthViewModel(repo)
        vm.login(credenciales)
        val evento = vm.events.first()
        assertEquals(AuthEvent.LoginFailed(AuthError.CREDENCIALES_INCORRECTAS), evento)
        assertEquals(AuthError.CREDENCIALES_INCORRECTAS.mensaje, (evento as AuthEvent.LoginFailed).message)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun registroExitosoEmiteRegisteredConLosDatosYElNombreCompleto() = runTest {
        val repo = RepositorioFalso()
        val vm = AuthViewModel(repo)
        vm.register(registro)
        assertEquals(AuthEvent.Registered(registro), vm.events.first())
        assertEquals("Ana Pérez", repo.ultimoNombre)
    }

    @Test
    fun registroConCorreoYaUsadoEmiteError() = runTest {
        val repo = RepositorioFalso(AuthResult.Failure(AuthError.CORREO_YA_REGISTRADO))
        val vm = AuthViewModel(repo)
        vm.register(registro)
        val evento = vm.events.first()
        assertTrue(evento is AuthEvent.RegistrationFailed)
        assertEquals(AuthError.CORREO_YA_REGISTRADO.mensaje, (evento as AuthEvent.RegistrationFailed).message)
    }

    @Test
    fun noEnviaDosVecesMientrasEstaCargando() {
        val repo = RepositorioFalso()
        val vm = AuthViewModel(repo)
        vm.update { it.copy(isSubmitting = true) }
        vm.login(credenciales)
        vm.register(registro)
        assertEquals(0, repo.llamadasSignIn)
        assertEquals(0, repo.llamadasRegister)
    }

    @Test
    fun goToLoginWithVuelveAlLoginConElCorreoYSinContrasena() {
        val vm = AuthViewModel(RepositorioFalso())
        vm.goToRegister()
        vm.goToLoginWith("ana@cafe.com")
        assertEquals(AuthPage.LOGIN, vm.uiState.page)
        assertEquals("ana@cafe.com", vm.uiState.loginEmail)
        assertEquals("", vm.uiState.loginPassword)
    }

    @Test
    fun signOutCierraSesionYLimpiaElFormulario() {
        val repo = RepositorioFalso()
        val vm = AuthViewModel(repo)
        vm.update { it.copy(loginEmail = "ana@cafe.com", loginPassword = "Secreto123") }
        vm.signOut()
        assertTrue(repo.cerrada)
        assertEquals("", vm.uiState.loginEmail)
        assertEquals("", vm.uiState.loginPassword)
    }
}
