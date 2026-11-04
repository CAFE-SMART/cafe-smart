package com.cafesmart.app.viewmodel.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitCalculatorTest {

    // --- Ejemplos del ERS (apartado 2.3) ---

    @Test
    fun ejemploCooperativaDelErs() {
        // Ingreso 12.000.000, costo 10.500.000, gastos 400.000 -> 1.100.000
        val r = calcularUtilidad(kgVendidos = 1000.0, precioVentaKg = 12000.0, precioCompraKg = 10500.0, gastos = 400_000.0)
        assertEquals(12_000_000L, r.ingresosPesos)
        assertEquals(10_500_000L, r.costoCompraPesos)
        assertEquals(400_000L, r.gastosPesos)
        assertEquals(1_100_000L, r.utilidadPesos)
    }

    @Test
    fun ejemploCompraventaDelErs() {
        // Compra 9.000/kg, gastos 300/kg (300.000 en 1.000 kg), venta 10.200/kg -> margen 900/kg -> 900.000
        val r = calcularUtilidad(1000.0, 10_200.0, 9_000.0, 300_000.0)
        assertEquals(900_000L, r.utilidadPesos)
    }

    // --- Ganancia, pérdida y equilibrio ---

    @Test
    fun ganancia() {
        val r = calcularUtilidad(100.0, 12_000.0, 10_000.0, 50_000.0)
        assertEquals(150_000L, r.utilidadPesos)
        assertTrue(r.esGanancia)
        assertFalse(r.esPerdida)
    }

    @Test
    fun perdidaDaUtilidadNegativa() {
        val r = calcularUtilidad(100.0, 9_000.0, 10_000.0, 0.0)
        assertEquals(-100_000L, r.utilidadPesos)
        assertTrue(r.esPerdida)
        assertFalse(r.esGanancia)
    }

    @Test
    fun perdidaPorLosGastos() {
        // Vende por encima del costo, pero los gastos se comen la ganancia.
        val r = calcularUtilidad(100.0, 10_500.0, 10_000.0, 80_000.0)
        assertEquals(-30_000L, r.utilidadPesos)
        assertTrue(r.esPerdida)
    }

    @Test
    fun puntoDeEquilibrioNoEsGananciaNiPerdida() {
        val r = calcularUtilidad(100.0, 10_000.0, 9_000.0, 100_000.0)
        assertEquals(0L, r.utilidadPesos)
        assertFalse(r.esGanancia)
        assertFalse(r.esPerdida)
    }

    // --- Gastos ---

    @Test
    fun sinGastosPorDefecto() {
        val r = calcularUtilidad(50.0, 12_000.0, 10_000.0)
        assertEquals(0L, r.gastosPesos)
        assertEquals(100_000L, r.utilidadPesos)
    }

    @Test
    fun losGastosSeRestanNoSeSuman() {
        val sin = calcularUtilidad(100.0, 12_000.0, 10_000.0, 0.0).utilidadPesos
        val con = calcularUtilidad(100.0, 12_000.0, 10_000.0, 40_000.0).utilidadPesos
        assertEquals(40_000L, sin - con)
    }

    // --- Redondeo a pesos enteros ---

    @Test
    fun kgConDecimalesSeRedondeaAPesos() {
        // 12,345 kg * 10.200 = 125.919 ; costo 12,345 * 9.000 = 111.105 ; utilidad 14.814
        val r = calcularUtilidad(12.345, 10_200.0, 9_000.0)
        assertEquals(125_919L, r.ingresosPesos)
        assertEquals(111_105L, r.costoCompraPesos)
        assertEquals(14_814L, r.utilidadPesos)
    }

    @Test
    fun redondeaHaciaArribaDesdeMedioPeso() {
        // 0,5 kg * 1.001 = 500,5 -> 501 ; sin costo ni gastos
        assertEquals(501L, calcularUtilidad(0.5, 1_001.0, 0.0).ingresosPesos)
    }

    @Test
    fun redondeaHaciaAbajoSiNoLlegaAMedioPeso() {
        // 0,333 kg * 1.000 = 333 exacto; 0,3334 * 1.000 = 333,4 -> 333
        assertEquals(333L, calcularUtilidad(0.3334, 1_000.0, 0.0).ingresosPesos)
    }

    @Test
    fun gastosConDecimalesSeRedondean() {
        assertEquals(1_235L, calcularUtilidad(0.0, 0.0, 0.0, 1_234.6).gastosPesos)
    }

    @Test
    fun laUtilidadSeRedondeaUnaSolaVezAlFinal() {
        // Ingreso 100,4 -> 100 y costo 50,6 -> 51: redondeando por partes la utilidad sería 49.
        // Exacta: 100,4 - 50,6 = 49,8 -> 50. La utilidad se redondea una sola vez, al final.
        val r = calcularUtilidad(1.0, 100.4, 50.6, 0.0)
        assertEquals(100L, r.ingresosPesos)
        assertEquals(51L, r.costoCompraPesos)
        assertEquals(50L, r.utilidadPesos)
    }

    // --- Casos límite ---

    @Test
    fun ventaDeCeroKgTodoEnCero() {
        val r = calcularUtilidad(0.0, 12_000.0, 10_000.0, 0.0)
        assertEquals(ResultadoUtilidad(0L, 0L, 0L, 0L), r)
    }

    @Test
    fun ventaDeCeroKgConGastosEsPerdidaPorLosGastos() {
        assertEquals(-5_000L, calcularUtilidad(0.0, 12_000.0, 10_000.0, 5_000.0).utilidadPesos)
    }

    @Test
    fun precioDeVentaCeroEsPerdidaPorElCosto() {
        assertEquals(-1_000_000L, calcularUtilidad(100.0, 0.0, 10_000.0).utilidadPesos)
    }

    @Test
    fun precioDeCompraCeroEsTodoGanancia() {
        assertEquals(1_200_000L, calcularUtilidad(100.0, 12_000.0, 0.0).utilidadPesos)
    }

    @Test
    fun cantidadesGrandes() {
        val r = calcularUtilidad(50_000.0, 12_000.0, 10_000.0, 5_000_000.0)
        assertEquals(95_000_000L, r.utilidadPesos)
    }

    @Test
    fun resultadoEsComparablePorValor() {
        assertEquals(
            ResultadoUtilidad(1_200_000L, 1_000_000L, 50_000L, 150_000L),
            calcularUtilidad(100.0, 12_000.0, 10_000.0, 50_000.0),
        )
    }

    // --- Validaciones (RNF-07: rechazar valores negativos) ---

    @Test(expected = IllegalArgumentException::class)
    fun kgNegativosLanzaExcepcion() {
        calcularUtilidad(-1.0, 12_000.0, 10_000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun precioVentaNegativoLanzaExcepcion() {
        calcularUtilidad(1.0, -12_000.0, 10_000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun precioCompraNegativoLanzaExcepcion() {
        calcularUtilidad(1.0, 12_000.0, -10_000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun gastosNegativosLanzaExcepcion() {
        calcularUtilidad(1.0, 12_000.0, 10_000.0, -1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nanLanzaExcepcion() {
        calcularUtilidad(Double.NaN, 12_000.0, 10_000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun infinitoLanzaExcepcion() {
        calcularUtilidad(1.0, Double.POSITIVE_INFINITY, 10_000.0)
    }
}
