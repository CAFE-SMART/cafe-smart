package com.cafesmart.app.viewmodel.home

import com.cafesmart.app.data.finanzas.TipoMovimiento
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.local.CompraEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.purchase.BaseFalsa
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.data.purchase.ResultadoProductor
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Inicio (ciclo 3, orden 11): indicadores del día y movimientos recientes con datos de Room (base falsa en memoria).
class IndicadoresInicioTest {
    @Before fun antes() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun despues() { Dispatchers.resetMain() }

    private val hoy = Fecha(27, 11, 2026)
    private val ayer = Fecha(26, 11, 2026)
    private val base = BaseFalsa()
    private val repo = base.repositorio()
    private val inventario = RoomInventoryRepository(base.lotes, base.sublotes)

    private fun cafe(tipo: String, kg: Double, precio: Double = 10_000.0) = CafeValidado(tipo, Calidad.BUENA, kg, precio)
    private fun comprar(productorId: Long?, fecha: Fecha, vararg cafes: CafeValidado) =
        runBlocking { repo.registrarCompra(productorId, fecha, cafes.toList()) }
    private fun productor(nombre: String, doc: String) =
        (runBlocking { repo.guardarProductor(Productor(nombre = nombre, documento = doc)) } as ResultadoProductor.Guardado).productor
    private fun lotes() = runBlocking { inventario.observarLotes().first() }
    private fun compras(): List<CompraEntity> = runBlocking { base.compras.listar() }

    @Test fun soloCuentaLasComprasDeHoy() {
        val ana = productor("Ana Ruiz", "1234567")
        comprar(ana.id, hoy, cafe("Verde", 100.0), cafe("Seco", 20.5))
        comprar(null, hoy, cafe("Verde", 10.0, 12_000.0))
        comprar(ana.id, ayer, cafe("Verde", 500.0))
        val i = indicadoresDelDia(lotes(), compras(), ventas = null, hoy = hoy)
        assertEquals(2, i.compras.compras)
        assertEquals(130.5, i.compras.kgComprados, 0.0005)
        assertEquals(1, i.compras.productores)            // "Sin registrar" no cuenta como productor
        assertEquals(1_000_000L + 205_000L + 120_000L, i.pagadoComprasPesos)
    }

    @Test fun elLimiteDelDiaUsaLaFechaDeLaCompra() {
        comprar(null, ayer, cafe("Verde", 5.0))
        assertEquals(0, indicadoresDelDia(lotes(), compras(), null, hoy).compras.compras)
        assertEquals(1, indicadoresDelDia(lotes(), compras(), null, ayer).compras.compras)
    }

    @Test fun sinFuenteDeVentasNoSeInventaUnCero() {
        assertNull(indicadoresDelDia(lotes(), compras(), ventas = null, hoy = hoy).ventas)
        // Conectada y sin ventas hoy: ahí sí es 0.
        assertEquals(VentasDelDia(0, 0.0, 0), indicadoresDelDia(lotes(), compras(), emptyList(), hoy).ventas)
    }

    @Test fun ventasDeHoyConKgEIngresos() {
        val ventas = listOf(
            VentaRegistrada(hoy, 50.0, 15_000.0, 10_000.0, 0.0),
            VentaRegistrada(hoy, 25.5, 16_000.0, 10_000.0, 5_000.0),
            VentaRegistrada(ayer, 999.0, 15_000.0, 10_000.0, 0.0),
        )
        val v = indicadoresDelDia(lotes(), compras(), ventas, hoy).ventas!!
        assertEquals(2, v.ventas)
        assertEquals(75.5, v.kgVendidos, 0.0005)
        assertEquals(750_000L + 408_000L, v.ingresosPesos)
    }

    @Test fun movimientosRecientesOrdenadosSinRepetirYConLimite() {
        val ana = productor("Ana Ruiz", "1234567")
        comprar(ana.id, ayer, cafe("Verde", 100.0))
        comprar(null, hoy, cafe("Seco", 20.0), cafe("Verde", 5.0))
        comprar(ana.id, hoy, cafe("Trillado", 23.0))
        val lista = compras()
        val movs = movimientosRecientes(comprasRegistradas(lista + lista.first(), lotes(), mapOf(ana.id to ana.nombre)), null)
        assertEquals(3, movs.size)                                   // la compra repetida no se duplica
        assertEquals(listOf("Compra · Ana Ruiz", "Compra · Sin registrar", "Compra · Ana Ruiz"), movs.map { it.titulo })
        assertEquals(listOf("Café trillado · 23 kg", "2 cafés · 25 kg", "Café verde · 100 kg"), movs.map { it.detalle })
        assertEquals(listOf(hoy, hoy, ayer), movs.map { it.fecha })
        assertTrue(movs.all { it.tipo == TipoMovimiento.COMPRA && it.montoPesos < 0 })
        assertEquals(2, movimientosRecientes(comprasRegistradas(lista, lotes(), emptyMap()), null, cuantos = 2).size)
    }

    @Test fun sinOperacionesTodoQuedaVacio() {
        val i = indicadoresDelDia(emptyList(), emptyList(), null, hoy)
        assertEquals(0, i.compras.compras); assertEquals(0L, i.pagadoComprasPesos)
        assertTrue(movimientosRecientes(emptyList(), null).isEmpty())
    }

    @Test fun productorNoDisponibleNoSeConfundeConCompraSinRegistrar() {
        val ana = productor("Ana Ruiz", "1234567")
        comprar(ana.id, hoy, cafe("Verde", 5.0))
        val compra = comprasRegistradas(compras(), lotes(), emptyMap()).single()
        assertEquals("Productor no disponible", compra.productor)
    }

    @Test fun inicioMuestraIndicadoresYActualizarVuelveALeerLosNombres() {
        var lecturas = 0
        var nombre = "Ana"
        val ana = productor("Ana", "1234567")
        comprar(ana.id, hoy, cafe("Verde", 60.0))
        val vm = HomeViewModel(inventario, base.compras.observarTodas(), null, null, hoy = { hoy },
            nombresProductores = { lecturas++; mapOf(ana.id to nombre) })
        assertEquals(1, vm.uiState.value.indicadores.compras.compras)
        assertNull(vm.uiState.value.indicadores.ventas)
        assertEquals("Compra · Ana", vm.uiState.value.movimientos.single().titulo)
        // Se edita el nombre del productor; "Actualizar" lo vuelve a leer.
        nombre = "Ana María"
        val antes = lecturas
        vm.actualizar()
        assertTrue(lecturas > antes)
        assertEquals("Compra · Ana María", vm.uiState.value.movimientos.single().titulo)
        // Una compra nueva actualiza Inicio sola.
        comprar(null, hoy, cafe("Seco", 10.0))
        assertEquals(2, vm.uiState.value.indicadores.compras.compras)
        assertEquals(fechaAMilis(hoy), compras().last().fecha)
    }
}
