package com.cafesmart.app.ui.auth

import com.cafesmart.app.data.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorDialogDataTest {

    @Test
    fun sinConexionUsaElIconoDeRedYPermiteReintentarEnLoginYRegistro() {
        AuthOrigin.entries.forEach { origen ->
            val d = authErrorDialogData(AuthError.SIN_CONEXION, origen)
            assertEquals("Sin conexión", d.title)
            assertTrue(d.sinConexion)
            assertEquals("Reintentar", d.primaryText)
            assertEquals(AuthErrorAction.REINTENTAR, d.primaryAction)
            assertEquals("Regresar", d.secondaryText)
            assertEquals(AuthErrorAction.CERRAR, d.secondaryAction)
            assertEquals(origen, d.origin)
        }
    }

    @Test
    fun correoYaRegistradoOfreceIniciarSesionOCambiarElCorreo() {
        val d = authErrorDialogData(AuthError.CORREO_YA_REGISTRADO, AuthOrigin.REGISTRO)
        assertEquals("No pudimos crear tu cuenta", d.title)
        assertFalse(d.sinConexion)
        assertEquals("Iniciar sesión", d.primaryText)
        assertEquals(AuthErrorAction.IR_A_LOGIN, d.primaryAction)
        assertEquals("Cambiar correo", d.secondaryText)
        assertEquals(AuthErrorAction.CERRAR, d.secondaryAction)
    }

    @Test
    fun credencialesIncorrectasTienenTituloPropioYNoDicenCualDatoFallo() {
        val d = authErrorDialogData(AuthError.CREDENCIALES_INCORRECTAS, AuthOrigin.LOGIN)
        assertEquals("No pudimos iniciar sesión", d.title)
        assertTrue(d.message.startsWith("Correo o contraseña incorrectos"))
        assertEquals(AuthErrorAction.CERRAR, d.primaryAction)
        assertEquals("Crear cuenta", d.secondaryText)
        assertEquals(AuthErrorAction.IR_A_REGISTRO, d.secondaryAction)
    }

    @Test
    fun errorDesconocidoSePuedeReintentar() {
        val d = authErrorDialogData(AuthError.DESCONOCIDO, AuthOrigin.LOGIN)
        assertEquals(AuthErrorAction.REINTENTAR, d.primaryAction)
        assertEquals("Regresar", d.secondaryText)
    }

    @Test
    fun demasiadosIntentosTieneUnSoloBotonQueSoloCierra() {
        val d = authErrorDialogData(AuthError.DEMASIADOS_INTENTOS, AuthOrigin.LOGIN)
        assertEquals("Regresar", d.primaryText)
        assertEquals(AuthErrorAction.CERRAR, d.primaryAction)
        assertNull(d.secondaryText)
    }

    @Test
    fun contrasenaDebilEnRegistroVuelveAlFormulario() {
        val d = authErrorDialogData(AuthError.CONTRASENA_DEBIL, AuthOrigin.REGISTRO)
        assertEquals("Volver al formulario", d.primaryText)
        assertNull(d.secondaryText)
    }

    @Test
    fun todaCombinacionTieneTituloMensajeYBotonPrincipal() {
        AuthError.entries.forEach { error ->
            AuthOrigin.entries.forEach { origen ->
                val d = authErrorDialogData(error, origen)
                assertTrue("$error/$origen sin título", d.title.isNotBlank())
                assertEquals(error.mensaje, d.message)
                assertTrue("$error/$origen sin botón", d.primaryText.isNotBlank())
                // Si hay segundo botón, debe tener texto.
                assertTrue(d.secondaryText?.isNotBlank() != false)
            }
        }
    }
}
