package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.model.Fecha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Gráficas financieras (lógica): agrupación mensual, cambio de año, meses sin datos y cálculos.
class SerieMensualTest {
    private fun venta(d: Int, m: Int, a: Int, kg: Double, venta: Double, compra: Double, gastos: Double = 0.0) =
        VentaRegistrada(Fecha(d, m, a), kg, venta, compra, gastos)
    private fun gasto(d: Int, m: Int, a: Int, pesos: Long) = GastoRegistrado(Fecha(d, m, a), pesos)

    @Test fun ventanaDeMesesCruzaElAnio() {
        assertEquals(listOf(2026 to 11, 2026 to 12, 2027 to 1, 2027 to 2), ultimosMeses(Fecha(15, 2, 2027), 4))
        assertEquals(listOf(2026 to 11), ultimosMeses(Fecha(1, 11, 2026), 1))
        assertThrows(IllegalArgumentException::class.java) { ultimosMeses(Fecha(1, 1, 2026), 0) }
    }

    @Test fun agrupaPorMesYCalculaIgualQueElResumen() {
        val serie = serieMensual(
            ventas = listOf(
                venta(3, 11, 2026, 100.0, 15_000.0, 14_000.0, gastos = 20_000.0), // ingresos 1.500.000, utilidad 80.000
                venta(28, 11, 2026, 10.0, 20_000.0, 14_000.0),                     // ingresos 200.000, utilidad 60.000
                venta(1, 10, 2026, 50.0, 16_000.0, 14_000.0),                      // octubre: 800.000 / 100.000
            ),
            gastos = listOf(gasto(5, 11, 2026, 35_000), gasto(30, 10, 2026, 2_000)),
            hoy = Fecha(23, 11, 2026), meses = 3,
        )
        assertEquals(listOf(9, 10, 11), serie.map { it.mes })
        assertEquals(MesFinanciero(2026, 9, 0, 0, 0), serie[0])
        assertTrue(serie[0].sinDatos)
        assertEquals(MesFinanciero(2026, 10, 800_000, 2_000, 100_000), serie[1])
        assertEquals(MesFinanciero(2026, 11, 1_700_000, 35_000, 140_000), serie[2])
        // La utilidad del mes es la misma suma que haría resumirVentas con esas ventas.
        val noviembre = listOf(calcularUtilidad(100.0, 15_000.0, 14_000.0, 20_000.0), calcularUtilidad(10.0, 20_000.0, 14_000.0))
        assertEquals(noviembre.sumOf { it.utilidadPesos }, serie[2].utilidadPesos)
    }

    @Test fun loQueCaeFueraDeLaVentanaNoCuenta() {
        val serie = serieMensual(
            ventas = listOf(venta(31, 12, 2025, 10.0, 15_000.0, 1_000.0), venta(1, 3, 2027, 10.0, 15_000.0, 1_000.0)),
            gastos = listOf(gasto(1, 1, 2026, 999)),
            hoy = Fecha(10, 2, 2027), meses = 12,
        )
        assertEquals(2026 to 3, serie.first().anio to serie.first().mes)
        assertTrue(serie.all { it.sinDatos })
    }

    @Test fun perdidaDaUtilidadNegativaYEscala() {
        val serie = serieMensual(listOf(venta(1, 1, 2027, 10.0, 10_000.0, 14_000.0)), emptyList(), Fecha(31, 1, 2027), meses = 2)
        assertEquals(-40_000L, serie.last().utilidadPesos)
        assertEquals(100_000L, maximoDeLaSerie(serie))
        assertEquals(1L, maximoDeLaSerie(serieMensual(emptyList(), emptyList(), Fecha(31, 1, 2027), 2)))
    }

    @Test fun etiquetas() {
        val enero = MesFinanciero(2027, 1, 0, 0, 0)
        assertEquals("Ene", etiquetaMes(enero))
        assertEquals("Ene 2027", etiquetaMes(enero, conAnio = true))
        assertEquals("Dic", etiquetaMes(MesFinanciero(2026, 12, 0, 0, 0)))
    }
}
