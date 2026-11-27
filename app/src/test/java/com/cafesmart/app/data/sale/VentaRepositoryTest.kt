package com.cafesmart.app.data.sale

import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.purchase.BaseFalsa
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// El repositorio de ventas con DAOs falsos: qué se guarda al confirmar una venta, cuánto se descuenta y que
// el guardado es todo o nada. Las consultas reales se prueban en androidTest (VentasRoomTest).
class VentaRepositoryTest {

    private val fecha = Fecha(dia = 10, mes = 11, anio = 2026)
    private val base = BaseFalsa()
    private val repositorio = base.repositorioVentas()

    // Crea un sublote con ese peso (inicial = disponible) y devuelve su id.
    private suspend fun subloteCon(kg: Double): Long {
        val loteId = base.lotes.insertar(LoteEntity(tipoCafe = "SECO${base.lotes.filas.size}", calidad = "BUENA"))
        return base.sublotes.insertar(
            SubloteEntity(
                loteId = loteId, pesoInicialKg = kg, pesoDisponibleKg = kg,
                costoKg = 10_000, fechaIngreso = fechaAMilis(fecha),
            ),
        )
    }

    private fun disponible(id: Long) = base.sublotes.filas.first { it.id == id }.pesoDisponibleKg

    // ── Venta ──────────────────────────────────────────────

    @Test fun venderCincuentaDeCienDejaCincuentaYGuardaVentaYDetalle() = runBlocking {
        val sublote = subloteCon(100.0)
        // 50 kg x $15.000 = $750.000
        val ventaId = repositorio.registrarVenta(null, fecha, listOf(ItemVenta(sublote, 50.0, 15_000)))

        assertEquals(50.0, disponible(sublote), 0.0)
        val venta = base.ventas.listar().single()
        assertEquals(ventaId, venta.id)
        assertEquals(750_000L, venta.totalRecibido)
        assertEquals(fechaAMilis(fecha), venta.fecha)
        val detalle = base.ventas.detalles.single()
        assertEquals(ventaId, detalle.ventaId)
        assertEquals(sublote, detalle.subloteId)
        assertEquals(50.0, detalle.pesoKg, 0.0)
        assertEquals(15_000L, detalle.precioKg)
    }

