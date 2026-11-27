package com.cafesmart.app.viewmodel.inventory

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.LoteConSublotes
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.TipoCafe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// "Inventario · Detalle · Lista de sublotes": totales, promedios y filas salen de los sublotes con café del lote.
class SublotesGrupoTest {
    private val hoy = Fecha(23, 11, 2026)
    private val lote = Lote(7, TipoCafe.VERDE, Calidad.REGULAR)
    private fun s(id: Long, kg: Double, costo: Long, fecha: Fecha, humedad: Double? = null, factor: Int? = null, inicial: Double = 200.0) =
        Sublote(id, 7, 1, null, inicial, kg, costo, fecha, humedad, factor)

    // El ejemplo del Figma: 120 kg + 80 kg a $ 14.000 = 200 kg, $ 2.800.000, 2 sublotes, el más antiguo hace 2 días.
    @Test fun ejemploDelFigma() {
        val g = armarGrupo(listOf(LoteConSublotes(lote, listOf(s(1, 120.0, 14_000, hoy), s(2, 80.0, 14_000, Fecha(21, 11, 2026))))),
            7, hoy, pesoBultoKg = 40.7)!!
        assertEquals("200 kg", g.pesoTotal)
        assertEquals("4,9 bultos", g.bultosTotal)
        assertEquals(2, g.cantidad)
        assertEquals("$ 2.800.000", g.valorTotal)
        assertEquals("$ 14.000 por kg", g.costoPromedio)
        assertEquals("Hace 2 días", g.masAntiguo)
        assertEquals("Sin dato", g.humedadPromedio)
        assertEquals(listOf(1L, 2L), g.sublotes.map { it.id }) // más reciente primero
        val f = g.sublotes.first()
        assertEquals("Sublote #1", f.titulo)
        assertEquals("Hoy", f.antiguedad)
        assertEquals("120 kg", f.peso)
        assertEquals("2,9 bultos", f.bultos)
        assertEquals("$ 1.680.000", f.valor)
        assertEquals("Humedad: sin dato", f.humedad)
        assertEquals("Factor: sin dato", f.factor)
    }

    @Test fun costoPromedioPonderadoYHumedadSoloConDatos() {
        val g = armarGrupo(listOf(LoteConSublotes(lote, listOf(
            s(1, 100.0, 10_000, hoy, humedad = 12.0, factor = 95),
            s(2, 300.0, 14_000, hoy, humedad = null, inicial = 300.0),
            s(3, 0.0, 99_000, hoy, humedad = 8.0, inicial = 50.0), // agotado: no cuenta
        ))), 7, hoy, pesoBultoKg = 50.0)!!
        assertEquals("$ 13.000 por kg", g.costoPromedio) // (1.000.000 + 4.200.000) / 400
        assertEquals("12 %", g.humedadPromedio)
        assertEquals(2, g.cantidad)
        assertEquals("8,0 bultos", g.bultosTotal)
        assertEquals("Factor: 95", g.sublotes.first { it.id == 1L }.factor)
    }

    @Test fun loteSinCafeYLoteInexistente() {
        val g = armarGrupo(listOf(LoteConSublotes(lote, listOf(s(1, 0.0, 10_000, hoy)))), 7, hoy)!!
        assertTrue(g.sublotes.isEmpty())
        assertEquals("0 kg", g.pesoTotal)
        assertEquals("Sin dato", g.costoPromedio)
        assertEquals("Sin dato", g.masAntiguo)
        assertNull(armarGrupo(emptyList(), 7, hoy))
    }

    @Test fun antiguedad() {
        assertEquals("Hoy", textoAntiguedad(hoy, hoy))
        assertEquals("Hoy", textoAntiguedad(Fecha(24, 11, 2026), hoy))
        assertEquals("Hace 1 día", textoAntiguedad(Fecha(22, 11, 2026), hoy))
        assertEquals("Hace 31 días", textoAntiguedad(Fecha(23, 10, 2026), hoy))
    }
}
