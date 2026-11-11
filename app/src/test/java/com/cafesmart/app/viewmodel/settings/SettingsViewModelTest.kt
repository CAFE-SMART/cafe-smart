package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.auth.AuthError
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.session.SessionGateway
import com.cafesmart.app.data.session.UsuarioSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

// Versión falsa de la sesión: permite probar sin Firebase y contar los cierres de sesión.
private class SesionFalsa(var usuario: UsuarioSesion?) : SessionGateway {
    var cierres = 0
    // Lo que responde actualizarNombre: null = salió bien, o el error de Firebase.
    var errorAlCambiarNombre: AuthError? = null
    val nombresGuardados = mutableListOf<String>()
    override suspend fun actualizarNombre(nombre: String): AuthError? {
        nombresGuardados += nombre
        if (errorAlCambiarNombre == null) usuario = usuario?.copy(nombre = nombre)
        return errorAlCambiarNombre
    }
    override fun usuarioActual(): UsuarioSesion? = usuario
    override fun cerrarSesion() {
        cierres++
        usuario = null
    }
}

// Versión falsa del repositorio: guarda en memoria y avisa los cambios como Room.
private class NegociosFalsos : NegocioRepository {
    val datos = MutableStateFlow<Map<String, NegocioEntity>>(emptyMap())
    var consultas = 0
    override suspend fun guardar(negocio: NegocioEntity) = datos.update(negocio)
    override fun observar(uid: String): Flow<NegocioEntity?> {
        consultas++
        return datos.map { it[uid] }
    }
    private fun MutableStateFlow<Map<String, NegocioEntity>>.update(n: NegocioEntity) { value = value + (n.uid to n) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val andres = UsuarioSesion("u1", "Andrés Peña", "andres@correo.com")
    private val negocioAndres =
        NegocioEntity(uid = "u1", nombre = "Café La Esperanza", tipo = "COMPRAVENTA", telefono = "3001234567", creadoEn = 0L)

    @Before fun preparar() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }

    @Test
    fun alEmpezarMuestraNombreYCorreoSinConfirmacion() {
        val vm = SettingsViewModel(SesionFalsa(andres), NegociosFalsos())
        assertEquals("Andrés Peña", vm.uiState.value.perfil.nombre)
        assertEquals("andres@correo.com", vm.uiState.value.perfil.correo)
        assertFalse(vm.uiState.value.mostrarConfirmacion)
    }

    @Test
    fun conNegocioGuardadoMuestraTipoYNombreDelNegocio() {
        val negocios = NegociosFalsos().apply { datos.value = mapOf("u1" to negocioAndres) }
        val vm = SettingsViewModel(SesionFalsa(andres), negocios)
        assertEquals(PerfilUsuario("Andrés Peña", "andres@correo.com", "Compraventa", "Café La Esperanza"), vm.uiState.value.perfil)
    }

    @Test
    fun siElNegocioSeGuardaDespuesLaPantallaSeActualiza() {
        val negocios = NegociosFalsos()
        val vm = SettingsViewModel(SesionFalsa(andres), negocios)
        assertNull(vm.uiState.value.perfil.tipoNegocio)
        negocios.datos.value = mapOf("u1" to negocioAndres)
        assertEquals("Compraventa", vm.uiState.value.perfil.tipoNegocio)
    }

    @Test
    fun noMuestraElNegocioDeOtraCuenta() {
        val negocios = NegociosFalsos().apply { datos.value = mapOf("otro" to negocioAndres.copy(uid = "otro")) }
        val vm = SettingsViewModel(SesionFalsa(andres), negocios)
        assertNull(vm.uiState.value.perfil.tipoNegocio)
    }

    @Test
    fun sinSesionNoConsultaElNegocioYElPerfilQuedaVacio() {
        val negocios = NegociosFalsos()
        val vm = SettingsViewModel(SesionFalsa(null), negocios)
        assertEquals(PerfilUsuario(), vm.uiState.value.perfil)
        assertEquals(0, negocios.consultas)
    }

