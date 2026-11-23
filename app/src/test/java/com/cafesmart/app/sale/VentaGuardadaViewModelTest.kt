package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.data.sale.ItemVenta
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import com.cafesmart.app.viewmodel.sale.subtotal
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// "Confirmar venta" guarda de verdad con VentaRepository.registrarVenta y el inventario baja por el Flow.
class VentaGuardadaViewModelTest {

    @get:Rule val principal = PrincipalDeVentasRule()

    // Un cliente con id real (los de ejemplo traen id 0).
    private val carlos = Cliente(id = 7, nombre = "Carlos Mejía", documento = "16780012")
    private val repositorio = RepositorioDeVentasFalso(listOf(carlos))
    private val inventario = InventarioDeVentasFalso()
    private lateinit var vm: SaleViewModel

    // Se crea aqui y no al declarar el campo: la regla ya puso el Main de prueba (viewModelScope lo usa).
    @Before
    fun preparar() { vm = viewModelDeVentasDePrueba(repositorio, inventario) }

    private fun SaleViewModel.disponible(id: Long) =
        uiState.value.inventario.first { it.sublote.id == id }.sublote.pesoDisponibleKg

    private fun SaleViewModel.ofrecidos() =
        uiState.value.grupos.flatMap { it.sublotes }.associate { it.sublote.sublote.id to it.sublote.sublote.pesoDisponibleKg }

    // Venta lista sin cliente: 150 kg del sublote 5 a $ 20.000 (de 200 kg).
    private fun SaleViewModel.prepararSinRegistrar(kg: String = "150") {
        elegirModo(ModoCliente.SIN_REGISTRAR)
        elegir(5)
        escribirCantidad(5, kg)
        cambiarPrecio(Calidad.BUENA, "20000")
    }

    // a) UNA venta con los items correctos, la fecha y el cliente elegido.
    @Test
    fun confirmar_guardaUnaVentaConItemsFechaYCliente() {
        vm.elegirModo(ModoCliente.REGISTRADO)
        vm.elegirCliente(carlos)
        vm.elegir(5, 8)
        vm.escribirCantidad(5, "150")
        vm.escribirCantidad(8, "30,5")
        vm.cambiarPrecio(Calidad.BUENA, "20000")
        vm.cambiarPrecio(Calidad.REGULAR, "15000")
        vm.cambiarFecha(fechaAMilis(Fecha(25, 10, 2026)))
        vm.confirmarVenta()

        val venta = repositorio.ventas.single()
        assertEquals(carlos.id, venta.clienteId)
        assertEquals(Fecha(25, 10, 2026), venta.fecha)
        assertEquals(listOf(ItemVenta(5, 150.0, 20000), ItemVenta(8, 30.5, 15000)), venta.items)
        assertTrue(vm.uiState.value.ventaRegistrada)
        assertFalse(vm.uiState.value.guardando)
        assertNull(vm.uiState.value.errorAlGuardar)
    }

    // b) Sin registrar guarda null; con Cliente nuevo usa el id real guardado.
    @Test
    fun ventaSinRegistrar_guardaClienteIdNull() {
        vm.prepararSinRegistrar()
        vm.confirmarVenta()
        assertNull(repositorio.ventas.single().clienteId)
    }

    @Test
    fun conClienteNuevo_laVentaUsaElIdRealGuardado() {
        vm.escribirClienteValido()
        vm.continuarPaso1()
        // El cliente nuevo recibe el siguiente id, como en Room.
        val guardado = repositorio.clientes.single { it.documento == "1114059684" }
        assertEquals(8L, guardado.id)
        vm.elegir(5)
        vm.cambiarPrecio(Calidad.BUENA, "20000")
        vm.confirmarVenta()
        assertEquals(guardado.id, repositorio.ventas.single().clienteId)
    }

    // c) El inventario baja solo, por el Flow: 50 de 100, segunda venta parte de 50, al agotar desaparece.
    @Test
    fun inventarioBajaPorElFlow_yElSubloteAgotadoDesaparece() {
        // El sublote 6 tiene 100 kg.
        vm.elegirModo(ModoCliente.SIN_REGISTRAR)
        vm.elegir(6)
        vm.escribirCantidad(6, "50")
        vm.cambiarPrecio(Calidad.BUENA, "20000")
        vm.confirmarVenta()
        assertEquals(50.0, vm.disponible(6), 0.0)
        assertEquals(50.0, vm.ofrecidos().getValue(6), 0.0)

        vm.reiniciarVenta()
        vm.elegirModo(ModoCliente.SIN_REGISTRAR)
        vm.elegir(6)
        assertEquals(50.0, vm.uiState.value.elegidos.single().cantidadKg, 0.0)
        vm.cambiarPrecio(Calidad.BUENA, "20000")
        vm.confirmarVenta()
        assertEquals(0.0, vm.disponible(6), 0.0)
        assertFalse(6L in vm.ofrecidos())
        assertEquals(2, repositorio.ventas.size)
    }

