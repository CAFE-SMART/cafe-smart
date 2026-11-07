package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.Lote
import com.cafesmart.app.data.model.Sublote
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleViewModelTest {

    // Sublotes del Figma "Ventas - Paso 2" (cafe seco)
    private fun subloteDe(id: Long, calidad: Calidad, disponibleKg: Double) = SubloteConLote(
        Sublote(
            id = id,
            loteId = id,
            compraId = id,
            pesoInicialKg = disponibleKg,
            pesoDisponibleKg = disponibleKg,
            costoKg = 18_000,
            fechaIngreso = Fecha(1, 11, 2026)
        ),
        Lote(id, TipoCafe.SECO, calidad)
    )

    private val sublote5 = subloteDe(5, Calidad.BUENA, 200.0)
    private val sublote6 = subloteDe(6, Calidad.REGULAR, 180.0)
    private val sublote7 = subloteDe(7, Calidad.REGULAR, 100.0)
    private val sublote8 = subloteDe(8, Calidad.MALA, 41.0)

    @Test
    fun alEmpezar_noHayClienteNiSublotesYNoSePuedeContinuar() {
        val estado = SaleViewModel().uiState.value
        assertNull(estado.cliente)
        assertTrue(estado.elegidos.isEmpty())
        assertFalse(estado.puedeContinuar)
        assertEquals("0 kg", estado.totalKgTexto)
        assertEquals("$ 0", estado.totalARecibirTexto)
    }

    @Test
    fun elegirCliente_loGuardaEnElEstado() {
        val viewModel = SaleViewModel()
        val cliente = Cliente(nombre = "Carlos Mejía", documento = "16780012")
        viewModel.elegirCliente(cliente)
        assertEquals(cliente, viewModel.uiState.value.cliente)
    }

    @Test
    fun alMarcarUnSublote_seVendeTodoLoDisponible() {
        val viewModel = SaleViewModel()
        viewModel.alternarSublote(sublote5)
        val estado = viewModel.uiState.value
        assertEquals(200.0, estado.elegidos[0].cantidadKg, 0.001)
        assertEquals("200 kg", estado.totalKgTexto)
    }

    @Test
    fun alMarcarDosVeces_elSubloteSeQuita() {
        val viewModel = SaleViewModel()
        viewModel.alternarSublote(sublote5)
        viewModel.alternarSublote(sublote5)
        assertTrue(viewModel.uiState.value.elegidos.isEmpty())
    }

    @Test
    fun cambiarCantidad_aceptaLoValidoEIgnoraLoDemas() {
        val viewModel = SaleViewModel()
        viewModel.alternarSublote(sublote5)
        viewModel.cambiarCantidad(5, 150.0)
        assertEquals(150.0, viewModel.uiState.value.elegidos[0].cantidadKg, 0.001)
        // Mas de lo disponible o 0: se queda en 150
        viewModel.cambiarCantidad(5, 250.0)
        viewModel.cambiarCantidad(5, 0.0)
        assertEquals(150.0, viewModel.uiState.value.elegidos[0].cantidadKg, 0.001)
    }

    @Test
    fun venderUnaParte_calculaElTotalDelFigma() {
        val viewModel = SaleViewModel()
        viewModel.alternarSublote(sublote5)
        viewModel.cambiarCantidad(5, 150.0)
        // El campo muestra "20.000" y se guarda solo con digitos
        viewModel.cambiarPrecio(Calidad.BUENA, "20.000")
        val estado = viewModel.uiState.value
        assertEquals("20000", estado.precios[Calidad.BUENA])
        assertEquals("150 kg", estado.totalKgTexto)
        assertEquals("$ 3.000.000", estado.totalARecibirTexto)
    }

    @Test
    fun venderTodo_cadaCalidadUsaSuPrecio() {
        val viewModel = SaleViewModel()
        listOf(sublote5, sublote6, sublote7, sublote8).forEach { viewModel.alternarSublote(it) }
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.cambiarPrecio(Calidad.REGULAR, "18000")
        viewModel.cambiarPrecio(Calidad.MALA, "14000")
        val estado = viewModel.uiState.value
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), estado.calidadesElegidas)
        assertEquals("521 kg", estado.totalKgTexto)
        assertEquals("$ 9.614.000", estado.totalARecibirTexto)
    }

    @Test
    fun continuar_necesitaUnPrecioValidoPorCadaCalidadElegida() {
        val viewModel = SaleViewModel()
        viewModel.alternarSublote(sublote5)
        viewModel.alternarSublote(sublote6)
        assertFalse(viewModel.uiState.value.puedeContinuar)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        // Falta el precio de la calidad regular
        assertFalse(viewModel.uiState.value.puedeContinuar)
        viewModel.cambiarPrecio(Calidad.REGULAR, "999")
        assertFalse(viewModel.uiState.value.puedeContinuar)
        viewModel.cambiarPrecio(Calidad.REGULAR, "18000")
        assertTrue(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun precioFueraDeRango_muestraElMensajeDeCompras() {
        val viewModel = SaleViewModel()
        viewModel.cambiarPrecio(Calidad.BUENA, "100001")
        assertEquals(
            "Escribe un valor entre \$1.000 y \$100.000 pesos colombianos.",
            viewModel.uiState.value.errorDePrecio(Calidad.BUENA)
        )
        assertNull(viewModel.uiState.value.errorDePrecio(Calidad.REGULAR))
    }
}