    @Test
    fun pedirCerrarSesionAbreLaConfirmacionSinCerrarLaSesion() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.pedirCerrarSesion()
        assertTrue(vm.uiState.value.mostrarConfirmacion)
        assertEquals(0, sesion.cierres)
    }

    @Test
    fun cancelarCierraLaConfirmacionYNoCierraLaSesion() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.pedirCerrarSesion()
        vm.cancelarCierre()
        assertFalse(vm.uiState.value.mostrarConfirmacion)
        assertEquals(0, sesion.cierres)
    }

    // El signOut lo hace solo cerrarSesion() de AppNavHost; antes se llamaba dos veces.
    @Test
    fun confirmarOcultaLaHojaYNoLlamaSignOut() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.pedirCerrarSesion()
        vm.confirmarCierre()
        assertFalse(vm.uiState.value.mostrarConfirmacion)
        assertEquals(0, sesion.cierres)
    }

    @Test
    fun sePuedePedirCancelarYVolverAPedir() {
        val vm = SettingsViewModel(SesionFalsa(andres), NegociosFalsos())
        vm.pedirCerrarSesion()
        vm.cancelarCierre()
        vm.pedirCerrarSesion()
        assertTrue(vm.uiState.value.mostrarConfirmacion)
    }

    @Test
    fun elEstadoInicialPorDefectoNoTieneConfirmacionNiPerfil() {
        val estado = SettingsUiState()
        assertFalse(estado.mostrarConfirmacion)
        assertEquals(PerfilUsuario(), estado.perfil)
    }

    // ── Editar perfil ──────────────────────────────────────

    @Test
    fun abrirEditarPerfilMuestraElNombreActualEnLaHoja() {
        val vm = SettingsViewModel(SesionFalsa(andres), NegociosFalsos())
        vm.abrirEditarPerfil()
        assertEquals(EdicionPerfil(nombre = "Andrés Peña"), vm.uiState.value.edicion)
    }

    @Test
    fun sinSesionNoSeAbreLaHojaDeEditar() {
        val vm = SettingsViewModel(SesionFalsa(null), NegociosFalsos())
        vm.abrirEditarPerfil()
        assertNull(vm.uiState.value.edicion)
    }

    @Test
    fun cancelarCierraLaHojaSinGuardar() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.abrirEditarPerfil()
        vm.cambiarNombre("Otro Nombre")
        vm.cancelarEdicion()
        assertNull(vm.uiState.value.edicion)
        assertTrue(sesion.nombresGuardados.isEmpty())
        assertEquals("Andrés Peña", vm.uiState.value.perfil.nombre)
    }

    @Test
    fun nombreSinApellidoMuestraErrorYNoGuarda() {
        val sesion = SesionFalsa(andres)
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.abrirEditarPerfil()
        vm.cambiarNombre("Andrés")
        vm.guardarNombre()
        assertEquals(ERROR_APELLIDOS_VACIOS, vm.uiState.value.edicion?.error)
        assertTrue(sesion.nombresGuardados.isEmpty())
    }

    @Test
    fun escribirDeNuevoQuitaElError() {
        val vm = SettingsViewModel(SesionFalsa(andres), NegociosFalsos())
        vm.abrirEditarPerfil()
        vm.cambiarNombre("")
        vm.guardarNombre()
        vm.cambiarNombre("A")
        assertNull(vm.uiState.value.edicion?.error)
    }

    @Test
    fun guardarNombreValidoLoNormalizaCierraLaHojaYActualizaElPerfil() {
        val sesion = SesionFalsa(andres)
        val negocios = NegociosFalsos().apply { datos.value = mapOf("u1" to negocioAndres) }
        val vm = SettingsViewModel(sesion, negocios)
        vm.abrirEditarPerfil()
        vm.cambiarNombre("  Juan   Pérez  ")
        vm.guardarNombre()
        assertEquals(listOf("Juan Pérez"), sesion.nombresGuardados)
        assertNull(vm.uiState.value.edicion)
        // El perfil conserva el correo y el negocio, y muestra el nombre nuevo.
        assertEquals(PerfilUsuario("Juan Pérez", "andres@correo.com", "Compraventa", "Café La Esperanza"), vm.uiState.value.perfil)
    }

    @Test
    fun siFirebaseFallaLaHojaSigueAbiertaConElMensaje() {
        val sesion = SesionFalsa(andres).apply { errorAlCambiarNombre = AuthError.SIN_CONEXION }
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.abrirEditarPerfil()
        vm.cambiarNombre("Juan Pérez")
        vm.guardarNombre()
        val edicion = vm.uiState.value.edicion
        assertEquals(AuthError.SIN_CONEXION.mensaje, edicion?.error)
        assertFalse(edicion!!.guardando)
        assertEquals("Juan Pérez", edicion.nombre)
        assertEquals("Andrés Peña", vm.uiState.value.perfil.nombre)
    }

    @Test
    fun alGuardarElNombreApareceElAvisoDeExitoYSeOculta() {
        val vm = SettingsViewModel(SesionFalsa(andres), NegociosFalsos())
        assertNull(vm.uiState.value.aviso)
        vm.abrirEditarPerfil()
        vm.cambiarNombre("Juan Pérez")
        vm.guardarNombre()
        assertEquals(AvisoUi(AVISO_NOMBRE_ACTUALIZADO, TipoAviso.EXITO), vm.uiState.value.aviso)
        vm.ocultarAviso()
        assertNull(vm.uiState.value.aviso)
    }

    @Test
    fun siElGuardadoFallaApareceElAvisoRojoYNoElVerde() {
        val sesion = SesionFalsa(andres).apply { errorAlCambiarNombre = AuthError.SIN_CONEXION }
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.abrirEditarPerfil()
        vm.cambiarNombre("Juan Pérez")
        vm.guardarNombre()
        assertEquals(AvisoUi(AVISO_NOMBRE_ERROR, TipoAviso.ERROR), vm.uiState.value.aviso)
    }

    @Test
    fun cancelarLaHojaBorraElAvisoRojoPendiente() {
        val sesion = SesionFalsa(andres).apply { errorAlCambiarNombre = AuthError.SIN_CONEXION }
        val vm = SettingsViewModel(sesion, NegociosFalsos())
        vm.abrirEditarPerfil()
        vm.cambiarNombre("Juan Pérez")
        vm.guardarNombre()
        vm.cancelarEdicion()
        assertNull(vm.uiState.value.edicion)
        assertNull(vm.uiState.value.aviso)
    }

    @Test
    fun unAvisoQueDejaOtraPantallaSeMuestraYSeOculta() {
        val vm = SettingsViewModel(SesionFalsa(andres), NegociosFalsos())
        vm.mostrarAviso(AvisoUi("Datos del negocio guardados"))
        assertEquals(AvisoUi("Datos del negocio guardados", TipoAviso.EXITO), vm.uiState.value.aviso)
        vm.ocultarAviso()
        assertNull(vm.uiState.value.aviso)
    }
}
