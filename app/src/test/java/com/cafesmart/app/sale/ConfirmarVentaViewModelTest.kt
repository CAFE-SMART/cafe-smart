package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Cliente
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoDocumento
import com.cafesmart.app.data.sale.ModoCliente
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import com.cafesmart.app.viewmodel.purchase.fechaDeHoy
import com.cafesmart.app.viewmodel.sale.SaleUiState
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Paso 3 de Ventas (confirmar) y pantalla "Venta registrada".
class ConfirmarVentaViewModelTest {

    private val exportadora = Cliente(nombre = "Exportadora del Sur S.A.", documento = "9003421215", tipoDocumento = TipoDocumento.NIT)

    // Una venta lista para confirmar: cliente registrado, 150 kg del sublote 5 a $ 20.000.
    private fun ventaLista(): SaleViewModel {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(exportadora)
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "150")
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        return viewModel
    }

    private fun SaleViewModel.disponibleDelSublote(id: Long) = sublote(id).sublote.pesoDisponibleKg

    // ---------- Cliente y fecha en el resumen ----------

    @Test
    fun clienteRegistrado_apareceConSuNombreYDocumento() {
        val estado = ventaLista().uiState.value
        assertEquals("Exportadora del Sur S.A.", estado.clienteTexto)
        assertEquals("NIT 9003421215", estado.clienteDocumentoTexto)
    }

    @Test
    fun clienteConCedula_muestraCedula() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegirCliente(Cliente(nombre = "Carlos Mejía", documento = "16780012"))
        assertEquals("Cédula 16780012", viewModel.uiState.value.clienteDocumentoTexto)
    }

    @Test
    fun ventaSinRegistrar_apareceComoSinRegistrarYSinDocumento() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        assertEquals("Sin registrar", viewModel.uiState.value.clienteTexto)
        assertNull(viewModel.uiState.value.clienteDocumentoTexto)
    }

    @Test
    fun sinRegistrar_ignoraUnClienteElegidoAntes() {
        val viewModel = SaleViewModel()
        viewModel.elegirCliente(exportadora)
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        assertEquals("Sin registrar", viewModel.uiState.value.clienteTexto)
    }

    @Test
    fun laFechaPorDefectoEsHoy() {
        val estado = SaleViewModel().uiState.value
        assertEquals(fechaDeHoy(), estado.fecha)
        assertEquals(estado.hoy, estado.fecha)
    }

    @Test
    fun cambiarFecha_usaLosMilisDelCalendario() {
        val viewModel = SaleViewModel()
        viewModel.cambiarFecha(fechaAMilis(Fecha(3, 11, 2026)))
        assertEquals(Fecha(3, 11, 2026), viewModel.uiState.value.fecha)
        assertEquals("3 de noviembre de 2026", viewModel.uiState.value.fechaResumenTexto)
    }

    @Test
    fun fechaMilis_esLaFechaElegidaParaElCalendario() {
        val viewModel = SaleViewModel()
        viewModel.cambiarFecha(fechaAMilis(Fecha(3, 11, 2026)))
        assertEquals(fechaAMilis(Fecha(3, 11, 2026)), viewModel.uiState.value.fechaMilis)
    }

    // ---------- Validacion de la fecha ----------

    @Test
    fun fechaFutura_muestraErrorYNoDejaConfirmar() {
        val viewModel = ventaLista()
        viewModel.cambiarFecha(fechaAMilis(Fecha(1, 1, 2099)))
        assertEquals("Solo puedes registrar ventas desde 2026 hasta hoy.", viewModel.uiState.value.errorDeFecha)
        assertFalse(viewModel.uiState.value.puedeConfirmar)
    }

    @Test
    fun fechaAnteriorA2026_muestraErrorYNoDejaConfirmar() {
        val viewModel = ventaLista()
        viewModel.cambiarFecha(fechaAMilis(Fecha(31, 12, 2025)))
        assertEquals("Solo puedes registrar ventas desde 2026 hasta hoy.", viewModel.uiState.value.errorDeFecha)
        assertFalse(viewModel.uiState.value.puedeConfirmar)
    }

    @Test
    fun fechaAnteriorALaEntradaDelCafe_muestraErrorYNoDejaConfirmar() {
        val viewModel = ventaLista()
        // El sublote 5 entro el 20 de octubre de 2026
        viewModel.cambiarFecha(fechaAMilis(Fecha(19, 10, 2026)))
        assertEquals(
            "La fecha no puede ser anterior a la entrada del café (20 de octubre de 2026).",
            viewModel.uiState.value.errorDeFecha
        )
        assertFalse(viewModel.uiState.value.puedeConfirmar)
    }

    @Test
    fun fechaDelDiaDeEntrada_sePuedeConfirmar() {
        val viewModel = ventaLista()
        viewModel.cambiarFecha(fechaAMilis(Fecha(20, 10, 2026)))
        assertNull(viewModel.uiState.value.errorDeFecha)
        assertTrue(viewModel.uiState.value.puedeConfirmar)
    }

    @Test
    fun arreglarLaFecha_vuelveAHabilitarConfirmar() {
        val viewModel = ventaLista()
        viewModel.cambiarFecha(fechaAMilis(Fecha(1, 1, 2099)))
        viewModel.cambiarFecha(fechaAMilis(Fecha(25, 10, 2026)))
        assertTrue(viewModel.uiState.value.puedeConfirmar)
    }

    // ---------- Resumen del cafe ----------

    @Test
    fun cafeEnResumen_unSublote_comoEnElFigma() {
        val cafe = ventaLista().uiState.value.cafesEnResumen.single()
        assertEquals("Seco", cafe.tipoCafe)
        assertEquals("Seco · Sublote 5", cafe.titulo)
        assertEquals("Calidad buena", cafe.calidad)
        assertEquals("150 kg × $ 20.000", cafe.detalle)
        assertEquals("$ 3.000.000", cafe.total)
    }

    @Test
    fun cafeEnResumen_unaTarjetaPorSublote() {
        val viewModel = SaleViewModel()
        viewModel.alternarTodos()
        assertEquals(4, viewModel.uiState.value.cafesEnResumen.size)
        assertEquals(
            listOf("Seco · Sublote 5", "Seco · Sublote 6", "Seco · Sublote 8", "Seco · Sublote 9"),
            viewModel.uiState.value.cafesEnResumen.map { it.titulo }
        )
    }

    @Test
    fun cafeEnResumen_sinPrecioMuestraCero() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        assertEquals("$ 0", viewModel.uiState.value.cafesEnResumen.single().total)
    }

    @Test
    fun cafeResumenTexto_unSubloteDiceCualEs() {
        assertEquals("Seco · Sublote 5", ventaLista().uiState.value.cafeResumenTexto)
    }

    @Test
    fun cafeResumenTexto_variosSublotesDiceCuantos() {
        val viewModel = SaleViewModel()
        viewModel.alternarTodos()
        assertEquals("Seco · 4 sublotes", viewModel.uiState.value.cafeResumenTexto)
    }

    // ---------- Confirmar ----------

    @Test
    fun ventaCompleta_sePuedeConfirmar() {
        assertTrue(ventaLista().uiState.value.puedeConfirmar)
    }

    @Test
    fun confirmar_marcaLaVentaComoRegistrada() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        assertTrue(viewModel.uiState.value.ventaRegistrada)
    }

    @Test
    fun confirmar_descuentaLoVendidoDelInventario() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        assertEquals(50.0, viewModel.disponibleDelSublote(5), 0.0)
        // Los otros sublotes no se tocan
        assertEquals(100.0, viewModel.disponibleDelSublote(6), 0.0)
    }

    @Test
    fun confirmar_vendiendoTodoElSubloteSeAgota_yYaNoSeOfrece() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.confirmarVenta()
        viewModel.reiniciarVenta()
        assertEquals(listOf(6L), viewModel.uiState.value.grupos.first().sublotes.map { it.sublote.sublote.id })
    }

    @Test
    fun confirmarDosVeces_soloDescuentaUna() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        viewModel.confirmarVenta()
        assertEquals(50.0, viewModel.disponibleDelSublote(5), 0.0)
    }

    @Test
    fun yaRegistrada_noSePuedeConfirmarOtraVez() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.puedeConfirmar)
    }

    @Test
    fun confirmar_sinHaberElegidoModoDeCliente_noHaceNada() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.ventaRegistrada)
        assertEquals(200.0, viewModel.disponibleDelSublote(5), 0.0)
    }

    @Test
    fun confirmar_conClienteRegistradoSinElegirNadie_noHaceNada() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.REGISTRADO)
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.ventaRegistrada)
    }

    @Test
    fun confirmar_sinSublotes_noHaceNada() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.ventaRegistrada)
    }

    @Test
    fun confirmar_sinPrecio_noHaceNada() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.elegir(5)
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.ventaRegistrada)
    }

    @Test
    fun confirmar_conCantidadInvalida_noHaceNada() {
        val viewModel = ventaLista()
        viewModel.escribirCantidad(5, "250")
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.ventaRegistrada)
        assertEquals(200.0, viewModel.disponibleDelSublote(5), 0.0)
    }

    @Test
    fun confirmar_conFechaFutura_noHaceNada() {
        val viewModel = ventaLista()
        viewModel.cambiarFecha(fechaAMilis(Fecha(1, 1, 2099)))
        viewModel.confirmarVenta()
        assertFalse(viewModel.uiState.value.ventaRegistrada)
    }

    @Test
    fun confirmar_conClienteNuevoValido_loRegistraYLoUsaEnLaVenta() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("Ana Gómez")
        viewModel.continuarPaso1()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.confirmarVenta()
        assertTrue(viewModel.uiState.value.ventaRegistrada)
        assertEquals("Ana Gómez", viewModel.uiState.value.clienteTexto)
    }

    // ---------- Despues de registrar ----------

    @Test
    fun alRegistrar_laPantallaFinalSigueMostrandoLaVenta() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        val estado = viewModel.uiState.value
        assertEquals("Exportadora del Sur S.A.", estado.clienteTexto)
        assertEquals("150 kg", estado.totalKgTexto)
        assertEquals("$ 3.000.000", estado.totalARecibirTexto)
        assertEquals("Seco · Sublote 5", estado.cafeResumenTexto)
    }

    @Test
    fun registrarNuevaVenta_empiezaDeCero() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        viewModel.reiniciarVenta()
        val estado = viewModel.uiState.value
        assertFalse(estado.ventaRegistrada)
        assertNull(estado.cliente)
        assertNull(estado.modo)
        assertTrue(estado.elegidos.isEmpty())
        assertTrue(estado.precios.isEmpty())
        assertFalse(estado.puedeConfirmar)
    }

    @Test
    fun registrarNuevaVenta_conservaLosClientes_yElInventarioDescontado() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        viewModel.reiniciarVenta()
        assertEquals(SaleUiState().clientes, viewModel.uiState.value.clientes)
        assertEquals(50.0, viewModel.disponibleDelSublote(5), 0.0)
    }

    @Test
    fun registrarNuevaVenta_conservaUnClienteNuevoCreado() {
        val viewModel = SaleViewModel()
        viewModel.elegirModo(ModoCliente.NUEVO)
        viewModel.cambiarDocumento("1114059684")
        viewModel.cambiarNombre("Ana Gómez")
        viewModel.continuarPaso1()
        viewModel.reiniciarVenta()
        assertTrue(viewModel.uiState.value.clientes.any { it.nombre == "Ana Gómez" })
    }

    @Test
    fun cancelarLaVenta_conReiniciar_noDescuentaNada() {
        val viewModel = ventaLista()
        viewModel.reiniciarVenta()
        assertEquals(200.0, viewModel.disponibleDelSublote(5), 0.0)
        assertFalse(viewModel.uiState.value.ventaRegistrada)
    }

    @Test
    fun segundaVenta_vendeLoQueQuedaDelSublote() {
        val viewModel = ventaLista()
        viewModel.confirmarVenta()
        viewModel.reiniciarVenta()
        viewModel.elegirModo(ModoCliente.SIN_REGISTRAR)
        viewModel.elegir(5)
        // Solo quedan 50 kg del sublote 5
        assertEquals(50.0, viewModel.uiState.value.elegidos.single().cantidadKg, 0.0)
        viewModel.escribirCantidad(5, "60")
        assertEquals("Debe ser menor o igual a 50 kg.", viewModel.uiState.value.errorDeCantidad(5))
    }
}
