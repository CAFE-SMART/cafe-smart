package com.cafesmart.app.data.sale

import com.cafesmart.app.data.inventario.OrigenOcupacionRoom
import com.cafesmart.app.data.inventario.RoomInventoryRepository
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.purchase.BaseFalsa
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.viewmodel.purchase.NivelAvisoCapacidad
import com.cafesmart.app.viewmodel.purchase.avisoCapacidadCompra
import com.cafesmart.app.viewmodel.sale.subtotal
import com.cafesmart.app.viewmodel.sale.sublotesDelTipo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// El flujo completo comprar -> vender -> inventario con los DAOs falsos (sin Room real): lo que se vende baja
// del sublote que se compró y el inventario y la bodega lo ven. Las consultas reales están en androidTest.
class FlujoCompraVentaInventarioTest {

    private val fecha = Fecha(dia = 10, mes = 11, anio = 2026)
    private val base = BaseFalsa()
    private val compras = base.repositorio()
    private val ventas = base.repositorioVentas()
    private val inventario = RoomInventoryRepository(base.lotes, base.sublotes)

    // Compra un café y devuelve el id de su sublote (el último que se creó).
    private suspend fun comprar(tipo: String, calidad: Calidad, kg: Double, precio: Double = 14_000.0): Long {
        compras.registrarCompra(null, fecha, listOf(CafeValidado(tipo, calidad, kg, precio)))
        return base.sublotes.filas.last().id
    }

    private suspend fun vender(vararg items: ItemVenta, clienteId: Long? = null): Long =
        ventas.registrarVenta(clienteId, fecha, items.toList())

    private suspend fun lotes(): List<LoteConSublotes> = inventario.observarLotes().first()
    private suspend fun sublotes(): List<Sublote> = lotes().flatMap { it.sublotes }
    private suspend fun disponible(id: Long) = sublotes().first { it.id == id }.pesoDisponibleKg

    // 1. Comprar 100 kg y vender 50: queda 1 sublote con 50 de 100.
    @Test fun venderNoDuplicaElSubloteYBajaLosDisponibles() = runBlocking {
        val id = comprar("Verde", Calidad.BUENA, 100.0)
        vender(ItemVenta(id, 50.0, 15_000))

        val grupos = lotes()
        assertEquals(1, grupos.size)
        val sublote = grupos.single().sublotes.single()   // un solo sublote, no un segundo "de lo vendido"
        assertEquals(id, sublote.id)
        assertEquals(50.0, sublote.pesoDisponibleKg, 0.0)
        assertEquals(100.0, sublote.pesoInicialKg, 0.0)
    }

    // 2. 100 kg: se venden 50 + 50 = queda 0. Una tercera venta de 1 kg falla y nada cambia.
    @Test fun vendidoTodoQuedaEnCeroYUnaVentaMasFalla() = runBlocking {
        val id = comprar("Verde", Calidad.BUENA, 100.0)
        vender(ItemVenta(id, 50.0, 15_000))
        vender(ItemVenta(id, 50.0, 15_000))
        assertEquals(0.0, disponible(id), 0.0)
        assertTrue(sublotes().single().estaAgotado)

        val antes = lotes()
        try {
            vender(ItemVenta(id, 1.0, 15_000))
            fail("Debía fallar por falta de café")
        } catch (e: SinCafeSuficienteException) {
            assertEquals(id, e.subloteId)
        }
        assertEquals(antes, lotes())
        assertEquals(2, base.ventas.listar().size)       // solo las dos ventas buenas
    }

    // 3. Tres sublotes (100, 80 y 60 kg). Una venta toma 30 del primero y 20 del segundo: quedan 70, 60 y 60.
    @Test fun unaVentaConDosSublotesNoTocaElTercero() = runBlocking {
        val a = comprar("Verde", Calidad.BUENA, 100.0)
        val b = comprar("Seco", Calidad.BUENA, 80.0)
        val c = comprar("Seco", Calidad.REGULAR, 60.0)
        vender(ItemVenta(a, 30.0, 15_000), ItemVenta(b, 20.0, 16_000))

        assertEquals(70.0, disponible(a), 0.0)
        assertEquals(60.0, disponible(b), 0.0)
        assertEquals(60.0, disponible(c), 0.0)
    }

