package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sample.lotesEjemplo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Los datos de "Lista por tipo" (calidades de un tipo) y "Sublotes del grupo", calculados con los lotes de ejemplo
// (las cifras del Figma: Verde 290 kg = 60 + 200 + 30; Seco Buena 300 kg sin contar el sublote agotado).
class InventoryListsTest {

    private fun sublote(id: Long, loteId: Long, kg: Double, disponible: Double = kg, fecha: Fecha) = Sublote(
        id = id, loteId = loteId, compraId = id, pesoInicialKg = kg, pesoDisponibleKg = disponible,
        costoKg = 10_000, fechaIngreso = fecha,
    )

    // ── Lista por tipo: resumirCalidades ───────────────────

    @Test fun lasCalidadesDeVerdeSumanLoMismoQueElResumen() {
        val calidades = resumirCalidades(lotesEjemplo(), TipoCafe.VERDE)
        assertEquals(290.0, calidades.sumOf { it.pesoKg }, 1e-9)
        assertEquals(4, calidades.sumOf { it.cantidadSublotes })
        // Es lo mismo que dice la fila de Verde del resumen.
        val filaDelResumen = resumirInventario(lotesEjemplo(), 3000.0).tipos.first { it.tipo == TipoCafe.VERDE }
        assertEquals(filaDelResumen.pesoKg, calidades.sumOf { it.pesoKg }, 1e-9)
        assertEquals(filaDelResumen.cantidadSublotes, calidades.sumOf { it.cantidadSublotes })
    }

    @Test fun cadaCalidadTraeSuPesoSusSublotesYSuUltimoIngreso() {
        val regular = resumirCalidades(lotesEjemplo(), TipoCafe.VERDE).first { it.calidad == Calidad.REGULAR }
        assertEquals(200.0, regular.pesoKg, 1e-9)            // 120 + 80
        assertEquals(2, regular.cantidadSublotes)
        assertEquals(Fecha(1, 11, 2026), regular.ultimoIngreso)
    }

    @Test fun elSubloteAgotadoNoCuentaEnSuCalidad() {
        val buena = resumirCalidades(lotesEjemplo(), TipoCafe.SECO).first { it.calidad == Calidad.BUENA }
        assertEquals(300.0, buena.pesoKg, 1e-9)               // 200 + 100; el sublote 7 está agotado
        assertEquals(2, buena.cantidadSublotes)
    }

    @Test fun elOrdenPuedeSerMasRecienteOMasAntiguo() {
        assertEquals(
            listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA),
            resumirCalidades(lotesEjemplo(), TipoCafe.VERDE, OrdenInventario.MAS_RECIENTE).map { it.calidad },
        )
        assertEquals(
            listOf(Calidad.MALA, Calidad.REGULAR, Calidad.BUENA),
            resumirCalidades(lotesEjemplo(), TipoCafe.VERDE, OrdenInventario.MAS_ANTIGUO).map { it.calidad },
        )
    }

    @Test fun unTipoSinLotesDevuelveUnaListaVacia() {
        assertTrue(resumirCalidades(lotesEjemplo(), TipoCafe.PASILLA).isEmpty())
        assertTrue(resumirCalidades(emptyList(), TipoCafe.VERDE).isEmpty())
    }

    @Test fun laCalidadSinCafeVaAlFinalEnCualquierOrden() {
        val vacia = LoteConSublotes(Lote(9, TipoCafe.PASILLA, Calidad.BUENA), emptyList())
        val conCafe = LoteConSublotes(
            Lote(10, TipoCafe.PASILLA, Calidad.MALA),
            listOf(sublote(1, 10, 20.0, fecha = Fecha(5, 11, 2026))),
        )
        for (orden in OrdenInventario.entries) {
            val filas = resumirCalidades(listOf(vacia, conCafe), TipoCafe.PASILLA, orden)
            assertEquals(listOf(Calidad.MALA, Calidad.BUENA), filas.map { it.calidad })
        }
        val sin = resumirCalidades(listOf(vacia), TipoCafe.PASILLA).single()
        assertEquals(0.0, sin.pesoKg, 0.0)
        assertEquals(0, sin.cantidadSublotes)
        assertNull(sin.ultimoIngreso)
    }

    @Test fun siDosCalidadesEmpatanSeQuedaElOrdenBuenaRegularMala() {
        val mismaFecha = Fecha(3, 11, 2026)
        val lotes = listOf(
            LoteConSublotes(Lote(3, TipoCafe.SECO, Calidad.MALA), listOf(sublote(3, 3, 10.0, fecha = mismaFecha))),
            LoteConSublotes(Lote(1, TipoCafe.SECO, Calidad.BUENA), listOf(sublote(1, 1, 10.0, fecha = mismaFecha))),
            LoteConSublotes(Lote(2, TipoCafe.SECO, Calidad.REGULAR), listOf(sublote(2, 2, 10.0, fecha = mismaFecha))),
        )
        assertEquals(
            listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA),
            resumirCalidades(lotes, TipoCafe.SECO).map { it.calidad },
        )
    }

    // ── Sublotes del grupo: sublotesDelLote ────────────────

    @Test fun losSublotesDeUnGrupoSalenConSuLoteYElMasRecientePrimero() {
        val sublotes = sublotesDelLote(lotesEjemplo(), loteId = 2)
        assertEquals(listOf(2L, 3L), sublotes.map { it.sublote.id })   // 1 nov y 28 oct
        assertTrue(sublotes.all { it.tipoCafe == TipoCafe.VERDE && it.calidad == Calidad.REGULAR })
    }

    @Test fun elOrdenMasAntiguoInvierteLaLista() {
        val sublotes = sublotesDelLote(lotesEjemplo(), loteId = 2, orden = OrdenInventario.MAS_ANTIGUO)
        assertEquals(listOf(3L, 2L), sublotes.map { it.sublote.id })
    }

    @Test fun losSublotesAgotadosNoSeListan() {
        // Seco · Buena tiene tres sublotes (5, 6 y 7) pero el 7 está agotado.
        val sublotes = sublotesDelLote(lotesEjemplo(), loteId = 4)
        assertEquals(listOf(6L, 5L), sublotes.map { it.sublote.id })
    }

    @Test fun siEntraronElMismoDiaElMasNuevoVaPrimero() {
        val dia = Fecha(8, 11, 2026)
        val lotes = listOf(
            LoteConSublotes(
                Lote(1, TipoCafe.VERDE, Calidad.BUENA),
                listOf(sublote(4, 1, 10.0, fecha = dia), sublote(9, 1, 10.0, fecha = dia), sublote(6, 1, 10.0, fecha = dia)),
            )
        )
        assertEquals(listOf(9L, 6L, 4L), sublotesDelLote(lotes, 1).map { it.sublote.id })
        assertEquals(
            listOf(4L, 6L, 9L),
            sublotesDelLote(lotes, 1, OrdenInventario.MAS_ANTIGUO).map { it.sublote.id },
        )
    }

    @Test fun unLoteQueNoExisteDevuelveUnaListaVacia() {
        assertTrue(sublotesDelLote(lotesEjemplo(), loteId = 999).isEmpty())
        assertTrue(sublotesDelLote(emptyList(), loteId = 1).isEmpty())
    }
}
