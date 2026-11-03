package com.cafesmart.app.viewmodel.drying

import org.junit.Assert.assertEquals
import org.junit.Test

class MermaCalculatorTest {

    @Test
    fun casoNormal() {
        assertEquals(20.0, calcularMermaPorcentaje(100.0, 80.0), 1e-9)
    }

    @Test
    fun mermaCero() {
        assertEquals(0.0, calcularMermaPorcentaje(50.0, 50.0), 1e-9)
    }

    @Test
    fun mermaCien() {
        assertEquals(100.0, calcularMermaPorcentaje(50.0, 0.0), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun kgInicialCeroLanzaExcepcion() {
        calcularMermaPorcentaje(0.0, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun kgInicialNegativoLanzaExcepcion() {
        calcularMermaPorcentaje(-10.0, 5.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun kgFinalNegativoLanzaExcepcion() {
        calcularMermaPorcentaje(100.0, -1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun kgFinalMayorQueInicialLanzaExcepcion() {
        calcularMermaPorcentaje(100.0, 101.0)
    }
}
