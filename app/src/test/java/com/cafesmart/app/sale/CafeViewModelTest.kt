package com.cafesmart.app.sale

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.SubloteConLote
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.sale.SaleUiState
import com.cafesmart.app.viewmodel.sale.SaleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Paso 2 de Ventas: pestanas, "Seleccionar todo", sublotes, cantidad y precio.
// Usa los sublotes de ejemplo (cafe seco): 5 y 6 buena (200 y 100 kg), 8 regular (150 kg) y 9 mala (71 kg).
class CafeViewModelTest {

    // ---------- Al empezar ----------

    @Test
    fun alEmpezar_laPestanaEsSeco() {
        assertEquals(TipoCafe.SECO, SaleViewModel().uiState.value.tipoCafe)
    }

    @Test
    fun alEmpezar_haySeisPestanasEnElOrdenDelFigma() {
        assertEquals(listOf(TipoCafe.VERDE, TipoCafe.SECO, TipoCafe.TRILLADO, TipoCafe.PASILLA), SaleViewModel().uiState.value.tipos)
    }

    @Test
    fun alEmpezar_nadaEstaElegido() {
        val estado = SaleViewModel().uiState.value
        assertEquals("0 de 4 seleccionados", estado.textoSeleccionarTodo)
        assertFalse(estado.todosElegidos)
        assertFalse(estado.algunosElegidos)
        assertTrue(estado.grupos.flatMap { it.sublotes }.none { it.elegido })
        assertFalse(estado.puedeContinuar)
        assertEquals("0 kg", estado.totalKgTexto)
        assertEquals("$ 0", estado.totalARecibirTexto)
    }

