package com.cafesmart.app.purchase

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.ModoProductor
import com.cafesmart.app.viewmodel.purchase.MENSAJE_ERROR_GUARDAR_COMPRA
import com.cafesmart.app.viewmodel.purchase.NivelAvisoCapacidad
import com.cafesmart.app.viewmodel.purchase.PurchaseViewModel
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

// Compras conectadas a Room: al confirmar se guarda la compra (con repositorio falso) y la bodega se actualiza.
@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseViewModelGuardadoTest {

    @Before fun preparar() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun limpiar() { Dispatchers.resetMain() }

    private val repositorio = RepositorioDeComprasFalso()
    private val ocupacion = OcupacionFalsa()

    private fun viewModel() = PurchaseViewModel(repositorio, ocupacion)

    // Deja la compra lista en el paso 3 con un cafe Verde / Buena.
    private fun PurchaseViewModel.conCafe(kg: String, precio: String = "14000") = apply {
        cambiarTipoCafe(0, "Verde"); cambiarCalidad(0, Calidad.BUENA)
        cambiarPeso(0, kg); cambiarPrecio(0, precio)
    }

    // ── Guardar la compra ──────────────────────────────────

    @Test fun confirmarGuardaLaCompraYMuestraCompraRegistrada() {
        val vm = viewModel().conCafe("25")
        vm.elegirModo(ModoProductor.SIN_REGISTRAR); vm.continuarPaso1()
        vm.confirmarCompra()

        assertTrue(vm.uiState.value.compraRegistrada)
        assertFalse(vm.uiState.value.guardando)
        assertEquals(1, repositorio.compras.size)
        // Sin registrar: no hay productor (id 0 del productor "Sin registrar" se guarda como null).
        assertNull(repositorio.compras[0].productorId)
        assertEquals(listOf(CafeValidado("Verde", Calidad.BUENA, 25.0, 14_000.0)), repositorio.compras[0].cafes)
        assertEquals(Fecha(vm.uiState.value.fecha.dia, vm.uiState.value.fecha.mes, vm.uiState.value.fecha.anio), repositorio.compras[0].fecha)
    }

    @Test fun conProductorRegistradoGuardaElIdDeRoom() {
        val vm = viewModel()
        vm.cambiarDocumento("1114059684"); vm.cambiarNombre("Ana Gómez")
        vm.guardarNuevoProductor()
        vm.continuarPaso1(); vm.conCafe("25")
        vm.confirmarCompra()

        val ana = repositorio.productores.single()
        assertTrue(ana.id > 0)
        assertEquals(ana.id, repositorio.compras.single().productorId)
    }

    @Test fun conVariosCafesPasaTodosAlRepositorio() {
        val vm = viewModel().conCafe("25")
        vm.agregarCafe()
        vm.cambiarTipoCafe(1, "Seco"); vm.cambiarCalidad(1, Calidad.REGULAR); vm.cambiarPeso(1, "100,5"); vm.cambiarPrecio(1, "20000")
        vm.elegirModo(ModoProductor.SIN_REGISTRAR); vm.continuarPaso1()
        vm.confirmarCompra()

        assertEquals(
            listOf(
                CafeValidado("Verde", Calidad.BUENA, 25.0, 14_000.0),
                CafeValidado("Seco", Calidad.REGULAR, 100.5, 20_000.0),
            ),
            repositorio.compras.single().cafes,
        )
    }

    @Test fun conUnCafeIncompletoNoGuarda() {
        val vm = viewModel()
        vm.cambiarTipoCafe(0, "Verde")
        vm.confirmarCompra()
        assertTrue(repositorio.compras.isEmpty())
        assertFalse(vm.uiState.value.compraRegistrada)
    }

    @Test fun siFallaElGuardadoNoMuestraCompraRegistradaYSePuedeReintentar() {
        val vm = viewModel().conCafe("25")
        repositorio.fallarAlRegistrar = true
        vm.confirmarCompra()

        assertFalse(vm.uiState.value.compraRegistrada)
        assertFalse(vm.uiState.value.guardando)
        assertEquals(MENSAJE_ERROR_GUARDAR_COMPRA, vm.uiState.value.errorAlGuardar)
        assertTrue(repositorio.compras.isEmpty())

        repositorio.fallarAlRegistrar = false
        vm.confirmarCompra()
        assertTrue(vm.uiState.value.compraRegistrada)
        assertNull(vm.uiState.value.errorAlGuardar)
        assertEquals(1, repositorio.compras.size)
    }

    // ── Bodega: la compra no se cuenta dos veces ───────────

    // Capacidad 3.000 kg, 834 kg en bodega, compra de 2.300 kg (la prueba de la tarjeta).
    private fun compraQueSuperaLaCapacidad(): PurchaseViewModel {
        ocupacion.kg = 834.0
        repositorio.bodega = ocupacion
        val vm = viewModel().conCafe("2300")
        vm.actualizarBodega(capacidadKg = 3000.0, kgOcupados = ocupacion.kg)
        return vm
    }

    @Test fun enLosPasosAvisaQueLaCompraSuperaLaCapacidad() {
        val vm = compraQueSuperaLaCapacidad()
        val aviso = vm.uiState.value.avisoCapacidad!!
        assertEquals(NivelAvisoCapacidad.SUPERADA, aviso.nivel)
        assertEquals("Esta compra supera tu capacidad", aviso.titulo)
        // Es solo un aviso: la compra se puede registrar.
        assertTrue(vm.uiState.value.cafesListos)
    }

    @Test fun alRegistrarElAvisoUsaLaOcupacionDeAntesDeGuardar() {
        val vm = compraQueSuperaLaCapacidad()
        vm.confirmarCompra()

        val estado = vm.uiState.value
        assertTrue(estado.compraRegistrada)
        val aviso = estado.avisoAlRegistrar!!
        assertEquals("Tu bodega superó su capacidad", aviso.tituloRegistrada)
        // 834 + 2.300 = 3.134 (y no 834 + 2.300 + 2.300, que seria contar la compra dos veces).
        assertEquals(3134.0, aviso.nuevoTotalKg, 0.0)
        assertTrue(aviso.mensajeRegistrada.startsWith("Nuevo total: 3.134 kg de 3.000 kg"))
    }

    @Test fun despuesDeGuardarLaBodegaYaIncluyeLaCompraYNoHayAvisoDeNuevo() {
        val vm = compraQueSuperaLaCapacidad()
        vm.confirmarCompra()

        // Los kilos de hoy se leen de nuevo: ya incluyen la compra guardada.
        assertEquals(3134.0, vm.uiState.value.kgOcupados, 0.0)
        // Con la compra registrada, el aviso en vivo no se vuelve a calcular sobre la misma compra.
        assertNull(vm.uiState.value.avisoCapacidad)
        // Una actualizacion de la bodega no cambia el aviso de la compra ya registrada.
        vm.actualizarBodega(3000.0, 9999.0)
        assertEquals(3134.0, vm.uiState.value.avisoAlRegistrar!!.nuevoTotalKg, 0.0)
    }

    @Test fun laSiguienteCompraParteDeLaBodegaActualizada() {
        val vm = compraQueSuperaLaCapacidad()
        vm.confirmarCompra()
        vm.reiniciarCompra()

        assertEquals(3134.0, vm.uiState.value.kgOcupados, 0.0)
        vm.conCafe("100")
        // 3.134 + 100 = 3.234 kg: se pasa 234 kg de los 3.000 que caben.
        assertEquals(3234.0, vm.uiState.value.avisoCapacidad!!.nuevoTotalKg, 0.0)
    }

    // ── Productores en Room ────────────────────────────────

    @Test fun cargarProductoresLeeLosGuardados() {
        val repo = RepositorioDeComprasFalso(com.cafesmart.app.data.purchase.productoresDeEjemplo)
        val vm = PurchaseViewModel(repo, ocupacion)
        assertTrue(vm.uiState.value.productores.isEmpty())
        vm.cargarProductores()
        assertEquals(3, vm.uiState.value.productores.size)
    }

    @Test fun elProductorNuevoSeGuardaEnRoomYQuedaElegidoConSuId() {
        val vm = viewModel()
        vm.cambiarDocumento("1114059684"); vm.cambiarNombre("Ana Gómez")
        vm.guardarNuevoProductor()

        assertEquals(1, repositorio.productores.size)
        val estado = vm.uiState.value
        assertEquals(repositorio.productores.single(), estado.productorElegido)
        assertTrue(estado.productorElegido!!.id > 0)
        assertEquals(ModoProductor.REGISTRADO, estado.modo)
    }

    @Test fun siElDocumentoYaEstaEnRoomSeAvisaYNoSeGuardaOtro() {
        val vm = viewModel()
        // La copia en pantalla no sabe del productor: otro celular o una lista vieja.
        repositorio.productores += com.cafesmart.app.data.model.Productor(id = 1, nombre = "Ana Gómez", documento = "1114059684")
        vm.cambiarDocumento("1114059684"); vm.cambiarNombre("Otra Persona")
        vm.guardarNuevoProductor()

        assertEquals(1, repositorio.productores.size)
        assertEquals("Este documento ya está registrado. Verifica en tu lista.", vm.uiState.value.documentoError)
        assertFalse(vm.uiState.value.guardarNuevoHabilitado)
    }

    @Test fun modificarUnProductorConservaSuIdYSeGuardaEnRoom() {
        val repo = RepositorioDeComprasFalso(com.cafesmart.app.data.purchase.productoresDeEjemplo)
        val vm = viewModelDePrueba(repo)
        val pepito = vm.uiState.value.productores.last()
        vm.elegirProductor(pepito)
        vm.editarProductor(pepito)
        vm.cambiarNombreEdicion("Pepito Gómez")
        vm.guardarEdicion()

        assertEquals("Pepito Gómez", repo.productores.last().nombre)
        assertEquals(pepito.id, repo.productores.last().id)
        assertEquals(pepito.id, vm.uiState.value.productorElegido?.id)
    }
}
