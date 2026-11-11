package com.cafesmart.app.viewmodel.settings

import com.cafesmart.app.data.inventario.OrigenOcupacion
import com.cafesmart.app.data.local.NegocioEntity
import com.cafesmart.app.data.negocio.NegocioRepository
import com.cafesmart.app.data.session.SessionGateway
import com.cafesmart.app.data.session.UsuarioSesion
import com.cafesmart.app.ui.components.TipoAviso
import com.cafesmart.app.viewmodel.inventory.InventoryViewModel
import com.cafesmart.app.viewmodel.inventory.EstadoBodega
import com.cafesmart.app.viewmodel.inventory.KG_POR_BULTO
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

private class SesionCap(private val u: UsuarioSesion?) : SessionGateway {
    override fun usuarioActual() = u
    override fun cerrarSesion() = Unit
    override suspend fun actualizarNombre(nombre: String): com.cafesmart.app.data.auth.AuthError? = null
}

private class NegociosCap(inicial: NegocioEntity? = null) : NegocioRepository {
    val datos = MutableStateFlow(listOfNotNull(inicial).associateBy { it.uid })
    var fallar = false
    override suspend fun guardar(negocio: NegocioEntity) {
        if (fallar) throw IllegalStateException("fallo simulado")
        datos.value = datos.value + (negocio.uid to negocio)
    }
    override fun observar(uid: String): Flow<NegocioEntity?> = datos.map { it[uid] }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CapacidadBodegaTest {

    private val usuario = UsuarioSesion("u1", "Andrés", "andres@correo.com")
    private val sinConfigurar = NegocioEntity("u1", "Café Nuevo", "COMPRAVENTA", "3001234567", creadoEn = 1L)
    private val ocupacion = OrigenOcupacion { 834.0 }

    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    // ── Reglas ─────────────────────────────────────────────

    @Test fun elTextoDeKgEntiendePuntoDeMilesYComaDecimal() {
        assertEquals(3000.0, kgDesdeTexto("3000")!!, 0.0)
        assertEquals(3000.0, kgDesdeTexto("3.000")!!, 0.0)
        assertEquals(1234.5, kgDesdeTexto("1.234,5")!!, 0.0)
        assertEquals(40.7, kgDesdeTexto("40,7")!!, 0.0)
        assertEquals(40.7, kgDesdeTexto("40.7")!!, 0.0)
        assertNull(kgDesdeTexto(""))
        assertNull(kgDesdeTexto("abc"))
    }

    @Test fun unaCapacidadValidaYUnBultoNormalNoTienenErrores() {
        assertTrue(validarCapacidadBodega(CapacidadFormulario("3000", "40,7"), kgOcupados = 834.0).isEmpty())
    }

    @Test fun nuncaHayUnMaximoDeCapacidadPeroSiUnMinimoMayorACero() {
        // Cada negocio define la suya: una bodega enorme es válida.
        assertTrue(validarCapacidadBodega(CapacidadFormulario("999999999", "40,7"), 0.0).isEmpty())
        assertEquals(ERROR_CAPACIDAD_VACIA, validarCapacidadBodega(CapacidadFormulario("", "40,7"), 0.0)["capacidad"])
        // 0 solo significa "sin configurar": no se puede guardar.
        assertEquals(ERROR_CAPACIDAD_CERO, validarCapacidadBodega(CapacidadFormulario("0", "40,7"), 0.0)["capacidad"])
    }

    @Test fun laCapacidadNoPuedeSerMenorAlCafeQueYaHay() {
        val e = validarCapacidadBodega(CapacidadFormulario("500", "40,7"), kgOcupados = 834.0)
        assertEquals("La capacidad no puede ser menor al café que ya tienes (834 kg).", e["capacidad"])
        // Igual al café guardado sí se puede: la bodega queda justo llena.
        assertTrue(validarCapacidadBodega(CapacidadFormulario("834", "40,7"), 834.0).isEmpty())
    }

    @Test fun elBultoPesaEntre10Y100Kg() {
        assertEquals(ERROR_BULTO_RANGO, validarCapacidadBodega(CapacidadFormulario("3000", "9,9"), 0.0)["bulto"])
        assertEquals(ERROR_BULTO_RANGO, validarCapacidadBodega(CapacidadFormulario("3000", "100,1"), 0.0)["bulto"])
        assertTrue(validarCapacidadBodega(CapacidadFormulario("3000", "10"), 0.0).isEmpty())
        assertTrue(validarCapacidadBodega(CapacidadFormulario("3000", "100"), 0.0).isEmpty())
        assertEquals(ERROR_BULTO_VACIO, validarCapacidadBodega(CapacidadFormulario("3000", ""), 0.0)["bulto"])
    }

    @Test fun textoDeKgMuestraEnteroOConComa() {
        assertEquals("3000", textoDeKg(3000.0))
        assertEquals("40,7", textoDeKg(40.7))
    }

    // ── ViewModel ──────────────────────────────────────────

    @Test fun conCapacidadEnCeroElCampoQuedaVacioYElBultoEsElInicial() {
        val vm = CapacidadBodegaViewModel(SesionCap(usuario), NegociosCap(sinConfigurar), ocupacion)
        val e = vm.uiState.value
        assertFalse(e.cargando)
        assertEquals("", e.form.capacidad)
        assertEquals("40,7", e.form.pesoBulto)
        assertEquals(834.0, e.kgOcupados, 0.0)
    }

    @Test fun guardaLaCapacidadYElPesoDelBulto() {
        val negocios = NegociosCap(sinConfigurar)
        val vm = CapacidadBodegaViewModel(SesionCap(usuario), negocios, ocupacion)
        vm.editar { it.copy(capacidad = "3000", pesoBulto = "41") }
        vm.guardar()
        assertTrue(vm.uiState.value.guardado)
        assertEquals(3000.0, negocios.datos.value["u1"]!!.capacidadBodegaKg, 0.0)
        assertEquals(41.0, negocios.datos.value["u1"]!!.pesoBultoKg, 0.0)
        // Lo demás del negocio no cambia.
        assertEquals("Café Nuevo", negocios.datos.value["u1"]!!.nombre)
    }

    @Test fun conErroresNoGuardaYLosMuestra() {
        val negocios = NegociosCap(sinConfigurar)
        val vm = CapacidadBodegaViewModel(SesionCap(usuario), negocios, ocupacion)
        vm.editar { it.copy(capacidad = "100") }
        vm.guardar()
        assertFalse(vm.uiState.value.guardado)
        assertTrue(vm.uiState.value.errores.containsKey("capacidad"))
        assertEquals(0.0, negocios.datos.value["u1"]!!.capacidadBodegaKg, 0.0)
        // Al corregir, el error se va.
        vm.editar { it.copy(capacidad = "900") }
        assertTrue(vm.uiState.value.errores.isEmpty())
    }

    @Test fun siFallaGuardarMuestraElAvisoRojo() {
        val negocios = NegociosCap(sinConfigurar).also { it.fallar = true }
        val vm = CapacidadBodegaViewModel(SesionCap(usuario), negocios, ocupacion)
        vm.editar { it.copy(capacidad = "3000") }
        vm.guardar()
        assertFalse(vm.uiState.value.guardado)
        assertEquals(AVISO_CAPACIDAD_ERROR, vm.uiState.value.aviso?.mensaje)
        assertEquals(TipoAviso.ERROR, vm.uiState.value.aviso?.tipo)
    }

    @Test fun sinNegocioPideCompletarLaInformacionDeLaEmpresa() {
        val vm = CapacidadBodegaViewModel(SesionCap(usuario), NegociosCap(), ocupacion)
        vm.editar { it.copy(capacidad = "3000") }
        vm.guardar()
        assertFalse(vm.uiState.value.guardado)
        assertEquals(AVISO_CAPACIDAD_SIN_NEGOCIO, vm.uiState.value.aviso?.mensaje)
    }

    @Test fun elDisponibleSaleDeLaCapacidadEscrita() {
        val vm = CapacidadBodegaViewModel(SesionCap(usuario), NegociosCap(sinConfigurar), ocupacion)
        vm.editar { it.copy(capacidad = "3000") }
        assertEquals(3000.0, vm.uiState.value.capacidadKg!!, 0.0)
        vm.editar { it.copy(capacidad = "") }
        assertNull(vm.uiState.value.capacidadKg)
    }

    // ── Inventario lee la capacidad del Negocio ────────────

    @Test fun inventarioUsaLaCapacidadYElBultoDelNegocioYSeActualizaSolo() {
        val negocios = NegociosCap(sinConfigurar)
        val vm = InventoryViewModel(negocios = negocios, uid = "u1")
        // Cuenta nueva: capacidad sin configurar.
        assertEquals(EstadoBodega.SIN_CAPACIDAD, vm.uiState.value.resumen.ocupacion.estado)
        assertEquals(KG_POR_BULTO, vm.uiState.value.resumen.pesoBultoKg, 0.0)
        // Al guardar la capacidad en Ajustes, Inventario se actualiza sin reiniciar.
        negocios.datos.value = mapOf("u1" to sinConfigurar.copy(capacidadBodegaKg = 5000.0, pesoBultoKg = 50.0))
        assertEquals(5000.0, vm.uiState.value.resumen.capacidadKg, 0.0)
        assertEquals(50.0, vm.uiState.value.resumen.pesoBultoKg, 0.0)
        assertTrue(vm.uiState.value.resumen.ocupacion.estado != EstadoBodega.SIN_CAPACIDAD)
    }
}