    @Test
    fun alEmpezar_losGruposVanPorCalidad() {
        val grupos = SaleViewModel().uiState.value.grupos
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), grupos.map { it.calidad })
        assertEquals(listOf("Calidad buena", "Calidad regular", "Calidad mala"), grupos.map { it.etiqueta })
        assertEquals(listOf("2 sublotes · 300 kg", "1 sublote · 150 kg", "1 sublote · 71 kg"), grupos.map { it.resumen })
    }

    @Test
    fun alEmpezar_ningunGrupoPideAunPrecio() {
        assertTrue(SaleViewModel().uiState.value.grupos.all { it.precio == null })
    }

    @Test
    fun losTitulosDeLosSublotesLlevanSuNumero() {
        val titulos = SaleViewModel().uiState.value.grupos.flatMap { it.sublotes }.map { it.titulo }
        assertEquals(listOf("Sublote 5", "Sublote 6", "Sublote 8", "Sublote 9"), titulos)
    }

    @Test
    fun elDetalleDelSubloteDiceCuantoHayYHaceCuantoLlego() {
        // El sublote 5 entro el 20 de octubre de 2026: el 26 de octubre hace 6 dias
        val estado = SaleUiState(hoy = Fecha(26, 10, 2026))
        assertEquals("200 kg disponibles · Comprado hace 6 días", estado.grupos.first().sublotes.first().detalle)
    }

    @Test
    fun elDetalleDeUnSubloteDeHoy() {
        val estado = SaleUiState(hoy = Fecha(20, 10, 2026))
        assertEquals("200 kg disponibles · Comprado hoy", estado.grupos.first().sublotes.first().detalle)
    }

    // ---------- Pestanas ----------

    @Test
    fun cambiarDePestana_muestraLosSublotesDeEseTipo() {
        val viewModel = SaleViewModel()
        viewModel.elegirTipoCafe(TipoCafe.VERDE)
        val estado = viewModel.uiState.value
        assertEquals(TipoCafe.VERDE, estado.tipoCafe)
        assertEquals(listOf(1L, 2L, 3L, 4L), estado.grupos.flatMap { it.sublotes }.map { it.sublote.sublote.id })
        assertEquals("0 de 4 seleccionados", estado.textoSeleccionarTodo)
    }

    @Test
    fun cambiarDePestana_borraLoElegidoYLosPrecios() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "150")
        viewModel.elegirTipoCafe(TipoCafe.VERDE)
        val estado = viewModel.uiState.value
        assertTrue(estado.elegidos.isEmpty())
        assertTrue(estado.precios.isEmpty())
        assertTrue(estado.textosCantidad.isEmpty())
        assertFalse(estado.puedeContinuar)
    }

    @Test
    fun tocarLaPestanaQueYaEstaActiva_noBorraNada() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.elegirTipoCafe(TipoCafe.SECO)
        assertEquals(1, viewModel.uiState.value.elegidos.size)
        assertEquals("20000", viewModel.uiState.value.precios[Calidad.BUENA])
    }

    @Test
    fun pestanaSinCafe_muestraMensajeYNoTieneGrupos() {
        val viewModel = SaleViewModel()
        viewModel.elegirTipoCafe(TipoCafe.PASILLA)
        val estado = viewModel.uiState.value
        assertEquals("No tienes café pasilla disponible para vender.", estado.mensajeSinCafe)
        assertTrue(estado.grupos.isEmpty())
        assertEquals("0 de 0 seleccionados", estado.textoSeleccionarTodo)
    }

    @Test
    fun pestanaSinCafe_seleccionarTodoNoElegirNada() {
        val viewModel = SaleViewModel()
        viewModel.elegirTipoCafe(TipoCafe.PASILLA)
        viewModel.alternarTodos()
        assertTrue(viewModel.uiState.value.elegidos.isEmpty())
        assertFalse(viewModel.uiState.value.todosElegidos)
    }

    @Test
    fun pestanaConCafe_noMuestraMensajeDeVacio() {
        assertNull(SaleViewModel().uiState.value.mensajeSinCafe)
    }

    // ---------- Seleccionar todo ----------

    @Test
    fun seleccionarTodo_eligeLosCuatroConTodoLoDisponible() {
        val viewModel = SaleViewModel()
        viewModel.alternarTodos()
        val estado = viewModel.uiState.value
        assertTrue(estado.todosElegidos)
        assertFalse(estado.algunosElegidos)
        assertEquals("4 de 4 seleccionados", estado.textoSeleccionarTodo)
        assertEquals("521 kg", estado.totalKgTexto)
    }

    @Test
    fun seleccionarTodo_otraVezLosQuitaTodos() {
        val viewModel = SaleViewModel()
        viewModel.alternarTodos()
        viewModel.alternarTodos()
        val estado = viewModel.uiState.value
        assertTrue(estado.elegidos.isEmpty())
        assertEquals("0 de 4 seleccionados", estado.textoSeleccionarTodo)
    }

    @Test
    fun conAlgunosElegidos_laCasillaEstaAMedias() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        val estado = viewModel.uiState.value
        assertTrue(estado.algunosElegidos)
        assertFalse(estado.todosElegidos)
        assertEquals("1 de 4 seleccionados", estado.textoSeleccionarTodo)
    }

    @Test
    fun conAlgunosElegidos_seleccionarTodoCompletaLosQueFaltan() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.alternarTodos()
        assertTrue(viewModel.uiState.value.todosElegidos)
        assertEquals(4, viewModel.uiState.value.elegidos.size)
    }

    @Test
    fun seleccionarTodo_respetaLaCantidadYaCambiada() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "150")
        viewModel.alternarTodos()
        val estado = viewModel.uiState.value
        assertEquals(150.0, estado.elegidos.first { it.id == 5L }.cantidadKg, 0.0)
        // 150 del sublote 5 + 100 + 150 + 71 de los otros tres
        assertEquals(150.0 + 321.0, estado.totalKg, 0.0)
    }

    @Test
    fun desmarcarUnoDespuesDeSeleccionarTodo_dejaLaCasillaAMedias() {
        val viewModel = SaleViewModel()
        viewModel.alternarTodos()
        viewModel.alternarSublote(viewModel.sublote(9))
        assertTrue(viewModel.uiState.value.algunosElegidos)
        assertEquals("3 de 4 seleccionados", viewModel.uiState.value.textoSeleccionarTodo)
    }

    // ---------- Elegir sublotes ----------

    @Test
    fun alElegirUnSublote_aparecenSuCantidadYElPrecioDeSuCalidad() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        val grupos = viewModel.uiState.value.grupos
        val buena = grupos.first { it.calidad == Calidad.BUENA }
        assertTrue(buena.sublotes.first { it.sublote.sublote.id == 5L }.elegido)
        assertEquals("200 kg", buena.sublotes.first { it.sublote.sublote.id == 5L }.cantidadTexto)
        assertNotNull(buena.precio)
        assertEquals("Precio por kg · calidad buena", buena.precio!!.titulo)
        // Las otras calidades todavia no piden precio
        assertNull(grupos.first { it.calidad == Calidad.REGULAR }.precio)
        assertNull(grupos.first { it.calidad == Calidad.MALA }.precio)
    }

    @Test
    fun dosSublotesDeLaMismaCalidadComparteUnSoloPrecio() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5, 6)
        assertEquals(1, viewModel.uiState.value.grupos.count { it.precio != null })
        assertEquals(listOf(Calidad.BUENA), viewModel.uiState.value.calidadesElegidas)
    }

    @Test
    fun quitarElUltimoSubloteDeUnaCalidad_haceDesaparecerSuPrecio() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.alternarSublote(viewModel.sublote(5))
        assertTrue(viewModel.uiState.value.grupos.all { it.precio == null })
    }

    @Test
    fun laAyudaDelPrecio_traeElCostoPromedioDeLoQueSeVende() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        // El sublote 5 costo $ 21.000 por kg
        assertEquals(
            "Entre $ 1.000 y $ 100.000. Costo promedio: $ 21.000 por kg.",
            viewModel.uiState.value.grupos.first { it.calidad == Calidad.BUENA }.precio!!.ayuda
        )
    }

    // ---------- Cantidad a vender ----------

    @Test
    fun cantidad_dentroDeLoDisponibleSeAcepta() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "150")
        val fila = viewModel.uiState.value.grupos.first().sublotes.first()
        assertEquals("150 kg", fila.cantidadTexto)
        assertEquals("150", fila.cantidadCampo)
        assertNull(fila.errorCantidad)
    }

    @Test
    fun cantidad_quePasaLoDisponibleMuestraError_yNoCambiaLoElegido() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "250")
        val fila = viewModel.uiState.value.grupos.first().sublotes.first()
        assertEquals("Debe ser menor o igual a 200 kg.", fila.errorCantidad)
        assertEquals("250", fila.cantidadCampo)
        assertEquals("200 kg", fila.cantidadTexto)
    }

    @Test
    fun cantidad_conError_impideContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "250")
        assertFalse(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun cantidad_exactaALoDisponibleSeAcepta() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "200")
        assertNull(viewModel.uiState.value.errorDeCantidad(5))
    }

    @Test
    fun cantidad_unGramoDeMasMuestraError() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "200,001")
        assertEquals("Debe ser menor o igual a 200 kg.", viewModel.uiState.value.errorDeCantidad(5))
    }

    @Test
    fun cantidad_laFilaDiceSiLoEscritoSirve() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        val sirve = { viewModel.uiState.value.grupos.first().sublotes.first().cantidadValida }
        assertTrue(sirve())
        viewModel.escribirCantidad(5, "250")
        assertFalse(sirve())
        viewModel.escribirCantidad(5, "0")
        assertFalse(sirve())
        viewModel.escribirCantidad(5, "")
        assertFalse(sirve())
        viewModel.escribirCantidad(5, "100")
        assertTrue(sirve())
    }

    @Test
    fun cantidad_cero_noMuestraErrorPeroNoDejaContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "0")
        assertNull(viewModel.uiState.value.errorDeCantidad(5))
        assertFalse(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun cantidad_vacia_noDejaContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "")
        assertFalse(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun cantidad_letrasYSimbolosSeQuitan() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "abc")
        assertEquals("", viewModel.uiState.value.textoCantidad(5))
        viewModel.escribirCantidad(5, "1a5#0")
        assertEquals("150", viewModel.uiState.value.textoCantidad(5))
    }

    @Test
    fun cantidad_negativaNoExiste_elSignoSeQuita() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "-50")
        assertEquals("50", viewModel.uiState.value.textoCantidad(5))
    }

    @Test
    fun cantidad_aceptaComaDecimal() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "12,5")
        assertEquals(12.5, viewModel.uiState.value.elegidos.first().cantidadKg, 0.0)
    }

    @Test
    fun cantidad_arreglaUnError_yVuelveAContar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "250")
        viewModel.escribirCantidad(5, "100")
        assertNull(viewModel.uiState.value.errorDeCantidad(5))
        assertTrue(viewModel.uiState.value.puedeContinuar)
        assertEquals(100.0, viewModel.uiState.value.totalKg, 0.0)
    }

    @Test
    fun cantidad_cadaSubloteTieneSuPropioMaximo() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5, 6)
        // 150 sirve para el sublote 5 (200 kg) pero no para el 6 (100 kg)
        viewModel.escribirCantidad(5, "150")
        viewModel.escribirCantidad(6, "150")
        assertNull(viewModel.uiState.value.errorDeCantidad(5))
        assertEquals("Debe ser menor o igual a 100 kg.", viewModel.uiState.value.errorDeCantidad(6))
    }

    @Test
    fun cantidad_alDesmarcarYVolverAMarcar_empiezaDeNuevoConTodo() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "50")
        viewModel.alternarSublote(viewModel.sublote(5))
        viewModel.alternarSublote(viewModel.sublote(5))
        assertEquals(200.0, viewModel.uiState.value.elegidos.first().cantidadKg, 0.0)
    }

    // ---------- Minimo de una venta (5 kg en total) ----------

    @Test
    fun total_menosDeCincoKg_muestraErrorYNoDejaContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "4,5")
        assertEquals("La cantidad total a vender debe ser mínimo 5 kg.", viewModel.uiState.value.errorDeTotal)
        assertFalse(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun total_cincoKgExactos_dejaContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "5")
        assertNull(viewModel.uiState.value.errorDeTotal)
        assertTrue(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun total_dosSublotesQueSumanCincoKg_sirven() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5, 6)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "2")
        viewModel.escribirCantidad(6, "3")
        assertNull(viewModel.uiState.value.errorDeTotal)
        assertTrue(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun total_sinNadaElegido_noMuestraErrorDeMinimo() {
        assertNull(SaleViewModel().uiState.value.errorDeTotal)
    }

    // ---------- Precio por kg ----------

    @Test
    fun precio_seMuestraConPuntoDeMiles() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        assertEquals("20.000", viewModel.uiState.value.grupos.first().precio!!.texto)
    }

    @Test
    fun precio_letrasYSimbolosSeQuitan() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "2a0.0,0$0")
        assertEquals("20000", viewModel.uiState.value.precios[Calidad.BUENA])
    }

    @Test
    fun precio_maximoSeisDigitos() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "12345678")
        assertEquals("123456", viewModel.uiState.value.precios[Calidad.BUENA])
    }

    @Test
    fun precio_conTresDigitosNoMarcaErrorMientrasSeEscribe_peroNoSirve() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "999")
        assertNull(viewModel.uiState.value.grupos.first().precio!!.error)
        assertFalse(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun precio_pocoConCuatroDigitosMuestraError() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "0999")
        assertNotNull(viewModel.uiState.value.grupos.first().precio!!.error)
    }

    @Test
    fun precio_demasiadoMuestraError() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "100001")
        assertNotNull(viewModel.uiState.value.grupos.first().precio!!.error)
    }

    @Test
    fun precio_losLimitesSeAceptan() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "1000")
        assertNull(viewModel.uiState.value.grupos.first().precio!!.error)
        viewModel.cambiarPrecio(Calidad.BUENA, "100000")
        assertNull(viewModel.uiState.value.grupos.first().precio!!.error)
    }

    @Test
    fun precio_cadaCalidadTieneElSuyo() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5, 8)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.cambiarPrecio(Calidad.REGULAR, "18000")
        val grupos = viewModel.uiState.value.grupos
        assertEquals("20.000", grupos.first { it.calidad == Calidad.BUENA }.precio!!.texto)
        assertEquals("18.000", grupos.first { it.calidad == Calidad.REGULAR }.precio!!.texto)
    }

    @Test
    fun sinPrecio_noSePuedeContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        assertFalse(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun falta_elPrecioDeUnaCalidad_noSePuedeContinuar() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5, 8)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        assertFalse(viewModel.uiState.value.puedeContinuar)
        viewModel.cambiarPrecio(Calidad.REGULAR, "18000")
        assertTrue(viewModel.uiState.value.puedeContinuar)
    }

    // ---------- Totales (cifras del Figma) ----------

    @Test
    fun total_ciento50KgPor20000SonTresMillones() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.escribirCantidad(5, "150")
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        val estado = viewModel.uiState.value
        assertEquals("150 kg", estado.totalKgTexto)
        assertEquals("$ 3.000.000", estado.totalARecibirTexto)
    }

    @Test
    fun total_deTodoElCafeSecoConPrecioPorCalidad() {
        val viewModel = SaleViewModel()
        viewModel.alternarTodos()
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.cambiarPrecio(Calidad.REGULAR, "18000")
        viewModel.cambiarPrecio(Calidad.MALA, "14000")
        // 300 x 20.000 + 150 x 18.000 + 71 x 14.000 = 9.694.000
        assertEquals("$ 9.694.000", viewModel.uiState.value.totalARecibirTexto)
        assertEquals("521 kg", viewModel.uiState.value.totalKgTexto)
        assertTrue(viewModel.uiState.value.puedeContinuar)
    }

    @Test
    fun total_sinPrecioNoSumaNada() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        assertEquals("$ 0", viewModel.uiState.value.totalARecibirTexto)
        assertEquals("200 kg", viewModel.uiState.value.totalKgTexto)
    }

    @Test
    fun total_cambiaAlCambiarLaCantidad() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "20000")
        viewModel.escribirCantidad(5, "100")
        assertEquals("$ 2.000.000", viewModel.uiState.value.totalARecibirTexto)
    }

    @Test
    fun total_conDecimalesSeRedondeaAPesos() {
        val viewModel = SaleViewModel()
        viewModel.elegir(5)
        viewModel.cambiarPrecio(Calidad.BUENA, "15555")
        viewModel.escribirCantidad(5, "2,5")
        // 2,5 x 15.555 = 38.887,5 -> 38.888
        assertEquals("$ 38.888", viewModel.uiState.value.totalARecibirTexto)
    }
}
