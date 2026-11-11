package com.cafesmart.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorTest {

    @Test
    fun contrasenaErradaYUsuarioInexistenteMuestranElMismoMensaje() {
        assertEquals(AuthError.CREDENCIALES_INCORRECTAS, authErrorDesdeCodigo("ERROR_WRONG_PASSWORD"))
        assertEquals(AuthError.CREDENCIALES_INCORRECTAS, authErrorDesdeCodigo("ERROR_USER_NOT_FOUND"))
        assertEquals(AuthError.CREDENCIALES_INCORRECTAS, authErrorDesdeCodigo("ERROR_INVALID_CREDENTIAL"))
    }

    @Test
    fun correoYaRegistrado() {
        assertEquals(AuthError.CORREO_YA_REGISTRADO, authErrorDesdeCodigo("ERROR_EMAIL_ALREADY_IN_USE"))
    }

    @Test
    fun contrasenaDebil() {
        assertEquals(AuthError.CONTRASENA_DEBIL, authErrorDesdeCodigo("ERROR_WEAK_PASSWORD"))
    }

    @Test
    fun sinConexionYDemasiadosIntentos() {
        assertEquals(AuthError.SIN_CONEXION, authErrorDesdeCodigo("ERROR_NETWORK_REQUEST_FAILED"))
        assertEquals(AuthError.DEMASIADOS_INTENTOS, authErrorDesdeCodigo("ERROR_TOO_MANY_REQUESTS"))
    }

    @Test
    fun codigoDesconocidoONuloDaMensajeGenerico() {
        assertEquals(AuthError.DESCONOCIDO, authErrorDesdeCodigo("ERROR_RARO"))
        assertEquals(AuthError.DESCONOCIDO, authErrorDesdeCodigo(null))
    }

    @Test
    fun todosLosErroresTienenMensajeEnEspanolNoVacio() {
        AuthError.entries.forEach { assert(it.mensaje.isNotBlank()) }
    }

    @Test
    fun alRecuperarContrasenaUnCorreoInexistenteSeDiceClaro() {
        assertEquals(AuthError.CORREO_NO_REGISTRADO, authErrorRecuperacionDesdeCodigo("ERROR_USER_NOT_FOUND"))
        assertEquals(AuthError.CORREO_INVALIDO, authErrorRecuperacionDesdeCodigo("ERROR_INVALID_EMAIL"))
        assertEquals(AuthError.SIN_CONEXION, authErrorRecuperacionDesdeCodigo("ERROR_NETWORK_REQUEST_FAILED"))
        assertEquals(AuthError.DESCONOCIDO, authErrorRecuperacionDesdeCodigo(null))
    }

    @Test
    fun alIniciarSesionUnCorreoInexistenteSigueSinDelatarse() {
        assertEquals(AuthError.CREDENCIALES_INCORRECTAS, authErrorDesdeCodigo("ERROR_USER_NOT_FOUND"))
    }
}
