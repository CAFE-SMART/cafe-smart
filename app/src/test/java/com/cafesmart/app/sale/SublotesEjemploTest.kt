package com.cafesmart.app.sale

import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.data.sale.sublotesParaVenderEjemplo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SublotesEjemploTest {

    private val sublotes = sublotesParaVenderEjemplo()

    @Test
    fun ningunSubloteEstaAgotado() {
        assertTrue(sublotes.none { it.sublote.estaAgotado })
    }

    @Test
    fun elSubloteAgotadoDelInventarioNoSeOfrece() {
        // En el inventario de ejemplo el sublote 7 es seco y esta agotado
        assertFalse(sublotes.any { it.sublote.id == 7L })
    }

    @Test
    fun loSecoSumaLos521KgDelFigma() {
        assertEquals(521.0, sublotes.filter { it.tipoCafe == TipoCafe.SECO }.sumOf { it.sublote.pesoDisponibleKg }, 0.0001)
    }

    @Test
    fun loVerdeSumaLos290KgDelFigma() {
        assertEquals(290.0, sublotes.filter { it.tipoCafe == TipoCafe.VERDE }.sumOf { it.sublote.pesoDisponibleKg }, 0.0001)
    }

    @Test
    fun noHayPasillaParaVender() {
        assertTrue(sublotes.none { it.tipoCafe == TipoCafe.PASILLA })
    }

    @Test
    fun cadaSubloteTieneIdDistinto() {
        assertEquals(sublotes.size, sublotes.map { it.sublote.id }.toSet().size)
    }
}
