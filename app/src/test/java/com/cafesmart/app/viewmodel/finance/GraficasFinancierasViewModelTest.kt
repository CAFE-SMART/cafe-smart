package com.cafesmart.app.viewmodel.finance

import com.cafesmart.app.data.finanzas.GastoRegistrado
import com.cafesmart.app.data.finanzas.OrigenFinanciero
import com.cafesmart.app.data.finanzas.SinDatosFinancieros
import com.cafesmart.app.data.finanzas.VentaRegistrada
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.ui.finance.alturaRelativa
import com.cafesmart.app.ui.finance.descripcionGrafica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Gráficas financieras: el ViewModel lee el mismo origen que el Resumen y arma la serie de los últimos 6 meses.
class GraficasFinancierasViewModelTest {
    @Before fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun despues() = Dispatchers.resetMain()

    private val hoy = Fecha(26, 11, 2026)

    @Test fun sinDatosMuestraElEstadoVacio() {
        val e = GraficasFinancierasViewModel(SinDatosFinancieros, { hoy }).estado.value
        assertFalse(e.cargando)
        assertTrue(e.vacia)
        assertEquals(6, e.serie.size)
    }

    @Test fun conDatosArmaLosUltimosSeisMesesEnPesos() {
        val origen = object : OrigenFinanciero {
            override suspend fun ventas() = listOf(
                VentaRegistrada(Fecha(10, 11, 2026), kgVendidos = 100.0, precioVentaKg = 15_000.0, precioCompraKg = 14_000.0, gastos = 20_000.0),
            )
            override suspend fun gastos() = listOf(
                GastoRegistrado(Fecha(3, 11, 2026), 20_000), GastoRegistrado(Fecha(5, 9, 2026), 45_000),
                GastoRegistrado(Fecha(1, 1, 2026), 99_000), // fuera de la ventana de 6 meses
            )
        }
        val e = GraficasFinancierasViewModel(origen, { hoy }).estado.value
        assertFalse(e.vacia)
        assertEquals(listOf(6, 7, 8, 9, 10, 11), e.serie.map { it.mes })
        val noviembre = e.serie.last()
        assertEquals(1_500_000L, noviembre.ingresosPesos)
        // El gasto del sublote se cuenta UNA vez en "gastos"; la utilidad ya lo descuenta (no se suma al costo por kg).
        assertEquals(20_000L, noviembre.gastosPesos)
        assertEquals(1_500_000L - 1_400_000L - 20_000L, noviembre.utilidadPesos)
        assertEquals(45_000L, e.serie.first { it.mes == 9 }.gastosPesos)
    }

    @Test fun siFallaLaLecturaMuestraError() {
        val origen = object : OrigenFinanciero { override suspend fun gastos(): List<GastoRegistrado> = error("Room") }
        val e = GraficasFinancierasViewModel(origen, { hoy }).estado.value
        assertEquals(ERROR_GRAFICAS, e.error)
        assertFalse(e.vacia)
    }

    @Test fun alturasYDescripcion() {
        assertEquals(0.5f, alturaRelativa(50, 100))
        assertEquals(-0.25f, alturaRelativa(-25, 100))
        assertEquals(0f, alturaRelativa(10, 0))
        val d = descripcionGrafica(listOf(MesFinanciero(2026, 11, 1_500_000, 20_000, 80_000)))
        assertTrue(d, d.startsWith("Nov: ingresos $ 1.500.000, gastos $ 20.000, utilidad $ 80.000"))
    }
}
