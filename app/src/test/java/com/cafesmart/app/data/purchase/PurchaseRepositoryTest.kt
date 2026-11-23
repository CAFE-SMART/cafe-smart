package com.cafesmart.app.data.purchase

import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Productor
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// El repositorio de compras con DAOs falsos: comprueba qué se guarda al confirmar una compra
// (los sublotes generados) y que el guardado es todo o nada. Las consultas reales se prueban en androidTest.
class PurchaseRepositoryTest {

    private val fecha = Fecha(dia = 10, mes = 11, anio = 2026)
    private val base = BaseFalsa()
    private val repositorio = base.repositorio()

    private fun cafe(tipo: String, calidad: Calidad, kg: Double, precio: Double) =
        CafeValidado(tipoCafe = tipo, calidad = calidad, pesoKg = kg, precioKg = precio)

    // ── Compra y sublotes ──────────────────────────────────

    @Test fun generaUnSubloteParaCadaCafeConSusDatos() = runBlocking {
        val compraId = repositorio.registrarCompra(
            productorId = null,
            fecha = fecha,
            cafes = listOf(
                cafe("Verde", Calidad.BUENA, 25.0, 14_000.0),
                cafe("Seco", Calidad.REGULAR, 100.5, 20_000.0),
            ),
        )

        val sublotes = base.sublotes.listar()
        assertEquals(2, sublotes.size)
        // Cada sublote nace de esta compra, con el peso completo disponible y el precio por kg del café.
        assertTrue(sublotes.all { it.compraId == compraId && it.secadoId == null })
        assertEquals(listOf(25.0, 100.5), sublotes.map { it.pesoInicialKg })
        assertEquals(sublotes.map { it.pesoInicialKg }, sublotes.map { it.pesoDisponibleKg })
        assertEquals(listOf(14_000L, 20_000L), sublotes.map { it.costoKg })
        assertTrue(sublotes.all { it.fechaIngreso == fechaAMilis(fecha) })
        assertTrue(sublotes.all { it.humedadPorcentaje == null && it.factorRendimiento == null })
        // Cada sublote apunta al lote de su tipo y calidad.
        assertEquals(
            listOf(LoteEntity(1, "VERDE", "BUENA"), LoteEntity(2, "SECO", "REGULAR")),
            sublotes.map { base.lotes.buscarPorId(it.loteId) },
        )
    }

    @Test fun laCompraGuardaLaFechaYElTotalPagado() = runBlocking {
        val compraId = repositorio.registrarCompra(
            productorId = null,
            fecha = fecha,
            cafes = listOf(
                cafe("Verde", Calidad.BUENA, 25.0, 14_000.0),   // 350.000
                cafe("Seco", Calidad.REGULAR, 100.5, 20_000.0), // 2.010.000
            ),
        )
        val compra = base.compras.buscarPorId(compraId)!!
        assertEquals(2_360_000L, compra.totalPagado)
        assertEquals(fechaAMilis(fecha), compra.fecha)
    }

