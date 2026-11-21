package com.cafesmart.app.data.inventario

import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.purchase.BaseFalsa
import com.cafesmart.app.data.purchase.CafeValidado
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// El repositorio de inventario con DAOs falsos: comprueba cómo junta los lotes con sus sublotes y que lo que
// guarda Compras llega a Inventario sin reiniciar. Las consultas reales (observarTodos) se prueban con Room
// real en androidTest (InventarioRoomTest).
class InventoryRepositoryTest {

    private val fecha = Fecha(dia = 10, mes = 11, anio = 2026)
    private val base = BaseFalsa()
    private val compras = base.repositorio()
    private val inventario = RoomInventoryRepository(base.lotes, base.sublotes)

    private fun cafe(tipo: String, calidad: Calidad, kg: Double, precio: Double = 14_000.0) =
        CafeValidado(tipoCafe = tipo, calidad = calidad, pesoKg = kg, precioKg = precio)

    private suspend fun lotesActuales(): List<LoteConSublotes> = inventario.observarLotes().first()

    // ── Lectura ────────────────────────────────────────────

    @Test fun sinNadaGuardadoEntregaUnaListaVacia() = runBlocking {
        assertEquals(emptyList<LoteConSublotes>(), lotesActuales())
    }

    @Test fun unaCompraLlegaConElLoteYElSubloteConSusDatos() = runBlocking {
        val compraId = compras.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 25.0, 14_000.0)))

        val grupo = lotesActuales().single()
        assertEquals(TipoCafe.VERDE, grupo.lote.tipoCafe)
        assertEquals(Calidad.BUENA, grupo.lote.calidad)
        val sublote = grupo.sublotes.single()
        assertEquals(grupo.lote.id, sublote.loteId)
        assertEquals(compraId, sublote.compraId)
        assertEquals(25.0, sublote.pesoInicialKg, 0.0)
        assertEquals(25.0, sublote.pesoDisponibleKg, 0.0)
        assertEquals(14_000L, sublote.costoKg)
        // La fecha vuelve de milisegundos UTC al día, mes y año que se registraron.
        assertEquals(fecha, sublote.fechaIngreso)
    }

    @Test fun losSublotesSeAgrupanEnElLoteDeSuTipoYCalidad() = runBlocking {
        compras.registrarCompra(
            null, fecha,
            listOf(cafe("Verde", Calidad.BUENA, 25.0), cafe("Seco", Calidad.REGULAR, 100.5)),
        )
        compras.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 10.0)))

        val lotes = lotesActuales()
        assertEquals(2, lotes.size)
        val verdeBueno = lotes.first { it.lote.tipoCafe == TipoCafe.VERDE }
        assertEquals(2, verdeBueno.cantidadSublotes)
        assertEquals(35.0, verdeBueno.pesoDisponibleKg, 1e-9)
        val secoRegular = lotes.first { it.lote.tipoCafe == TipoCafe.SECO }
        assertEquals(Calidad.REGULAR, secoRegular.lote.calidad)
        assertEquals(100.5, secoRegular.pesoDisponibleKg, 1e-9)
    }

    @Test fun unLoteSinSublotesApareceConLaListaVacia() = runBlocking {
        base.lotes.insertar(LoteEntity(tipoCafe = "PASILLA", calidad = "MALA"))

        val grupo = lotesActuales().single()
        assertEquals(TipoCafe.PASILLA, grupo.lote.tipoCafe)
        assertEquals(emptyList<Any>(), grupo.sublotes)
        assertEquals(0, grupo.cantidadSublotes)
    }

    @Test fun unSubloteAgotadoSeEntregaPeroNoCuentaComoExistencia() = runBlocking {
        val loteId = base.lotes.insertar(LoteEntity(tipoCafe = "SECO", calidad = "BUENA"))
        base.sublotes.insertar(
            SubloteEntity(
                loteId = loteId, secadoId = 1, pesoInicialKg = 90.0, pesoDisponibleKg = 0.0,
                costoKg = 20_000, fechaIngreso = fechaAMilis(fecha),
            )
        )
        base.sublotes.insertar(
            SubloteEntity(
                loteId = loteId, secadoId = 1, pesoInicialKg = 100.0, pesoDisponibleKg = 60.0,
                costoKg = 21_000, fechaIngreso = fechaAMilis(fecha),
            )
        )

        val grupo = lotesActuales().single()
        assertEquals(2, grupo.sublotes.size)
        assertEquals(1, grupo.cantidadSublotes)
        assertEquals(60.0, grupo.pesoDisponibleKg, 1e-9)
    }

    @Test fun losOpcionalesDelSubloteSeConservan() = runBlocking {
        val loteId = base.lotes.insertar(LoteEntity(tipoCafe = "SECO", calidad = "BUENA"))
        base.sublotes.insertar(
            SubloteEntity(
                loteId = loteId, secadoId = 3, pesoInicialKg = 80.0, pesoDisponibleKg = 70.0,
                costoKg = 21_000, fechaIngreso = fechaAMilis(fecha),
                humedadPorcentaje = 11.5, factorRendimiento = 92,
            )
        )

        val sublote = lotesActuales().single().sublotes.single()
        assertEquals(null, sublote.compraId)
        assertEquals(3L, sublote.secadoId)
        assertEquals(11.5, sublote.humedadPorcentaje!!, 0.0)
        assertEquals(92, sublote.factorRendimiento)
    }

    // ── Cambios en vivo (el entregable de la tarjeta) ──────

    @Test fun unaCompraNuevaLlegaSolaSinVolverAPedirLosDatos() = runBlocking {
        val vistos = mutableListOf<List<LoteConSublotes>>()
        // Inventario queda abierto escuchando, como la pantalla.
        val escucha = launch(Dispatchers.Unconfined) { inventario.observarLotes().toList(vistos) }

        assertEquals(emptyList<LoteConSublotes>(), vistos.last())
        compras.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 25.0)))
        compras.registrarCompra(null, fecha, listOf(cafe("Seco", Calidad.MALA, 40.0)))
        escucha.cancel()

        // Lo último que vio la escucha es el inventario completo, sin haberlo vuelto a pedir.
        val ultimo = vistos.last()
        assertEquals(2, ultimo.size)
        assertEquals(65.0, ultimo.sumOf { it.pesoDisponibleKg }, 1e-9)
    }

    @Test fun unaCompraQueFallaNoDejaNadaEnInventario() = runBlocking {
        base.sublotes.fallarEnInsercion = 1
        try {
            compras.registrarCompra(null, fecha, listOf(cafe("Verde", Calidad.BUENA, 25.0)))
            fail("la compra debía fallar")
        } catch (e: IllegalStateException) {
            // Esperado: el guardado es todo o nada.
        }
        assertTrue(lotesActuales().isEmpty())
    }
}