    // d) Confirmar dos veces seguidas guarda una sola venta.
    @Test
    fun confirmarDosVeces_guardaUnaSolaVenta() {
        vm.prepararSinRegistrar()
        vm.confirmarVenta()
        vm.confirmarVenta()
        assertEquals(1, repositorio.ventas.size)
        // El segundo toque ni llega al repositorio (si llegara, fallaria por falta de cafe y saldria un error falso).
        assertEquals(1, repositorio.intentosDeRegistrar)
        assertNull(vm.uiState.value.errorAlGuardar)
        assertEquals(50.0, vm.disponible(5), 0.0)
    }

    // d) Un segundo toque MIENTRAS se guarda la primera tampoco llega al repositorio.
    @Test
    fun confirmarMientrasSeGuarda_noGuardaOtraVez() {
        vm.prepararSinRegistrar()
        val lento = CompletableDeferred<Unit>()
        repositorio.esperarAlRegistrar = lento
        vm.confirmarVenta()
        assertTrue(vm.uiState.value.guardando)
        vm.confirmarVenta()
        assertEquals(1, repositorio.intentosDeRegistrar)
        lento.complete(Unit)
        assertTrue(vm.uiState.value.ventaRegistrada)
        assertFalse(vm.uiState.value.guardando)
        assertEquals(1, repositorio.ventas.size)
        assertEquals(50.0, vm.disponible(5), 0.0)
    }

    // e) Si falla: no queda registrada, hay error, el inventario no cambia; al reintentar guarda una sola vez.
    @Test
    fun siRegistrarFalla_dejaErrorNoDescuentaYSePuedeReintentar() {
        vm.prepararSinRegistrar()
        repositorio.fallarAlRegistrar = true
        vm.confirmarVenta()
        val estado = vm.uiState.value
        assertFalse(estado.ventaRegistrada)
        assertFalse(estado.guardando)
        assertNotNull(estado.errorAlGuardar)
        assertEquals(200.0, vm.disponible(5), 0.0)
        assertTrue(repositorio.ventas.isEmpty())

        repositorio.fallarAlRegistrar = false
        vm.confirmarVenta()
        assertTrue(vm.uiState.value.ventaRegistrada)
        assertNull(vm.uiState.value.errorAlGuardar)
        assertEquals(1, repositorio.ventas.size)
        assertEquals(50.0, vm.disponible(5), 0.0)
    }

    // f) SinCafeSuficienteException tambien deja el error y no descuenta nada.
    @Test
    fun sinCafeSuficiente_dejaErrorYNoDescuentaNada() {
        vm.prepararSinRegistrar()
        // Otra venta (otro dispositivo, por ejemplo) ya se llevo el cafe: el inventario falso queda con 100 kg.
        inventario.lotes.value = inventario.lotes.value.map { lote ->
            lote.copy(sublotes = lote.sublotes.map { if (it.id == 5L) it.copy(pesoDisponibleKg = 100.0) else it })
        }
        vm.confirmarVenta()
        val estado = vm.uiState.value
        assertFalse(estado.ventaRegistrada)
        assertFalse(estado.guardando)
        assertNotNull(estado.errorAlGuardar)
        assertTrue(repositorio.ventas.isEmpty())
        assertEquals(100.0, vm.disponible(5), 0.0)
    }

    // g) El total del estado es el que se guarda: subtotal por item redondeado.
    @Test
    fun totalARecibir_esElTotalGuardado() {
        vm.elegirModo(ModoCliente.SIN_REGISTRAR)
        vm.elegir(5, 6)
        vm.escribirCantidad(5, "33,333")
        vm.escribirCantidad(6, "10,111")
        vm.cambiarPrecio(Calidad.BUENA, "15555")
        val esperado = vm.uiState.value.totalARecibir
        vm.confirmarVenta()
        val guardado = repositorio.ventas.single().items.sumOf { subtotal(it.pesoKg, it.precioKg.toDouble()) }
        assertEquals(esperado, guardado)
        assertEquals(subtotal(33.333, 15555.0) + subtotal(10.111, 15555.0), guardado)
    }

    // h) Descartar el error (Regresar) lo limpia sin registrar nada y deja reintentar.
    @Test
    fun descartarElError_loLimpiaSinRegistrarYDejaReintentar() {
        vm.prepararSinRegistrar()
        repositorio.fallarAlRegistrar = true
        vm.confirmarVenta()
        assertNotNull(vm.uiState.value.errorAlGuardar)

        vm.descartarErrorAlGuardar()
        assertNull(vm.uiState.value.errorAlGuardar)
        assertFalse(vm.uiState.value.ventaRegistrada)
        assertTrue(repositorio.ventas.isEmpty())

        repositorio.fallarAlRegistrar = false
        vm.confirmarVenta()
        assertTrue(vm.uiState.value.ventaRegistrada)
        assertEquals(1, repositorio.ventas.size)
    }
}
