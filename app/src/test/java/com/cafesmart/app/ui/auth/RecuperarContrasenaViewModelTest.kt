package com.cafesmart.app.ui.auth

import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.auth.AuthRepository
import com.cafesmart.app.data.auth.AuthResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Firebase simulado: guarda a qué correos se les pidió el enlace y responde lo que le digamos. */
private class RepositorioRecuperacionFalso(
    var resultado: AuthResult = AuthResult.Success
) : AuthRepository {
    val correosPedidos = mutableListOf<String>()
    // Si no es null, la respuesta se queda esperando hasta completarlo (para ver el estado "enviando").
    var espera: CompletableDeferred<Unit>? = null

    override suspend fun sendPasswordReset(email: String): AuthResult {
        correosPedidos += email
        espera?.await()
        return resultado
    }

    override suspend fun signIn(email: String, password: String): AuthResult = error("no se usa")
    override suspend fun register(email: String, password: String, displayName: String): AuthResult = error("no se usa")
    override fun signOut() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarContrasenaViewModelTest {

    @Before
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun despues() {
        Dispatchers.resetMain()
    }

    @Test
    fun correoVacioPideEscribirloYNoLlamaAFirebase() {
        val repo = RepositorioRecuperacionFalso()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.enviar()
        assertEquals(ERROR_CORREO_VACIO, vm.state.value.error)
        assertTrue(repo.correosPedidos.isEmpty())
    }

    @Test
    fun correoConFormatoMaloMuestraErrorYNoLlamaAFirebase() {
        val repo = RepositorioRecuperacionFalso()
        val vm = RecuperarContrasenaViewModel(repo)
        for (malo in listOf("ana", "ana@", "ana@cafe", "ana cafe@correo.com", "@correo.com")) {
            vm.cambiarCorreo(malo)
            vm.enviar()
            assertEquals("con $malo", ERROR_CORREO_INVALIDO, vm.state.value.error)
        }
        assertTrue(repo.correosPedidos.isEmpty())
    }

    @Test
    fun escribirDeNuevoQuitaElError() {
        val vm = RecuperarContrasenaViewModel(RepositorioRecuperacionFalso())
        vm.enviar()
        vm.cambiarCorreo("a")
        assertNull(vm.state.value.error)
    }

    @Test
    fun envioExitosoMuestraLaConfirmacionConElCorreoLimpio() {
        val repo = RepositorioRecuperacionFalso()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.cambiarCorreo("  ana@cafe.com ")
        vm.enviar()
        assertEquals(listOf("ana@cafe.com"), repo.correosPedidos)
        assertEquals("ana@cafe.com", vm.state.value.enviadoA)
        assertFalse(vm.state.value.enviando)
        assertNull(vm.state.value.error)
    }

    @Test
    fun mientrasEsperaMarcaEnviandoYNoPermiteUnSegundoEnvio() {
        val repo = RepositorioRecuperacionFalso().apply { espera = CompletableDeferred() }
        val vm = RecuperarContrasenaViewModel(repo)
        vm.cambiarCorreo("ana@cafe.com")
        vm.enviar()
        assertTrue(vm.state.value.enviando)
        vm.enviar()
        assertEquals(1, repo.correosPedidos.size)
        repo.espera!!.complete(Unit)
        assertFalse(vm.state.value.enviando)
        assertEquals("ana@cafe.com", vm.state.value.enviadoA)
    }

    @Test
    fun correoNoRegistradoMuestraSuMensajeYNoPasaALaConfirmacion() {
        val repo = RepositorioRecuperacionFalso(AuthResult.Failure(AuthError.CORREO_NO_REGISTRADO))
        val vm = RecuperarContrasenaViewModel(repo)
        vm.cambiarCorreo("nadie@cafe.com")
        vm.enviar()
        assertEquals(AuthError.CORREO_NO_REGISTRADO.mensaje, vm.state.value.error)
        assertNull(vm.state.value.enviadoA)
        assertFalse(vm.state.value.enviando)
    }

    @Test
    fun fallaDeRedMuestraElMensajeDeSinConexionYConservaElCorreo() {
        val repo = RepositorioRecuperacionFalso(AuthResult.Failure(AuthError.SIN_CONEXION))
        val vm = RecuperarContrasenaViewModel(repo)
        vm.cambiarCorreo("ana@cafe.com")
        vm.enviar()
        assertEquals(AuthError.SIN_CONEXION.mensaje, vm.state.value.error)
        assertEquals("ana@cafe.com", vm.state.value.correo)
    }

    @Test
    fun reenviarPideOtraVezElEnlaceAlMismoCorreo() {
        val repo = RepositorioRecuperacionFalso()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.cambiarCorreo("ana@cafe.com")
        vm.enviar()
        vm.reenviar()
        assertEquals(listOf("ana@cafe.com", "ana@cafe.com"), repo.correosPedidos)
    }

    @Test
    fun reenviarSinHaberEnviadoNoHaceNada() {
        val repo = RepositorioRecuperacionFalso()
        val vm = RecuperarContrasenaViewModel(repo)
        vm.reenviar()
        assertTrue(repo.correosPedidos.isEmpty())
    }

    @Test
    fun reiniciarDejaLaPantallaComoNueva() {
        val vm = RecuperarContrasenaViewModel(RepositorioRecuperacionFalso())
        vm.cambiarCorreo("ana@cafe.com")
        vm.enviar()
        vm.reiniciar()
        assertEquals(RecuperarContrasenaUiState(), vm.state.value)
    }

    @Test
    fun validacionDeCorreo() {
        assertNull(errorDeCorreoParaRecuperar("ana@cafe.com"))
        assertNull(errorDeCorreoParaRecuperar("  ana@cafe.com  "))
        assertEquals(ERROR_CORREO_VACIO, errorDeCorreoParaRecuperar("   "))
        assertEquals(ERROR_CORREO_INVALIDO, errorDeCorreoParaRecuperar("ana@cafe"))
    }
}