    @Test fun sinRegistrarGuardaProductorNuloYConProductorGuardaSuId() = runBlocking {
        val sinRegistrar = repositorio.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 10.0, 14_000.0)))
        val guardado = (repositorio.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456")) as ResultadoProductor.Guardado).productor
        val registrado = repositorio.registrarCompra(guardado.id, fecha, listOf(cafe("Verde", Calidad.BUENA, 10.0, 14_000.0)))

        assertNull(base.compras.buscarPorId(sinRegistrar)!!.productorId)
        assertEquals(guardado.id, base.compras.buscarPorId(registrado)!!.productorId)
    }

    @Test fun dosCafesIgualesDeLaMismaCompraComparteLote() = runBlocking {
        repositorio.registrarCompra(
            null, fecha,
            listOf(cafe("Verde", Calidad.BUENA, 10.0, 14_000.0), cafe("Verde", Calidad.BUENA, 20.0, 15_000.0)),
        )
        assertEquals(1, base.lotes.listar().size)
        assertEquals(2, base.sublotes.listar().size)
        assertEquals(1, base.sublotes.listar().map { it.loteId }.distinct().size)
    }

    @Test fun reutilizaElLoteQueYaExisteEnVezDeCrearOtro() = runBlocking {
        val existente = base.lotes.insertar(LoteEntity(tipoCafe = TipoCafe.VERDE.name, calidad = Calidad.BUENA.name))
        repositorio.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 10.0, 14_000.0)))
        repositorio.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.MALA, 10.0, 9_000.0)))

        assertEquals(listOf("BUENA", "MALA"), base.lotes.listar().map { it.calidad })
        assertEquals(existente, base.sublotes.listar().first().loteId)
    }

    // ── Todo o nada ────────────────────────────────────────

    @Test fun siFallaElSegundoSubloteNoQuedaNadaGuardado() = runBlocking {
        base.sublotes.fallarEnInsercion = 2
        try {
            repositorio.registrarCompra(
                null, fecha,
                listOf(cafe("Verde", Calidad.BUENA, 10.0, 14_000.0), cafe("Seco", Calidad.BUENA, 20.0, 18_000.0)),
            )
            fail("Debía fallar al guardar el segundo sublote")
        } catch (e: IllegalStateException) {
            // Esperado: la transacción se deshace completa.
        }
        assertEquals(emptyList<Any>(), base.compras.listar())
        assertEquals(emptyList<Any>(), base.lotes.listar())
        assertEquals(emptyList<Any>(), base.sublotes.listar())
    }

    @Test fun unTipoDeCafeDesconocidoNoDejaNadaGuardado() = runBlocking {
        try {
            repositorio.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 10.0, 14_000.0), cafe("Mojado", Calidad.BUENA, 5.0, 9_000.0)))
            fail("Debía fallar: 'Mojado' no es un tipo de café")
        } catch (e: IllegalArgumentException) {
            // Esperado.
        }
        assertEquals(emptyList<Any>(), base.compras.listar())
        assertEquals(emptyList<Any>(), base.sublotes.listar())
    }

    @Test fun unaCompraSinCafesNoSeRegistra() = runBlocking {
        try {
            repositorio.registrarCompra(null, fecha, emptyList())
            fail("Debía fallar: no hay cafés")
        } catch (e: IllegalArgumentException) {
            // Esperado.
        }
        assertEquals(emptyList<Any>(), base.compras.listar())
    }

    // ── Productores ────────────────────────────────────────

    @Test fun guardaUnProductorNuevoYLoDevuelveConId() = runBlocking {
        val resultado = repositorio.guardarProductor(
            Productor(nombre = "Finca La Esperanza", documento = "900123456", tipoDocumento = TipoDocumento.NIT, telefono = "3001234567"),
        )
        val guardado = (resultado as ResultadoProductor.Guardado).productor
        assertEquals(1L, guardado.id)
        // Se lee de vuelta tal como se guardó.
        assertEquals(listOf(guardado), repositorio.listarProductores())
        assertEquals(TipoDocumento.NIT, repositorio.listarProductores().first().tipoDocumento)
    }

    @Test fun unDocumentoRepetidoNoSeGuarda() = runBlocking {
        repositorio.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456"))
        val resultado = repositorio.guardarProductor(Productor(nombre = "Otra Ana", documento = "52123456"))
        assertEquals(ResultadoProductor.DocumentoRepetido, resultado)
        assertEquals(1, repositorio.listarProductores().size)
    }

    @Test fun modificarConservaElIdYSuPropioDocumentoNoCuentaComoRepetido() = runBlocking {
        val ana = (repositorio.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456")) as ResultadoProductor.Guardado).productor
        val resultado = repositorio.actualizarProductor(ana.copy(nombre = "Ana Gómez Ruiz", telefono = "3109876543"))
        assertTrue(resultado is ResultadoProductor.Guardado)
        assertEquals(listOf(ana.copy(nombre = "Ana Gómez Ruiz", telefono = "3109876543")), repositorio.listarProductores())
    }

    @Test fun modificarNoDejaUsarElDocumentoDeOtro() = runBlocking {
        repositorio.guardarProductor(Productor(nombre = "Ana Gómez", documento = "52123456"))
        val pepe = (repositorio.guardarProductor(Productor(nombre = "Pepe Pérez", documento = "11140596")) as ResultadoProductor.Guardado).productor

        val resultado = repositorio.actualizarProductor(pepe.copy(documento = "52123456"))

        assertEquals(ResultadoProductor.DocumentoRepetido, resultado)
        assertEquals("11140596", repositorio.listarProductores().last().documento)
    }
}
