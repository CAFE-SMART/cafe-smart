package com.cafesmart.app.data.secado

import com.cafesmart.app.data.local.LoteEntity
import com.cafesmart.app.data.local.SubloteDao
import com.cafesmart.app.data.local.SubloteEntity
import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.EstadoSecado
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.purchase.BaseFalsa
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// El repositorio de secado con DAOs falsos: qué se guarda al iniciar, finalizar y cancelar, y que cada paso
// es todo o nada. Las consultas reales se prueban en androidTest (SecadoRoomTest).
class RoomSecadoRepositoryTest {

    private val inicio = Fecha(dia = 3, mes = 11, anio = 2026)
    private val fin = Fecha(dia = 10, mes = 11, anio = 2026)
    private val base = BaseFalsa()
    private val repositorio = base.repositorioSecado()

    // Crea un sublote verde (sin compra: compra_id es null) con ese peso y costo; devuelve su id.
    private suspend fun verdeCon(kg: Double, costoKg: Long = 14_000, calidad: String = "BUENA"): Long {
        val loteId = base.lotes.buscarPorTipoYCalidad("VERDE", calidad)?.id
            ?: base.lotes.insertar(LoteEntity(tipoCafe = "VERDE", calidad = calidad))
        return base.sublotes.insertar(
            SubloteEntity(
                loteId = loteId, pesoInicialKg = kg, pesoDisponibleKg = kg,
                costoKg = costoKg, fechaIngreso = fechaAMilis(inicio),
            ),
        )
    }

    private fun disponible(id: Long) = base.sublotes.filas.first { it.id == id }.pesoDisponibleKg

    private fun kilos(buena: Double = 0.0, regular: Double = 0.0, mala: Double = 0.0) =
        mapOf(Calidad.BUENA to buena, Calidad.REGULAR to regular, Calidad.MALA to mala)

    // ── Iniciar ────────────────────────────────────────────

