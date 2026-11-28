package com.cafesmart.app.purchase

import androidx.lifecycle.SavedStateHandle
import com.cafesmart.app.data.borradores.AlmacenBorradoresMemoria
import com.cafesmart.app.data.borradores.BorradorDeOperacion
import com.cafesmart.app.data.borradores.TipoOperacion
import com.cafesmart.app.data.borradores.claveBorrador
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.CafeFormulario
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.data.purchase.NuevoProductor
import com.cafesmart.app.data.purchase.productoresDeEjemplo
import com.cafesmart.app.viewmodel.purchase.BorradorCompra
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
import com.cafesmart.app.viewmodel.purchase.decodificarBorradorCompra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Recuperar una compra interrumpida (ciclo 3, orden 21): girar la pantalla, Android cierra el proceso o la app se
// cierra y se vuelve a abrir. Al confirmar o cancelar, el borrador se borra.
class RecuperarCompraTest {
    @Before fun preparar() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }

    private val repositorio = RepositorioDeComprasFalso(productoresDeEjemplo)
    private val telefono = AlmacenBorradoresMemoria()
    private val clave = claveBorrador(TipoOperacion.COMPRA, "cuentaA")

    /** Un ViewModel nuevo, como al abrir Compras otra vez (mismo teléfono = mismo almacén). */
    private fun abrir(estadoGuardado: SavedStateHandle? = null, claveCuenta: String = clave) =
        PurchaseViewModel(repositorio, OcupacionFalsa(), BorradorDeOperacion(telefono, claveCuenta, estadoGuardado))
            .also { it.cargarProductores() }

    private fun PurchaseViewModel.cafe(kg: String = "25", precio: String = "14000") = apply {
        cambiarTipoCafe(0, "Verde"); cambiarCalidad(0, Calidad.BUENA); cambiarPeso(0, kg); cambiarPrecio(0, precio)
    }

    @Test fun formularioSinTocarNoGuardaNada() {
        abrir()
        assertNull(telefono.leer(clave))
    }

    @Test fun alVolverAAbrirLaAppRecuperaElPasoYLoEscrito() {
        val vm = abrir()
        val productor = vm.uiState.value.productores[1]
        vm.elegirModo(ModoProductor.REGISTRADO); vm.elegirProductor(productor); vm.continuarPaso1()
        vm.cafe(kg = "12,5")
        val escrito = vm.uiState.value.cafes[0] // el formulario guarda el peso como "12.5" y lo muestra con coma
        assertNotNull(telefono.leer(clave))

        val otraVez = abrir().uiState.value   // la app se cerró y se volvió a abrir
        assertEquals(2, otraVez.paso)
        assertEquals(productor, otraVez.productorElegido)
        assertEquals(productor, otraVez.productorDeLaCompra)
        assertEquals(escrito, otraVez.cafes[0])
        assertEquals(CafeFormulario("Verde", Calidad.BUENA, "12.5", "14000"), otraVez.cafes[0])
    }

    @Test fun sinRegistrarEnElPaso3SeRecuperaEnElPaso3() {
        val vm = abrir().cafe()
        vm.elegirModo(ModoProductor.SIN_REGISTRAR); vm.continuarPaso1(); vm.continuarPaso2()
        assertEquals(3, vm.uiState.value.paso)
        val otraVez = abrir().uiState.value
        assertEquals(3, otraVez.paso)
        assertEquals(ModoProductor.SIN_REGISTRAR, otraVez.modo)
        assertEquals(0L, otraVez.productorDeLaCompra?.id)
    }

    @Test fun compraSinRegistrarReanudadaPuedeAvanzarAlPasoTres() {
        val vm = abrir()
        vm.elegirModo(ModoProductor.SIN_REGISTRAR)
        vm.continuarPaso1()
        vm.cafe(kg = "12,5", precio = "14000")
        val recuperado = abrir()
        assertEquals(2, recuperado.uiState.value.paso)
        assertTrue(recuperado.uiState.value.cafesListos)
        recuperado.continuarPaso2()
        assertEquals(3, recuperado.uiState.value.paso)
    }

    @Test fun siAndroidCierraElProcesoRecuperaDesdeSavedStateHandle() {
        val estado = SavedStateHandle()
        val vm = abrir(estado)
        vm.elegirModo(ModoProductor.REGISTRADO)
        vm.cambiarTipoDocumento(TipoDocumento.NIT); vm.cambiarDocumento("900123456"); vm.cambiarNombre("Finca QA")
        telefono.borrar(clave) // aunque el teléfono no lo tenga, el estado guardado por Android sí
        val restaurado = SavedStateHandle(estado.keys().associateWith { estado.get<Any>(it) })
        val otraVez = abrir(restaurado).uiState.value
        assertEquals(NuevoProductor(TipoDocumento.NIT, "900123456", "Finca QA", ""), otraVez.nuevoProductor)
    }

    @Test fun alConfirmarSeBorraElBorradorYNoSeDuplicaLaCompra() {
        val vm = abrir().cafe()
        vm.elegirModo(ModoProductor.SIN_REGISTRAR); vm.continuarPaso1(); vm.continuarPaso2()
        assertNotNull(telefono.leer(clave))
        vm.confirmarCompra()
        assertTrue(vm.uiState.value.compraRegistrada)
        assertNull(telefono.leer(clave))
        // Al volver a abrir no aparece la compra ya registrada para confirmarla otra vez.
        assertEquals(1, abrir().uiState.value.paso)
        assertEquals(1, repositorio.compras.size)
    }

    @Test fun alCancelarSeBorraElBorrador() {
        val vm = abrir().cafe()
        vm.elegirModo(ModoProductor.SIN_REGISTRAR); vm.continuarPaso1()
        assertNotNull(telefono.leer(clave))
        vm.reiniciarCompra() // lo que hace "Sí, cancelar"
        assertNull(telefono.leer(clave))
    }

    @Test fun cadaCuentaTieneSuBorrador() {
        abrir().cafe()
        assertNotNull(telefono.leer(clave))
        val otraCuenta = abrir(claveCuenta = claveBorrador(TipoOperacion.COMPRA, "cuentaB")).uiState.value
        assertEquals(CafeFormulario(), otraCuenta.cafes[0])
        assertEquals(1, otraCuenta.paso)
    }

    @Test fun siElProductorYaNoExisteVuelveAlPaso1() {
        val vm = abrir()
        val productor = vm.uiState.value.productores[0]
        vm.elegirModo(ModoProductor.REGISTRADO); vm.elegirProductor(productor); vm.continuarPaso1()
        repositorio.productores.removeIf { it.id == productor.id }
        val otraVez = abrir().uiState.value
        assertEquals(1, otraVez.paso)
        assertNull(otraVez.productorDeLaCompra) // nunca se confirma una compra sin su productor
    }

    @Test fun textoDanadoSeIgnora() {
        telefono.guardar(clave, "v1|no|es|valido")
        assertEquals(1, abrir().uiState.value.paso)
        assertNull(decodificarBorradorCompra("otra-version"))
    }

    @Test fun codificarYDecodificarConservaSeparadoresEnLosCampos() {
        val b = BorradorCompra(2, ModoProductor.REGISTRADO, 7, NuevoProductor(TipoDocumento.CC, "123", "Ana | 50% ; sí, no", "3001234567"),
            listOf(CafeFormulario("Seco", Calidad.MALA, "1,5", "2000"), CafeFormulario()), 1, Fecha(2, 12, 2026))
        assertEquals(b, decodificarBorradorCompra(b.codificar()))
    }
}
