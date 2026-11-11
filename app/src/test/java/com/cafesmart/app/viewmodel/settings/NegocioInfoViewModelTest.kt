package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.session.SessionGateway
import com.cafesmart.app.ui.components.AvisoUi
import com.cafesmart.app.ui.components.TipoAviso
import com.cafesmart.app.data.session.UsuarioSesion
import com.cafesmart.app.ui.auth.BusinessType
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

private class Sesion(private val u: UsuarioSesion?) : SessionGateway {
    override fun usuarioActual() = u
    override fun cerrarSesion() = Unit
    override suspend fun actualizarNombre(nombre: String): com.cafesmart.app.data.auth.AuthError? = null
}

private class Negocios(inicial: NegocioEntity? = null) : NegocioRepository {
    val datos = MutableStateFlow(listOfNotNull(inicial).associateBy { it.uid })
    var guardados = 0
    // true = guardar falla (por ejemplo, la base de datos no responde).
    var fallar = false
    override suspend fun guardar(negocio: NegocioEntity) {
        if (fallar) throw IllegalStateException("fallo simulado")
        guardados++
        datos.value = datos.value + (negocio.uid to negocio)
    }
    override fun observar(uid: String): Flow<NegocioEntity?> = datos.map { it[uid] }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NegocioInfoViewModelTest {

    private val andres = UsuarioSesion("u1", "Andrés", "andres@correo.com")
    private val existente = NegocioEntity(
        uid = "u1", nombre = "Café Viejo", tipo = "COOPERATIVA", telefono = "3110000000",
        capacidadBodegaKg = 3000.0, creadoEn = 5L
    )

    @Before fun preparar() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }

    @Test fun conNegocioGuardadoLlenaElFormulario() {
        val vm = NegocioInfoViewModel(Sesion(andres), Negocios(existente))
        assertFalse(vm.uiState.value.cargando)
        assertEquals(NegocioFormulario("Café Viejo", BusinessType.COOPERATIVA, "3110000000"), vm.uiState.value.form)
    }

    // El caso de la cuenta que perdió su Negocio: puede volver a completarlo desde Ajustes.
    @Test fun cuentaSinNegocioLoCompletaYQuedaGuardado() {
        val negocios = Negocios()
        val vm = NegocioInfoViewModel(Sesion(andres), negocios, reloj = { 7L })
        assertEquals(NegocioFormulario(), vm.uiState.value.form)
        vm.editar { NegocioFormulario("Café La Esperanza", BusinessType.COMPRAVENTA, "3001234567") }
        vm.guardar()
        assertTrue(vm.uiState.value.guardado)
        assertEquals("COMPRAVENTA", negocios.datos.value["u1"]?.tipo)
        assertEquals(7L, negocios.datos.value["u1"]?.creadoEn)
    }

    @Test fun conErroresNoGuardaYLosMuestra() {
        val negocios = Negocios()
        val vm = NegocioInfoViewModel(Sesion(andres), negocios)
        vm.guardar()
        assertEquals(0, negocios.guardados)
        assertFalse(vm.uiState.value.guardado)
        assertEquals(setOf("businessName", "businessType", "phone"), vm.uiState.value.errores.keys)
    }

    @Test fun antesDeGuardarNoMuestraErroresYAlCorregirDesaparecen() {
        val vm = NegocioInfoViewModel(Sesion(andres), Negocios())
        vm.editar { it.copy(nombre = "Café") }
        assertTrue(vm.uiState.value.errores.isEmpty())
        vm.guardar()
        vm.editar { it.copy(tipo = BusinessType.COMPRAVENTA, telefono = "3001234567") }
        assertTrue(vm.uiState.value.errores.isEmpty())
    }

    @Test fun editarConservaLaCapacidadDeBodega() {
        val negocios = Negocios(existente)
        val vm = NegocioInfoViewModel(Sesion(andres), negocios)
        vm.editar { it.copy(tipo = BusinessType.COMPRAVENTA) }
        vm.guardar()
        assertEquals(3000.0, negocios.datos.value["u1"]!!.capacidadBodegaKg, 0.0)
        assertEquals("COMPRAVENTA", negocios.datos.value["u1"]!!.tipo)
    }

    @Test fun sinSesionNoCargaNiGuarda() {
        val negocios = Negocios()
        val vm = NegocioInfoViewModel(Sesion(null), negocios)
        assertTrue(vm.uiState.value.sinSesion)
        vm.editar { NegocioFormulario("Café", BusinessType.COMPRAVENTA, "3001234567") }
        vm.guardar()
        assertEquals(0, negocios.guardados)
        assertNull(negocios.datos.value["u1"])
    }

    @Test fun siGuardarFallaMuestraElAvisoRojoYNoMarcaGuardado() {
        val negocios = Negocios().apply { fallar = true }
        val vm = NegocioInfoViewModel(Sesion(andres), negocios)
        vm.editar { NegocioFormulario("Café La Esperanza", BusinessType.COMPRAVENTA, "3001234567") }
        vm.guardar()
        assertFalse(vm.uiState.value.guardado)
        assertFalse(vm.uiState.value.guardando)
        assertEquals(AvisoUi(AVISO_NEGOCIO_ERROR, TipoAviso.ERROR), vm.uiState.value.aviso)
        vm.ocultarAviso()
        assertEquals(null, vm.uiState.value.aviso)
    }
}
