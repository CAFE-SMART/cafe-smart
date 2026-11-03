package com.cafesmart.app.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de las validaciones del inicio de sesión:
// - AuthValidation.login: reglas de correo y contraseña.
// - AuthViewModel.submitLogin: solo entrega las credenciales si son válidas.
class LoginValidationTest {

    // ── AuthValidation.login ─────────────────────────────────

    @Test
    fun credencialesValidasNoDevuelvenErrores() {
        val errores = AuthValidation.login("ana@cafe.com", "secreto123")

        assertTrue(errores.isEmpty())
    }

    @Test
    fun correoVacioDevuelveError() {
        val errores = AuthValidation.login("", "secreto123")

        assertEquals("Ingresa un correo electrónico válido.", errores["loginEmail"])
        assertFalse(errores.containsKey("loginPassword"))
    }

    @Test
    fun correoSoloConEspaciosDevuelveError() {
        val errores = AuthValidation.login("   ", "secreto123")

        assertTrue(errores.containsKey("loginEmail"))
    }

    @Test
    fun correoSinArrobaDevuelveError() {
        val errores = AuthValidation.login("anacafe.com", "secreto123")

        assertTrue(errores.containsKey("loginEmail"))
    }

    @Test
    fun correoSinDominioDevuelveError() {
        val errores = AuthValidation.login("ana@", "secreto123")

        assertTrue(errores.containsKey("loginEmail"))
    }

    @Test
    fun correoSinPuntoEnElDominioDevuelveError() {
        val errores = AuthValidation.login("ana@cafe", "secreto123")

        assertTrue(errores.containsKey("loginEmail"))
    }

    @Test
    fun correoConEspaciosInternosDevuelveError() {
        val errores = AuthValidation.login("ana perez@cafe.com", "secreto123")

        assertTrue(errores.containsKey("loginEmail"))
    }

    @Test
    fun correoValidoConEspaciosAlrededorSeAcepta() {
        // La validación recorta los espacios antes de comprobar el formato.
        val errores = AuthValidation.login("  ana@cafe.com  ", "secreto123")

        assertTrue(errores.isEmpty())
    }

    @Test
    fun contrasenaVaciaDevuelveError() {
        val errores = AuthValidation.login("ana@cafe.com", "")

        assertEquals("Escribe tu contraseña.", errores["loginPassword"])
        assertFalse(errores.containsKey("loginEmail"))
    }

    @Test
    fun contrasenaSoloConEspaciosDevuelveError() {
        val errores = AuthValidation.login("ana@cafe.com", "     ")

        assertTrue(errores.containsKey("loginPassword"))
    }

    @Test
    fun correoYContrasenaInvalidosDevuelvenAmbosErrores() {
        val errores = AuthValidation.login("", "")

        assertEquals(2, errores.size)
        assertTrue(errores.containsKey("loginEmail"))
        assertTrue(errores.containsKey("loginPassword"))
    }

    // ── AuthViewModel.submitLogin ────────────────────────────

    @Test
    fun submitLoginConDatosInvalidosNoEntregaCredencialesYMarcaErrores() {
        val viewModel = AuthViewModel()
        var credenciales: LoginCredentials? = null

        viewModel.submitLogin { credenciales = it }

        assertNull(credenciales)
        assertTrue(viewModel.uiState.errors.containsKey("loginEmail"))
        assertTrue(viewModel.uiState.errors.containsKey("loginPassword"))
    }

    @Test
    fun submitLoginConDatosValidosEntregaCredencialesSinEspacios() {
        val viewModel = AuthViewModel()
        viewModel.update { it.copy(loginEmail = "  ana@cafe.com  ", loginPassword = "secreto123") }
        var credenciales: LoginCredentials? = null

        viewModel.submitLogin { credenciales = it }

        assertEquals("ana@cafe.com", credenciales?.email)
        assertEquals("secreto123", credenciales?.password)
        assertTrue(viewModel.uiState.errors.isEmpty())
    }

    @Test
    fun submitLoginEntregaLaOpcionRecordarme() {
        val viewModel = AuthViewModel()
        viewModel.update {
            it.copy(loginEmail = "ana@cafe.com", loginPassword = "secreto123", rememberMe = false)
        }
        var credenciales: LoginCredentials? = null

        viewModel.submitLogin { credenciales = it }

        assertEquals(false, credenciales?.rememberMe)
    }

    @Test
    fun resetLimpiaLosDatosDelLogin() {
        val viewModel = AuthViewModel()
        viewModel.update { it.copy(loginEmail = "ana@cafe.com", loginPassword = "secreto123") }

        viewModel.reset()

        assertEquals("", viewModel.uiState.loginEmail)
        assertEquals("", viewModel.uiState.loginPassword)
    }
}