    @Test fun unaVentaConDosSublotesDescuentaCadaUno() = runBlocking {
        val a = subloteCon(100.0)
        val b = subloteCon(40.0)
        val c = subloteCon(70.0)
        // 30 x 15.000 = 450.000 y 40 x 12.000 = 480.000 -> 930.000; c no cambia.
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(a, 30.0, 15_000), ItemVenta(b, 40.0, 12_000)))

        assertEquals(70.0, disponible(a), 0.0)
        assertEquals(0.0, disponible(b), 0.0)
        assertEquals(70.0, disponible(c), 0.0)
        assertEquals(930_000L, base.ventas.listar().single().totalRecibido)
        assertEquals(2, base.ventas.detalles.size)
    }

    @Test fun siElSegundoSubloteNoAlcanzaNoQuedaNada() = runBlocking {
        val a = subloteCon(100.0)
        val b = subloteCon(10.0)
        try {
            // El primero sí alcanza (30 de 100); el segundo pide 10,001 de 10: falla ya con el primero descontado.
            repositorio.registrarVenta(null, fecha, listOf(ItemVenta(a, 30.0, 15_000), ItemVenta(b, 10.001, 12_000)))
            fail("Debía fallar por falta de café")
        } catch (e: SinCafeSuficienteException) {
            assertEquals(b, e.subloteId)
        }
        assertEquals(0, base.ventas.listar().size)
        assertEquals(0, base.ventas.detalles.size)
        assertEquals(100.0, disponible(a), 0.0)
        assertEquals(10.0, disponible(b), 0.0)
    }

    @Test fun venderTodoDejaCeroSinNegativosNiRuido() = runBlocking {
        val todo = subloteCon(100.0)
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(todo, 100.0, 15_000)))
        assertEquals(0.0, disponible(todo), 0.0)

        // 0,3 kg: se vende 0,1 y luego 0,2. En Double 0.1 + 0.2 = 0.30000000000000004; aquí queda 0 exacto.
        val poco = subloteCon(0.3)
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(poco, 0.1, 15_000)))
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(poco, 0.2, 15_000)))
        assertEquals(0.0, disponible(poco), 0.0)

        // 1 kg - 0,1 - 0,2 en Double da 0.7000000000000001; guardado queda 0.7 exacto.
        val uno = subloteCon(1.0)
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(uno, 0.1, 15_000)))
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(uno, 0.2, 15_000)))
        assertEquals(0.7, disponible(uno), 0.0)
    }

    @Test fun totalRecibidoEsLaSumaDeLosSubtotalesRedondeados() = runBlocking {
        val a = subloteCon(100.0)
        val b = subloteCon(100.0)
        // Un solo sublote: 25,5 kg x 14.000 = 357.000.
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(a, 25.5, 14_000)))
        assertEquals(357_000L, base.ventas.listar().last().totalRecibido)

        // Dos items con .5: 10,5 x 13.001 = 136.510,5 -> 136.511 cada uno = 273.022.
        // (Si se sumara sin redondear y se redondeara al final daría 273.021: ese es el error que se evita.)
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(a, 10.5, 13_001), ItemVenta(b, 10.5, 13_001)))
        assertEquals(273_022L, base.ventas.listar().last().totalRecibido)
    }

    @Test fun unaVentaSinClienteGuardaClienteIdNulo() = runBlocking {
        val sublote = subloteCon(100.0)
        repositorio.registrarVenta(null, fecha, listOf(ItemVenta(sublote, 5.0, 15_000)))
        assertNull(base.ventas.listar().single().clienteId)
    }

    @Test fun unaVentaConClienteGuardaSuId() = runBlocking {
        val cliente = (repositorio.guardarCliente(Cliente(nombre = "Trilladora Norte", documento = "9003421215", tipoDocumento = TipoDocumento.NIT)) as ResultadoCliente.Guardado).cliente
        val sublote = subloteCon(100.0)
        repositorio.registrarVenta(cliente.id, fecha, listOf(ItemVenta(sublote, 5.0, 15_000)))
        assertEquals(cliente.id, base.ventas.listar().single().clienteId)
    }

    @Test fun unaVentaInvalidaFallaSinGuardarNada() = runBlocking {
        val sublote = subloteCon(100.0)
        val invalidas = listOf(
            emptyList(),                                  // sin items
            listOf(ItemVenta(sublote, 0.0, 15_000)),      // peso 0
            listOf(ItemVenta(sublote, 5.0, 0)),           // precio 0
            listOf(ItemVenta(sublote, 5.0, 15_000), ItemVenta(sublote, 0.0, 15_000)), // el segundo item es inválido
        )
        for (items in invalidas) {
            try {
                repositorio.registrarVenta(null, fecha, items)
                fail("Debía fallar: $items")
            } catch (e: IllegalArgumentException) {
                // Esperado.
            }
        }
        assertEquals(0, base.ventas.listar().size)
        assertEquals(0, base.ventas.detalles.size)
        assertEquals(100.0, disponible(sublote), 0.0)
    }

    // ── Clientes ───────────────────────────────────────────

    @Test fun guardarClienteYListarloEnOrden() = runBlocking {
        val ana = repositorio.guardarCliente(Cliente(nombre = "Ana Gómez", documento = "52123456"))
        val beto = repositorio.guardarCliente(Cliente(nombre = "Beto Ruiz", documento = "80123456"))
        assertTrue(ana is ResultadoCliente.Guardado && beto is ResultadoCliente.Guardado)
        assertEquals(1L, (ana as ResultadoCliente.Guardado).cliente.id)
        assertEquals(listOf("Ana Gómez", "Beto Ruiz"), repositorio.listarClientes().map { it.nombre })
    }

    @Test fun unDocumentoRepetidoNoSeGuarda() = runBlocking {
        repositorio.guardarCliente(Cliente(nombre = "Ana Gómez", documento = "52123456"))
        val repetido = repositorio.guardarCliente(Cliente(nombre = "Otra Ana", documento = "52123456"))
        assertEquals(ResultadoCliente.DocumentoRepetido, repetido)
        assertEquals(1, repositorio.listarClientes().size)
    }

    @Test fun actualizarConSuPropioDocumentoSirveYElDeOtroSeRechaza() = runBlocking {
        val ana = (repositorio.guardarCliente(Cliente(nombre = "Ana Gómez", documento = "52123456")) as ResultadoCliente.Guardado).cliente
        val beto = (repositorio.guardarCliente(Cliente(nombre = "Beto Ruiz", documento = "80123456")) as ResultadoCliente.Guardado).cliente

        val propio = repositorio.actualizarCliente(ana.copy(nombre = "Ana Gómez Ruiz"))
        assertTrue(propio is ResultadoCliente.Guardado)
        assertEquals("Ana Gómez Ruiz", repositorio.listarClientes().first().nombre)

        val ajeno = repositorio.actualizarCliente(beto.copy(documento = "52123456"))
        assertEquals(ResultadoCliente.DocumentoRepetido, ajeno)
        assertEquals("80123456", repositorio.listarClientes().last().documento)
    }
}
