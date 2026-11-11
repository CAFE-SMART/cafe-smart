package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.finanzas.AlmacenClaveFinancieraEnMemoria
import com.cafesmart.app.data.finanzas.ConfirmacionCuenta
import com.cafesmart.app.data.finanzas.ERROR_CLAVES_DISTINTAS
import com.cafesmart.app.data.finanzas.ERROR_CLAVE_CORTA
import com.cafesmart.app.data.finanzas.ERROR_CLAVE_VACIA
import com.cafesmart.app.data.finanzas.ProveedorCuenta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Cuenta simulada: responde lo que se le diga, sin Firebase.
private class CuentaFalsa(
    var proveedor: ProveedorCuenta = ProveedorCuenta.CORREO,
    var contrasenaCorrecta: String = "ContrasenaCuenta1",
    var googleConfirma: Boolean = true,
    var error: AuthError? = null,
) : ConfirmacionCuenta {
    var intentos = 0
    override fun proveedor() = proveedor
    override suspend fun confirmarConContrasena(contrasena: String): AuthError? {
        intentos++
        return error ?: if (contrasena == contrasenaCorrecta) null else AuthError.CREDENCIALES_INCORRECTAS
    }
    override suspend fun confirmarConGoogle(): AuthError? = error ?: if (googleConfirma) null else AuthError.GOOGLE_NO_DISPONIBLE
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccesoFinancieroViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val almacen = AlmacenClaveFinancieraEnMemoria()
    private val cuenta = CuentaFalsa()

    @Before fun antes() { Dispatchers.setMain(dispatcher) }
    @After fun despues() { Dispatchers.resetMain() }

    // Reloj de prueba: se mueve a mano para probar la ventana de 15 minutos.
    private var ahora = 1_000_000L

    private fun vm(uid: String = "u1") = AccesoFinancieroViewModel(uid, almacen, cuenta, dispatcher) { ahora }

    private fun fallar(v: AccesoFinancieroViewModel, veces: Int) = repeat(veces) {
        v.cambiarClave("incorrecta"); v.ingresar()
    }

    private fun conClaveCreada(clave: String = "ClaveSegura1"): AccesoFinancieroViewModel {
        val v = vm()
        v.cambiarClave(clave); v.cambiarConfirmacion(clave); v.guardarClave()
        return vm()
    }

    // ── Primer acceso ──────────────────────────────────────

    @Test
    fun elPrimerAccesoPideCrearLaClave() {
        assertEquals(PasoAcceso.CREAR, vm().estado.value.paso)
    }

    @Test
    fun claveCortaOVaciaNoSeGuardaYMuestraElError() {
        val v = vm()
        v.guardarClave()
        assertEquals(ERROR_CLAVE_VACIA, v.estado.value.errorClave)
        v.cambiarClave("123"); v.cambiarConfirmacion("123"); v.guardarClave()
        assertEquals(ERROR_CLAVE_CORTA, v.estado.value.errorClave)
        assertNull(almacen.leer("u1"))
        assertFalse(v.estado.value.autorizado)
    }

    @Test
    fun confirmacionDistintaNoSeGuarda() {
        val v = vm()
        v.cambiarClave("ClaveSegura1"); v.cambiarConfirmacion("ClaveSegura2"); v.guardarClave()
        assertEquals(ERROR_CLAVES_DISTINTAS, v.estado.value.errorConfirmacion)
        assertNull(almacen.leer("u1"))
    }

    @Test
    fun creandoLaClaveSeGuardaSoloElHashYSeEntra() {
        val v = vm()
        v.cambiarClave("ClaveSegura1"); v.cambiarConfirmacion("ClaveSegura1"); v.guardarClave()
        assertTrue(v.estado.value.autorizado)
        val guardada = almacen.leer("u1")
        assertNotNull(guardada)
        assertFalse(guardada.toString().contains("ClaveSegura1"))
        // El estado ya no conserva la clave escrita.
        assertEquals("", v.estado.value.clave)
    }

    // ── Accesos siguientes ─────────────────────────────────

    @Test
    fun conClaveCreadaLosSiguientesAccesosLaPiden() {
        assertEquals(PasoAcceso.PEDIR, conClaveCreada().estado.value.paso)
    }

    @Test
    fun laClaveCorrectaDejaEntrar() {
        val v = conClaveCreada()
        v.cambiarClave("ClaveSegura1"); v.ingresar()
        assertTrue(v.estado.value.autorizado)
    }

    @Test
    fun laClaveIncorrectaNoDejaEntrarNiMuestraDatos() {
        val v = conClaveCreada()
        v.cambiarClave("otraclave"); v.ingresar()
        assertFalse(v.estado.value.autorizado)
        assertEquals(PasoAcceso.PEDIR, v.estado.value.paso)
        assertEquals(mensajeDeIntento(2), v.estado.value.errorClave)
        // Lo escrito se borra para que no quede a la vista.
        assertEquals("", v.estado.value.clave)
    }

    @Test
    fun sinEscribirNadaPideEscribirLaClave() {
        val v = conClaveCreada()
        v.ingresar()
        assertEquals(ERROR_CLAVE_VACIA_ACCESO, v.estado.value.errorClave)
        assertFalse(v.estado.value.autorizado)
    }

    @Test
    fun cadaCuentaTieneSuPropiaClave() {
        conClaveCreada()
        // Otra cuenta en el mismo celular no tiene clave: le toca crearla.
        assertEquals(PasoAcceso.CREAR, vm("u2").estado.value.paso)
    }

    @Test
    fun alEscribirDeNuevoSeQuitaElError() {
        val v = conClaveCreada()
        v.cambiarClave("x"); v.ingresar()
        v.cambiarClave("y")
        assertNull(v.estado.value.errorClave)
    }

    // ── Olvidé mi clave ────────────────────────────────────

    @Test
    fun olvidarLaClavePasaAConfirmarLaIdentidadConElProveedorDeLaCuenta() {
        val v = conClaveCreada()
        v.olvideMiClave()
        assertEquals(PasoAcceso.CONFIRMAR_IDENTIDAD, v.estado.value.paso)
        assertEquals(ProveedorCuenta.CORREO, v.estado.value.proveedor)
    }

    @Test
    fun confirmacionCorrectaConLaCuentaPermiteCrearUnaClaveNueva() {
        val v = conClaveCreada()
        v.olvideMiClave()
        v.cambiarContrasenaCuenta("ContrasenaCuenta1"); v.confirmarConContrasena()
        assertEquals(PasoAcceso.NUEVA_CLAVE, v.estado.value.paso)
        v.cambiarClave("ClaveNueva99"); v.cambiarConfirmacion("ClaveNueva99"); v.guardarClave()
        assertTrue(v.estado.value.autorizado)
        // La clave vieja ya no sirve y la nueva sí.
        val otra = vm()
        otra.cambiarClave("ClaveSegura1"); otra.ingresar()
        assertFalse(otra.estado.value.autorizado)
        otra.cambiarClave("ClaveNueva99"); otra.ingresar()
        assertTrue(otra.estado.value.autorizado)
    }

    @Test
    fun confirmacionIncorrectaNoCambiaNada() {
        val v = conClaveCreada()
        val antes = almacen.leer("u1")
        v.olvideMiClave()
        v.cambiarContrasenaCuenta("mala"); v.confirmarConContrasena()
        assertEquals(PasoAcceso.CONFIRMAR_IDENTIDAD, v.estado.value.paso)
        assertEquals(ERROR_CONTRASENA_CUENTA_INCORRECTA, v.estado.value.errorCuenta)
        assertEquals("", v.estado.value.contrasenaCuenta)
        assertEquals(antes, almacen.leer("u1"))
        assertFalse(v.estado.value.autorizado)
    }

    @Test
    fun contrasenaDeCuentaVaciaPideEscribirla() {
        val v = conClaveCreada()
        v.olvideMiClave()
        v.confirmarConContrasena()
        assertEquals(ERROR_CONTRASENA_CUENTA_VACIA, v.estado.value.errorCuenta)
        assertEquals(0, cuenta.intentos)
    }

    @Test
    fun sinInternetMuestraElMensajeYNoAvanza() {
        cuenta.error = AuthError.SIN_CONEXION
        val v = conClaveCreada()
        v.olvideMiClave()
        v.cambiarContrasenaCuenta("ContrasenaCuenta1"); v.confirmarConContrasena()
        assertEquals(AuthError.SIN_CONEXION.mensaje, v.estado.value.errorCuenta)
        assertEquals(PasoAcceso.CONFIRMAR_IDENTIDAD, v.estado.value.paso)
    }

    @Test
    fun cuentaDeGoogleConfirmadaPermiteCrearLaClaveNueva() {
        cuenta.proveedor = ProveedorCuenta.GOOGLE
        val v = conClaveCreada()
        v.olvideMiClave()
        assertEquals(ProveedorCuenta.GOOGLE, v.estado.value.proveedor)
        v.confirmarConGoogle()
        assertEquals(PasoAcceso.NUEVA_CLAVE, v.estado.value.paso)
    }

    @Test
    fun cuentaDeGoogleNoConfirmadaNoCambiaNada() {
        cuenta.proveedor = ProveedorCuenta.GOOGLE
        cuenta.googleConfirma = false
        val v = conClaveCreada()
        val antes = almacen.leer("u1")
        v.olvideMiClave()
        v.confirmarConGoogle()
        assertEquals(PasoAcceso.CONFIRMAR_IDENTIDAD, v.estado.value.paso)
        assertEquals(AuthError.GOOGLE_NO_DISPONIBLE.mensaje, v.estado.value.errorCuenta)
        assertEquals(antes, almacen.leer("u1"))
    }

    @Test
    fun cancelarElOlvidoVuelveAPedirLaClave() {
        val v = conClaveCreada()
        v.olvideMiClave()
        v.cancelarOlvido()
        assertEquals(PasoAcceso.PEDIR, v.estado.value.paso)
    }

    @Test
    fun noSePuedeSaltarAlPasoDeClaveNuevaSinConfirmar() {
        val v = conClaveCreada()
        v.cambiarClave("ClaveNueva99"); v.cambiarConfirmacion("ClaveNueva99")
        v.guardarClave()
        // Estando en PEDIR, guardarClave no hace nada: la clave guardada sigue siendo la vieja.
        assertEquals(PasoAcceso.PEDIR, v.estado.value.paso)
        val otra = vm()
        otra.cambiarClave("ClaveSegura1"); otra.ingresar()
        assertTrue(otra.estado.value.autorizado)
    }

    // ── Límite de 3 intentos en 15 minutos ─────────────────

    @Test
    fun cadaFalloDiceCuantosIntentosQuedan() {
        val v = conClaveCreada()
        fallar(v, 1)
        assertEquals(mensajeDeIntento(2), v.estado.value.errorClave)
        fallar(v, 1)
        assertEquals(mensajeDeIntento(1), v.estado.value.errorClave)
    }

    @Test
    fun alTercerFalloSeBloqueaYYaNoDejaEntrarNiConLaClaveCorrecta() {
        val v = conClaveCreada()
        fallar(v, 3)
        assertTrue(v.estado.value.errorClave!!.startsWith("Por seguridad, espera 5 minutos"))
        // Aunque escriba la clave correcta, mientras dure el bloqueo no entra.
        v.cambiarClave("ClaveSegura1"); v.ingresar()
        assertFalse(v.estado.value.autorizado)
        assertTrue(v.estado.value.errorClave!!.startsWith("Por seguridad"))
    }

    @Test
    fun elBloqueoDiceCuantoFalta() {
        val v = conClaveCreada()
        fallar(v, 3)
        ahora += 3 * 60_000L
        v.cambiarClave("ClaveSegura1"); v.ingresar()
        assertTrue(v.estado.value.errorClave!!.contains("espera 2 minutos"))
    }

    @Test
    fun pasados5MinutosPuedeVolverAIntentar() {
        val v = conClaveCreada()
        fallar(v, 3)
        ahora += 5 * 60_000L
        v.cambiarClave("ClaveSegura1"); v.ingresar()
        assertTrue(v.estado.value.autorizado)
    }

    @Test
    fun tresFallosRepartidosEnMasDe15MinutosNoBloquean() {
        val v = conClaveCreada()
        fallar(v, 2)
        ahora += 16 * 60_000L
        fallar(v, 1)
        assertEquals(mensajeDeIntento(2), v.estado.value.errorClave)
    }

    @Test
    fun unAccesoCorrectoBorraLosFallos() {
        val v = conClaveCreada()
        fallar(v, 2)
        v.cambiarClave("ClaveSegura1"); v.ingresar()
        assertTrue(v.estado.value.autorizado)
        assertTrue(almacen.leerFallos("u1").isEmpty())
    }

    @Test
    fun elBloqueoSobreviveACerrarYAbrirLaPantalla() {
        fallar(conClaveCreada(), 3)
        // Una pantalla nueva (otra instancia) ya sabe que está bloqueado.
        val nueva = vm()
        assertTrue(nueva.estado.value.errorClave!!.startsWith("Por seguridad"))
        nueva.cambiarClave("ClaveSegura1"); nueva.ingresar()
        assertFalse(nueva.estado.value.autorizado)
    }

    @Test
    fun creandoUnaClaveNuevaConLaCuentaSeLimpianLosFallosYSeEntra() {
        val v = conClaveCreada()
        fallar(v, 3)
        v.olvideMiClave()
        v.cambiarContrasenaCuenta("ContrasenaCuenta1"); v.confirmarConContrasena()
        v.cambiarClave("ClaveNueva99"); v.cambiarConfirmacion("ClaveNueva99"); v.guardarClave()
        assertTrue(v.estado.value.autorizado)
        assertTrue(almacen.leerFallos("u1").isEmpty())
        val otra = vm()
        otra.cambiarClave("ClaveNueva99"); otra.ingresar()
        assertTrue(otra.estado.value.autorizado)
    }

    @Test
    fun elBloqueoNoImpideRecuperarLaClaveConLaCuenta() {
        val v = conClaveCreada()
        fallar(v, 3)
        v.olvideMiClave()
        assertEquals(PasoAcceso.CONFIRMAR_IDENTIDAD, v.estado.value.paso)
    }

    @Test
    fun despuesDelBloqueoLaPersonaTieneTresIntentosNuevos() {
        val v = conClaveCreada()
        fallar(v, 3)
        ahora += 5 * 60_000L
        fallar(v, 1)
        assertEquals(mensajeDeIntento(2), v.estado.value.errorClave)
    }

    @Test fun laClaveNoPasaDe32CaracteresYLaContrasenaDeLaCuentaDe64() {
        val m = vm()
        m.cambiarClave("a".repeat(40))
        m.cambiarConfirmacion("b".repeat(40))
        m.cambiarContrasenaCuenta("c".repeat(100))
        assertEquals(com.cafesmart.app.data.finanzas.CLAVE_MAX_CARACTERES, m.estado.value.clave.length)
        assertEquals(com.cafesmart.app.data.finanzas.CLAVE_MAX_CARACTERES, m.estado.value.confirmacion.length)
        assertEquals(com.cafesmart.app.ui.auth.MAX_CONTRASENA, m.estado.value.contrasenaCuenta.length)
    }
}
