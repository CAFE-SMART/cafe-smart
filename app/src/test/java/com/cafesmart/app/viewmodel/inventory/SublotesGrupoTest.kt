package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Agrupamiento por calidad y totales de "Sublotes del grupo".
class SublotesGrupoTest {
    private val hoy = Fecha(3, 11, 2026)

    private fun sublote(id: Long, loteId: Long, kg: Double, costo: Long, dia: Int, humedad: Double? = null) = Sublote(
        id = id, loteId = loteId, compraId = id, pesoInicialKg = 500.0, pesoDisponibleKg = kg,
        costoKg = costo, fechaIngreso = Fecha(dia, 11, 2026), humedadPorcentaje = humedad,
    )

    private val lotes = listOf(
        LoteConSublotes(
            Lote(1, TipoCafe.VERDE, Calidad.BUENA),
            listOf(sublote(1, 1, 60.0, 16_000, 3)),
        ),
        LoteConSublotes(
            Lote(2, TipoCafe.VERDE, Calidad.REGULAR),
            listOf(
                sublote(2, 2, 120.0, 14_000, 3, humedad = 12.0),
                sublote(3, 2, 80.0, 13_000, 1),
                sublote(4, 2, 0.0, 99_000, 1), // agotado
            ),
        ),
    )

    private fun grupo(loteId: Long, orden: OrdenInventario = OrdenInventario.MAS_RECIENTE) =
        armarGrupo(lotes, loteId, orden, KG_POR_BULTO, hoy)

    @Test fun cadaCalidadSumaSoloSusSublotesConCafe() {
        val calidades = resumirCalidades(lotes, TipoCafe.VERDE).associateBy { it.calidad }
        assertEquals(60.0, calidades.getValue(Calidad.BUENA).pesoKg, 1e-9)
        assertEquals(200.0, calidades.getValue(Calidad.REGULAR).pesoKg, 1e-9)
        assertEquals(2, calidades.getValue(Calidad.REGULAR).cantidadSublotes)
    }

    @Test fun losTotalesDelGrupoIgnoranElAgotado() {
        val g = grupo(2)!!
        assertEquals("200 kg", g.pesoTotal)
        assertEquals("4,9 bultos", g.bultos)
        assertEquals(2, g.cantidad)
        assertEquals("$ 2.720.000", g.valorTotal)      // 120×14.000 + 80×13.000
        assertEquals("$ 13.600 por kg", g.costoPromedio) // ponderado por kg
        assertEquals("Hace 2 días", g.masAntiguo)
        assertEquals("12 %", g.humedadPromedio)        // solo el que tiene dato
    }

    @Test fun lasFilasSiguenElOrdenElegido() {
        assertEquals(listOf(2L, 3L), grupo(2)!!.filas.map { it.id })
        assertEquals(listOf(3L, 2L), grupo(2, OrdenInventario.MAS_ANTIGUO)!!.filas.map { it.id })
    }

    @Test fun sinDatosDeCalidadSeIndica() {
        val g = grupo(1)!!
        assertEquals("Sin dato", g.humedadPromedio)
        assertEquals("sin dato", g.filas.single().factor)
        assertEquals("Hoy", g.filas.single().antiguedad)
    }

    @Test fun unGrupoQueNoExisteOSinCafeEsVacio() {
        assertNull(grupo(99))
        val agotado = listOf(LoteConSublotes(lotes[0].lote, listOf(sublote(9, 1, 0.0, 1, 1))))
        assertNull(armarGrupo(agotado, 1, OrdenInventario.MAS_RECIENTE, KG_POR_BULTO, hoy))
    }

    @Test fun elFiltroSoloMuestraElTipoYLaCalidadElegidos() {
        val conSeco = lotes + LoteConSublotes(
            Lote(3, TipoCafe.SECO, Calidad.REGULAR),
            listOf(sublote(5, 3, 40.0, 21_000, 2)),
        )
        // Misma calidad en otro tipo: no se mezclan.
        assertEquals(listOf(40.0), resumirCalidades(conSeco, TipoCafe.SECO).map { it.pesoKg })
        val seco = armarGrupo(conSeco, 3, OrdenInventario.MAS_RECIENTE, KG_POR_BULTO, hoy)!!
        assertEquals(TipoCafe.SECO, seco.tipo)
        assertEquals(Calidad.REGULAR, seco.calidad)
        assertEquals(listOf(5L), seco.filas.map { it.id })
        val verde = armarGrupo(conSeco, 2, OrdenInventario.MAS_RECIENTE, KG_POR_BULTO, hoy)!!
        assertEquals(listOf(2L, 3L), verde.filas.map { it.id })
    }
}