    @Test fun iniciarDescuentaLosKgYGuardaElSecadoConSusEnvios() = runBlocking {
        val a = verdeCon(100.0)
        val b = verdeCon(40.0)
        val c = verdeCon(70.0)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 60.0), EnvioASecar(b, 40.0)))

        assertEquals(EstadoSecado.EN_CURSO, secado.estado)
        assertEquals(inicio, secado.fechaInicio)
        assertEquals(40.0, disponible(a), 0.0)
        assertEquals(0.0, disponible(b), 0.0)
        assertEquals(70.0, disponible(c), 0.0)   // no se envió: no cambia
        val fila = base.secados.filas.single()
        assertEquals(secado.id, fila.id)
        assertEquals("EN_CURSO", fila.estado)
        assertEquals(fechaAMilis(inicio), fila.fechaInicio)
        assertNull(fila.fechaFin)
        assertEquals(
            listOf(a to 60.0, b to 40.0),
            base.secados.envios.map { it.subloteId to it.pesoEnviadoKg }.sortedBy { it.first },
        )
        assertTrue(base.secados.envios.all { it.secadoId == secado.id })
        // El secado en curso aparece con sus envíos y sin resultantes.
        val enCurso = repositorio.secadosEnCurso().single()
        assertEquals(secado.id, enCurso.secado.id)
        assertEquals(100.0, enCurso.pesoEnviadoKg, 0.0)
        assertTrue(enCurso.resultantes.isEmpty())
    }

    @Test fun siElSegundoSubloteNoAlcanzaNoQuedaNada() = runBlocking {
        val a = verdeCon(100.0)
        val b = verdeCon(10.0)
        try {
            // El primero sí alcanza (30 de 100); el segundo pide 10,001 de 10.
            repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 30.0), EnvioASecar(b, 10.001)))
            fail("Debía fallar por falta de café")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("sublote $b"))
        }
        assertEquals(0, base.secados.filas.size)
        assertEquals(0, base.secados.envios.size)
        assertEquals(100.0, disponible(a), 0.0)
        assertEquals(10.0, disponible(b), 0.0)
    }

    @Test fun siElDescuentoFallaDespuesDeGuardarSeDeshaceTodo() = runBlocking {
        val a = verdeCon(100.0)
        val b = verdeCon(10.0)
        // Un DAO que dice "alcanza" al validar pero al descontar encuentra el sublote vacío (otra operación se adelantó).
        val vaciaB = object : SubloteDao by base.sublotes {
            override suspend fun descontar(id: Long, kg: Double): Int =
                if (id == b) 0 else base.sublotes.descontar(id, kg)
        }
        val repo = RoomSecadoRepository(base.secados, vaciaB, base.lotes, base.transaccion)
        try {
            repo.iniciarSecado(inicio, listOf(EnvioASecar(a, 30.0), EnvioASecar(b, 10.0)))
            fail("Debía fallar por falta de café")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("sublote $b"))
        }
        assertEquals(0, base.secados.filas.size)
        assertEquals(0, base.secados.envios.size)
        assertEquals(100.0, disponible(a), 0.0)   // el descuento del primero también se deshizo
        assertEquals(10.0, disponible(b), 0.0)
    }

    // ── Finalizar ──────────────────────────────────────────

    @Test fun finalizarCreaLotesYSublotesSecosConElCostoTrasladado() = runBlocking {
        val a = verdeCon(100.0, costoKg = 14_000)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 100.0)))
        // 100 kg x $14.000 = $1.400.000 entre 90 kg secos = $15.555,56 -> $15.556 por kg.
        val detalle = repositorio.finalizarSecado(secado.id, fin, kilos(buena = 60.0, regular = 30.0), 95)

        assertEquals(EstadoSecado.FINALIZADO, detalle.secado.estado)
        assertEquals(fin, detalle.secado.fechaFin)
        assertEquals(95, detalle.secado.factorRendimiento)
        assertEquals(100.0, detalle.pesoEnviadoKg, 0.0)
        assertEquals(90.0, detalle.pesoSecoKg, 0.0)
        assertEquals(10.0, detalle.mermaKg, 0.0)

        val fila = base.secados.filas.single()
        assertEquals("FINALIZADO", fila.estado)
        assertEquals(fechaAMilis(fin), fila.fechaFin)
        assertEquals(95, fila.factorRendimiento)

        val secos = base.sublotes.filas.filter { it.secadoId == secado.id }
        assertEquals(2, secos.size)
        assertTrue(secos.all { it.compraId == null && it.costoKg == 15_556L && it.factorRendimiento == 95 })
        assertTrue(secos.all { it.fechaIngreso == fechaAMilis(fin) })
        assertEquals(listOf(60.0, 30.0), secos.map { it.pesoInicialKg })
        assertEquals(secos.map { it.pesoInicialKg }, secos.map { it.pesoDisponibleKg })
        // Los lotes SECO se crearon (BUENA y REGULAR); MALA no, porque tenía 0 kg.
        assertEquals(
            setOf("SECO" to "BUENA", "SECO" to "REGULAR"),
            base.lotes.filas.filter { it.tipoCafe == "SECO" }.map { it.tipoCafe to it.calidad }.toSet(),
        )
        assertEquals(listOf(secos.map { it.id }), listOf(detalle.resultantes.map { it.id }))
        // Ya no está en curso.
        assertTrue(repositorio.secadosEnCurso().isEmpty())
    }

    @Test fun finalizarDosVecesFallaYNoDuplicaNada() = runBlocking {
        val a = verdeCon(100.0)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 100.0)))
        repositorio.finalizarSecado(secado.id, fin, kilos(buena = 90.0), null)
        val sublotesAntes = base.sublotes.filas.size

        try {
            repositorio.finalizarSecado(secado.id, fin, kilos(buena = 90.0), null)
            fail("Debía fallar: el secado ya no está en curso")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("ya no esta en curso"))
        }
        assertEquals(sublotesAntes, base.sublotes.filas.size)
        assertEquals("FINALIZADO", base.secados.filas.single().estado)
    }

    @Test fun siCerrarNoCambiaNingunaFilaFinalizarNoCreaLotesNiSublotesSecos() = runBlocking {
        val a = verdeCon(100.0)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 100.0)))
        val sublotesAntes = base.sublotes.filas
        val lotesAntes = base.lotes.filas
        // La validación previa lo ve EN_CURSO, pero al cerrar otro ya lo había cerrado: cerrar devuelve 0.
        base.secados.cerrarSinCambios = true
        try {
            repositorio.finalizarSecado(secado.id, fin, kilos(buena = 90.0), 95)
            fail("Debía fallar: el secado ya no está en curso")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("ya no esta en curso"))
        }
        assertEquals(sublotesAntes, base.sublotes.filas)   // ningún sublote seco
        assertEquals(lotesAntes, base.lotes.filas)         // ningún lote SECO
        assertEquals("EN_CURSO", base.secados.filas.single().estado)
    }

    // ── Cancelar ───────────────────────────────────────────

    @Test fun cancelarDevuelveLosKgEnviadosYMarcaElSecadoCancelado() = runBlocking {
        val a = verdeCon(100.0)
        val b = verdeCon(40.0)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 60.0), EnvioASecar(b, 40.0)))
        assertEquals(40.0, disponible(a), 0.0)
        assertEquals(0.0, disponible(b), 0.0)

        repositorio.cancelarSecado(secado.id)

        assertEquals(100.0, disponible(a), 0.0)
        assertEquals(40.0, disponible(b), 0.0)
        assertEquals("CANCELADO", base.secados.filas.single().estado)
        assertTrue(repositorio.secadosEnCurso().isEmpty())
    }

    @Test fun cancelarUnSecadoYaFinalizadoFallaYNoDevuelveKg() = runBlocking {
        val a = verdeCon(100.0)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 100.0)))
        repositorio.finalizarSecado(secado.id, fin, kilos(buena = 90.0), null)

        try {
            repositorio.cancelarSecado(secado.id)
            fail("Debía fallar: el secado ya no está en curso")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("ya no esta en curso"))
        }
        assertEquals(0.0, disponible(a), 0.0)   // no se le devolvió nada
        assertEquals("FINALIZADO", base.secados.filas.single().estado)
    }

    @Test fun siCerrarNoCambiaNingunaFilaCancelarNoDevuelveKg() = runBlocking {
        val a = verdeCon(100.0)
        val b = verdeCon(40.0)
        val secado = repositorio.iniciarSecado(inicio, listOf(EnvioASecar(a, 60.0), EnvioASecar(b, 40.0)))
        base.secados.cerrarSinCambios = true
        try {
            repositorio.cancelarSecado(secado.id)
            fail("Debía fallar: el secado ya no está en curso")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("ya no esta en curso"))
        }
        assertEquals(40.0, disponible(a), 0.0)   // siguen descontados: no se devolvió nada
        assertEquals(0.0, disponible(b), 0.0)
        assertEquals("EN_CURSO", base.secados.filas.single().estado)
    }

    // ── Inventario ─────────────────────────────────────────

    @Test fun inventarioSoloMuestraLosSublotesConCafeDisponible() = runBlocking {
        val lleno = verdeCon(100.0)
        val agotado = verdeCon(40.0, calidad = "REGULAR")
        repositorio.iniciarSecado(inicio, listOf(EnvioASecar(agotado, 40.0)))

        val ids = repositorio.inventario().map { it.sublote.id }
        assertEquals(listOf(lleno), ids)
    }
}