    // 4. El primer sublote alcanza (30 de 100) pero el segundo no (90 de 80): el inventario queda como estaba.
    @Test fun siElSegundoNoAlcanzaElInventarioQuedaIgual() = runBlocking {
        val a = comprar("Verde", Calidad.BUENA, 100.0)
        val b = comprar("Seco", Calidad.BUENA, 80.0)
        val antes = lotes()
        try {
            vender(ItemVenta(a, 30.0, 15_000), ItemVenta(b, 90.0, 16_000))
            fail("Debía fallar por falta de café")
        } catch (e: SinCafeSuficienteException) {
            assertEquals(b, e.subloteId)
        }
        assertEquals(antes, lotes())
        assertEquals(0, base.ventas.listar().size)
        assertEquals(0, base.ventas.detalles.size)
    }

    // 5. 25,5 - 10,1 = 15,4 y 15,4 - 15,4 = 0 exacto. 0,3 - 0,1 - 0,1 - 0,1 = 0 exacto.
    @Test fun conDecimalesQuedaCeroExacto() = runBlocking {
        val a = comprar("Verde", Calidad.BUENA, 25.5)
        vender(ItemVenta(a, 10.1, 15_000))
        assertEquals(15.4, disponible(a), 0.0)
        vender(ItemVenta(a, 15.4, 15_000))
        assertEquals(0.0, disponible(a), 0.0)

        val b = comprar("Seco", Calidad.BUENA, 0.3)
        repeat(3) { vender(ItemVenta(b, 0.1, 15_000)) }
        assertEquals(0.0, disponible(b), 0.0)
    }

    // 6. Compra 100 Verde Buena, vende 40 (quedan 60), compra 30 más igual: 1 lote, 2 sublotes (60/100 y 30/30).
    @Test fun volverAComprarReutilizaElLoteYCreaUnSubloteNuevo() = runBlocking {
        val primero = comprar("Verde", Calidad.BUENA, 100.0)
        vender(ItemVenta(primero, 40.0, 15_000))
        val segundo = comprar("Verde", Calidad.BUENA, 30.0)

        val grupo = lotes().single()                       // el mismo lote
        assertEquals(2, grupo.sublotes.size)
        assertTrue(primero != segundo)
        val viejo = grupo.sublotes.first { it.id == primero }
        val nuevo = grupo.sublotes.first { it.id == segundo }
        assertEquals(60.0, viejo.pesoDisponibleKg, 0.0)    // conserva su descuento
        assertEquals(100.0, viejo.pesoInicialKg, 0.0)
        assertEquals(30.0, nuevo.pesoDisponibleKg, 0.0)
        assertEquals(30.0, nuevo.pesoInicialKg, 0.0)
        assertEquals(90.0, grupo.pesoDisponibleKg, 0.0)    // 60 + 30, nada se suma dos veces
    }

    // 7. Capacidad 3.000 y 2.800 ocupados. Compra nueva de 100: 2.900 / 3.000 = 96,7 % -> casi llena.
    //    Se venden 500: quedan 2.300 (ocupación = disponibles, no iniciales: no 2.800). 2.300 + 100 = 80 % -> sin aviso.
    @Test fun venderLiberaBodegaYQuitaElAvisoDeCasiLlena() = runBlocking {
        val id = comprar("Verde", Calidad.BUENA, 2_800.0)
        val ocupacion = OrigenOcupacionRoom(base.sublotes)
        assertEquals(2_800.0, ocupacion.kgOcupados(), 0.0)
        assertEquals(NivelAvisoCapacidad.CASI_LLENA, avisoCapacidadCompra(3_000.0, ocupacion.kgOcupados(), 100.0)?.nivel)

        vender(ItemVenta(id, 500.0, 15_000))

        assertEquals(2_300.0, ocupacion.kgOcupados(), 0.0)
        assertNull(avisoCapacidadCompra(3_000.0, ocupacion.kgOcupados(), 100.0))
    }

    // 8. La compra no cambia al vender: total pagado 100 x 14.000 = 1.400.000 y peso inicial 100.
    @Test fun venderNoCambiaElHistorialDeLaCompra() = runBlocking {
        val id = comprar("Verde", Calidad.BUENA, 100.0, 14_000.0)
        val compraId = base.sublotes.filas.single().compraId!!
        vender(ItemVenta(id, 70.0, 15_000))

        assertEquals(1_400_000L, base.compras.buscarPorId(compraId)?.totalPagado)
        assertEquals(100.0, base.sublotes.filas.single().pesoInicialKg, 0.0)
        assertEquals(30.0, base.sublotes.filas.single().pesoDisponibleKg, 0.0)
    }

    // 9. 10,5 x 13.001 = 136.510,5 -> 136.511 (sube); dos así = 273.022 (y no 273.021 de redondear al final).
    //    Más 3 x 15.000 = 45.000: total 318.022. Coincide con subtotal() de SaleRules.
    @Test fun totalRecibidoCoincideConSubtotalDeSaleRules() = runBlocking {
        val a = comprar("Verde", Calidad.BUENA, 50.0)
        val b = comprar("Seco", Calidad.BUENA, 50.0)
        val c = comprar("Seco", Calidad.REGULAR, 50.0)
        vender(ItemVenta(a, 10.5, 13_001), ItemVenta(b, 10.5, 13_001), ItemVenta(c, 3.0, 15_000))

        val esperado = subtotal(10.5, 13_001.0) * 2 + subtotal(3.0, 15_000.0)
        assertEquals(136_511L, subtotal(10.5, 13_001.0))
        assertEquals(318_022L, esperado)
        assertEquals(esperado, base.ventas.listar().single().totalRecibido)
    }

    // 10. Con y sin cliente descuentan igual: dos sublotes de 100, a cada uno se le venden 25 -> quedan 75.
    @Test fun ventaConYSinClienteDescuentanIgual() = runBlocking {
        val sin = comprar("Verde", Calidad.BUENA, 100.0)
        val con = comprar("Seco", Calidad.BUENA, 100.0)
        val cliente = (ventas.guardarCliente(Cliente(nombre = "Ana Gómez", documento = "52123456")) as ResultadoCliente.Guardado).cliente

        vender(ItemVenta(sin, 25.0, 15_000))
        vender(ItemVenta(con, 25.0, 15_000), clienteId = cliente.id)

        assertEquals(75.0, disponible(sin), 0.0)
        assertEquals(75.0, disponible(con), 0.0)
        assertEquals(listOf(null, cliente.id), base.ventas.listar().map { it.clienteId })
    }

    // 11. Se compran 100 Verde Buena y se venden 50: la lista de venta muestra 50. Al vender los otros 50 desaparece.
    @Test fun elSubloteAgotadoYaNoSeOfreceParaVender() = runBlocking {
        val id = comprar("Verde", Calidad.BUENA, 100.0)
        suspend fun vendibles(): List<SubloteConLote> =
            sublotesDelTipo(lotes().flatMap { g -> g.sublotes.map { SubloteConLote(it, g.lote) } }, TipoCafe.VERDE)

        vender(ItemVenta(id, 50.0, 15_000))
        assertEquals(50.0, vendibles().single().sublote.pesoDisponibleKg, 0.0)

        vender(ItemVenta(id, 50.0, 15_000))
        assertTrue(vendibles().isEmpty())
        assertNotNull(sublotes().firstOrNull { it.id == id })   // sigue en el inventario, solo agotado
    }
}
