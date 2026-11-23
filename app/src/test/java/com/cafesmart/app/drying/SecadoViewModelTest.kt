package com.cafesmart.app.drying

import com.cafesmart.app.data.model.Calidad
import com.cafesmart.app.data.model.Fecha
import com.cafesmart.app.data.model.TipoCafe
import com.cafesmart.app.viewmodel.drying.MENSAJE_ELEGIR_SUBLOTE_SECADO
import com.cafesmart.app.viewmodel.drying.MENSAJE_SIN_CAFE_VERDE
import com.cafesmart.app.viewmodel.drying.SecadoUiState
import com.cafesmart.app.viewmodel.drying.SecadoViewModel
import com.cafesmart.app.viewmodel.purchase.fechaAMilis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecadoViewModelTest {

    private val repositorio = repositorioDePrueba()
    private val vm = SecadoViewModel(repositorio)
    private val estado get() = vm.uiState.value

    private fun sublote(id: Long) = estado.inventario.first { it.sublote.id == id }

    private fun disponibleEnRepositorio(id: Long): Double =
        repositorio.inventario().first { it.sublote.id == id }.sublote.pesoDisponibleKg

    // ---------- Paso 1: elegir el cafe ----------

    @Test
    fun alEntrar_nadaElegido_noSePuedeContinuarYDiceQueFalta() {
        assertTrue(estado.elegidos.isEmpty())
        assertFalse(estado.puedeContinuar)
        assertEquals(MENSAJE_ELEGIR_SUBLOTE_SECADO, estado.mensajePendientePaso1)
        assertEquals("0 kg", estado.totalKgTexto)
    }

    @Test
    fun soloAparecenLosSublotesVerdes_agrupadosPorCalidad() {
        val ids = estado.grupos.flatMap { grupo -> grupo.sublotes.map { it.sublote.sublote.id } }
        // El sublote 5 es seco: no se ofrece.
        assertEquals(listOf(3L, 1L, 2L, 4L), ids)
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), estado.grupos.map { it.calidad })
        assertEquals("0 de 4 seleccionados", estado.textoSeleccionarTodo)
        assertNull(estado.mensajeSinCafe)
    }

    @Test
    fun cadaGrupoMuestraSuEtiquetaYSuResumen() {
        val regular = estado.grupos.first { it.calidad == Calidad.REGULAR }
        assertEquals("Calidad regular", regular.etiqueta)
        assertEquals("2 sublotes · 200 kg", regular.resumen)
        val buena = estado.grupos.first { it.calidad == Calidad.BUENA }
        assertEquals("1 sublote · 60 kg", buena.resumen)
    }

    @Test
    fun cadaSubloteDiceSusKgDisponiblesYCuantoHaceQueSeCompro() {
        // Con "hoy" del Figma (3 de junio de 2026): el sublote 1 se compro hoy, el 3 hace 3 dias y el 4 hace 5.
        val figma = SecadoUiState(inventario = inventarioFigma(), hoy = HOY_FIGMA)
        val filas = figma.grupos.flatMap { it.sublotes }.associateBy { it.sublote.sublote.id }
        assertEquals("Sublote 3", filas.getValue(3).titulo)
        assertEquals("60 kg disponibles · Comprado hace 3 días", filas.getValue(3).detalle)
        assertEquals("120 kg disponibles · Comprado hoy", filas.getValue(1).detalle)
        assertEquals("80 kg disponibles · Comprado hace 2 días", filas.getValue(2).detalle)
        assertEquals("30 kg disponibles · Comprado hace 5 días", filas.getValue(4).detalle)
    }

    @Test
    fun alternarTodos_eligeLosCuatroYElTotalEsElDelFigma() {
        vm.alternarTodos()
        assertEquals(4, estado.elegidos.size)
        assertTrue(estado.todosElegidos)
        assertEquals("4 de 4 seleccionados", estado.textoSeleccionarTodo)
        assertEquals(290.0, estado.totalKg, 0.0)
        assertEquals("290 kg", estado.totalKgTexto)
        assertTrue(estado.puedeContinuar)
        assertNull(estado.mensajePendientePaso1)
    }

    @Test
    fun alternarTodos_conTodosElegidosLosQuita() {
        vm.alternarTodos()
        vm.alternarTodos()
        assertTrue(estado.elegidos.isEmpty())
        assertFalse(estado.puedeContinuar)
    }

    @Test
    fun alternarTodos_conAlgunosElegidosLosCompletaYRespetaLaCantidadYaAjustada() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("50")
        vm.confirmarAjuste()
        vm.alternarTodos()
        assertEquals(4, estado.elegidos.size)
        assertEquals(50.0, estado.elegidos.first { it.id == 1L }.cantidadKg, 0.0)
        assertEquals(50.0 + 80.0 + 60.0 + 30.0, estado.totalKg, 0.0)
    }

    @Test
    fun alternarSublote_alElegirloSeMandaTodoLoDisponible() {
        vm.alternarSublote(sublote(1))
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)
        assertTrue(estado.algunosElegidos)
        assertFalse(estado.todosElegidos)
        assertEquals("1 de 4 seleccionados", estado.textoSeleccionarTodo)
        assertEquals("120 kg", estado.totalKgTexto)
    }

    @Test
    fun alternarSublote_alTocarloDeNuevoLoQuita() {
        vm.alternarSublote(sublote(1))
        vm.alternarSublote(sublote(1))
        assertTrue(estado.elegidos.isEmpty())
        assertFalse(estado.algunosElegidos)
    }

    @Test
    fun sinCafeVerde_diceQueNoHayYNoSePuedeContinuar() {
        val soloSeco = SecadoViewModel(repositorioDePrueba(listOf(subloteDePrueba(5, TipoCafe.SECO, Calidad.BUENA, 200.0))))
        val e = soloSeco.uiState.value
        assertEquals(MENSAJE_SIN_CAFE_VERDE, e.mensajeSinCafe)
        assertTrue(e.grupos.isEmpty())
        assertFalse(e.todosElegidos)
        assertFalse(e.puedeContinuar)
    }

    @Test
    fun conInventarioVacio_tambienDiceQueNoHayCafe() {
        val vacio = SecadoViewModel(repositorioDePrueba(emptyList())).uiState.value
        assertEquals(MENSAJE_SIN_CAFE_VERDE, vacio.mensajeSinCafe)
        assertFalse(vacio.puedeContinuar)
    }

    // ---------- Hoja "Ajustar cantidad" ----------

    @Test
    fun cambiar_abreLaHojaConLaCantidadElegidaYLoDisponible() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        val ajuste = estado.ajusteEnPantalla!!
        assertEquals("Sublote 1", ajuste.titulo)
        assertEquals("120 kg", ajuste.disponibleTexto)
        assertEquals("120", ajuste.texto)
        assertEquals("Debe ser menor o igual a 120 kg.", ajuste.ayuda)
        assertNull(ajuste.error)
        assertTrue(ajuste.valido)
    }

    @Test
    fun cambiar_enUnSubloteNoElegidoNoAbreNada() {
        vm.abrirAjuste(1)
        assertNull(estado.ajuste)
        assertNull(estado.ajusteEnPantalla)
    }

    @Test
    fun confirmar_unaParteValida_cambiaLaCantidadYElTotalYCierraLaHoja() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("50")
        vm.confirmarAjuste()
        assertEquals(50.0, estado.elegidos.single().cantidadKg, 0.0)
        assertEquals("50 kg", estado.totalKgTexto)
        assertNull(estado.ajusteEnPantalla)
        assertTrue(estado.puedeContinuar)
    }

    @Test
    fun confirmar_todoElSublote_esValido() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("120")
        assertTrue(estado.ajusteEnPantalla!!.valido)
        vm.confirmarAjuste()
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)
    }

    @Test
    fun ajuste_masDeLoDisponible_muestraElErrorYNoSePuedeConfirmar() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("130")
        val ajuste = estado.ajusteEnPantalla!!
        assertEquals("Debe ser menor o igual a 120 kg.", ajuste.error)
        assertFalse(ajuste.valido)
        vm.confirmarAjuste()
        // Sigue la cantidad de antes y la hoja sigue abierta.
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)
        assertNotNull(estado.ajusteEnPantalla)
    }

    @Test
    fun ajuste_ceroOVacio_noSePuedeConfirmarPeroNoMuestraError() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("0")
        assertFalse(estado.ajusteEnPantalla!!.valido)
        assertNull(estado.ajusteEnPantalla!!.error)
        vm.confirmarAjuste()
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)

        vm.escribirAjuste("")
        assertFalse(estado.ajusteEnPantalla!!.valido)
        assertNull(estado.ajusteEnPantalla!!.error)
        vm.confirmarAjuste()
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)
    }

    @Test
    fun ajuste_conComaDecimal_seMuestraConComaYSeGuardaComoNumero() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("12,5")
        assertEquals("12,5", estado.ajusteEnPantalla!!.texto)
        vm.confirmarAjuste()
        assertEquals(12.5, estado.elegidos.single().cantidadKg, 0.0)
        assertEquals("12,5 kg", estado.totalKgTexto)
    }

    @Test
    fun ajuste_limpiaLetrasYDejaSoloTresDecimales() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("ab5,1234")
        assertEquals("5,123", estado.ajusteEnPantalla!!.texto)
    }

    @Test
    fun cancelarElAjuste_descartaLoEscritoYDejaLaCantidadComoEstaba() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("50")
        vm.cancelarAjuste()
        assertNull(estado.ajusteEnPantalla)
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)
        // Al volver a abrir, el campo muestra la cantidad elegida y no lo que se habia escrito.
        vm.abrirAjuste(1)
        assertEquals("120", estado.ajusteEnPantalla!!.texto)
    }

    @Test
    fun sinHojaAbierta_escribirOConfirmarNoHaceNada() {
        vm.alternarSublote(sublote(1))
        vm.escribirAjuste("50")
        vm.confirmarAjuste()
        assertEquals(120.0, estado.elegidos.single().cantidadKg, 0.0)
        assertNull(estado.ajuste)
    }

    // ---------- Paso 2: confirmar el inicio ----------

    @Test
    fun resumenDelPaso2_conLosCuatroSublotes() {
        vm.alternarTodos()
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), estado.calidadesElegidas)
        assertEquals("4 sublotes", estado.sublotesElegidosTexto)
        assertEquals("290 kg", estado.totalKgTexto)
        assertTrue(estado.fechaTexto.startsWith("Hoy, "))
    }

    @Test
    fun resumenDelPaso2_conUnSoloSubloteDiceSublote_ySoloSuCalidad() {
        vm.alternarSublote(sublote(1))
        assertEquals("1 sublote", estado.sublotesElegidosTexto)
        assertEquals(listOf(Calidad.REGULAR), estado.calidadesElegidas)
    }

    @Test
    fun cambiarFecha_actualizaLaFechaDeInicio() {
        vm.cambiarFecha(fechaAMilis(Fecha(5, 6, 2026)))
        assertEquals(Fecha(5, 6, 2026), estado.fechaInicio)
        assertEquals("5 de junio de 2026", estado.fechaResumenTexto)
        // Ya no es hoy: no lleva el "Hoy, ".
        assertEquals("5 de junio de 2026", estado.fechaTexto)
    }

    @Test
    fun iniciarSinElegirNada_noHaceNada() {
        vm.iniciarSecado()
        assertFalse(estado.secadoIniciado)
        assertEquals(120.0, disponibleEnRepositorio(1), 0.0)
        assertFalse(estado.puedeIniciar)
    }

    @Test
    fun iniciarSecado_conTodo_descuentaDelInventarioYQuedaIniciado() {
        vm.alternarTodos()
        assertTrue(estado.puedeIniciar)
        vm.iniciarSecado()
        assertTrue(estado.secadoIniciado)
        assertFalse(estado.puedeIniciar)
        // Los cuatro sublotes verdes quedaron en cero; el seco no se toco.
        listOf(1L, 2L, 3L, 4L).forEach { assertEquals(0.0, disponibleEnRepositorio(it), 0.0) }
        assertEquals(200.0, disponibleEnRepositorio(5), 0.0)
        // El estado ya no ofrece cafe verde.
        assertEquals(MENSAJE_SIN_CAFE_VERDE, estado.mensajeSinCafe)
    }

    @Test
    fun iniciarSecado_conlaCantidadAjustada_soloDescuentaEsaParte() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("50")
        vm.confirmarAjuste()
        vm.iniciarSecado()
        assertEquals(70.0, disponibleEnRepositorio(1), 0.0)
        // El sublote sigue ofreciendose, con lo que le queda.
        val fila = estado.grupos.flatMap { it.sublotes }.first { it.sublote.sublote.id == 1L }
        assertTrue(fila.detalle.startsWith("70 kg disponibles"))
    }

    @Test
    fun iniciarSecado_conservaLoElegidoParaElResumenDelPaso3() {
        vm.alternarTodos()
        vm.iniciarSecado()
        assertEquals("290 kg", estado.totalKgTexto)
        assertEquals("4 sublotes", estado.sublotesElegidosTexto)
        assertEquals(listOf(Calidad.BUENA, Calidad.REGULAR, Calidad.MALA), estado.calidadesElegidas)
    }

    @Test
    fun iniciarSecado_dosVeces_soloDescuentaUna() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("50")
        vm.confirmarAjuste()
        vm.iniciarSecado()
        vm.iniciarSecado()
        assertEquals(70.0, disponibleEnRepositorio(1), 0.0)
    }

    @Test
    fun iniciarSecado_laFechaDeInicioSeGuardaEnElSecado() {
        vm.alternarSublote(sublote(1))
        vm.cambiarFecha(fechaAMilis(Fecha(5, 6, 2026)))
        vm.iniciarSecado()
        assertEquals(Fecha(5, 6, 2026), estado.fechaInicio)
        assertTrue(estado.secadoIniciado)
    }

    // ---------- Empezar otro secado ----------

    @Test
    fun reiniciar_dejaElPasoUnoLimpioYConElInventarioAlDia() {
        vm.alternarSublote(sublote(1))
        vm.abrirAjuste(1)
        vm.escribirAjuste("50")
        vm.confirmarAjuste()
        vm.iniciarSecado()
        vm.reiniciar()
        assertTrue(estado.elegidos.isEmpty())
        assertFalse(estado.secadoIniciado)
        assertNull(estado.ajuste)
        // El sublote 1 muestra lo que quedo despues del primer secado.
        assertEquals(70.0, sublote(1).sublote.pesoDisponibleKg, 0.0)
        assertEquals("0 de 4 seleccionados", estado.textoSeleccionarTodo)
    }

    @Test
    fun cancelarElSecado_conReiniciar_noDescuentaNada() {
        vm.alternarTodos()
        vm.reiniciar()
        assertTrue(estado.elegidos.isEmpty())
        listOf(1L, 2L, 3L, 4L).forEach { assertTrue(disponibleEnRepositorio(it) > 0.0) }
    }

    @Test
    fun unSubloteAgotadoPorUnSecadoAnteriorYaNoSeOfrece() {
        vm.alternarSublote(sublote(4))
        vm.iniciarSecado()
        vm.reiniciar()
        val ids = estado.grupos.flatMap { grupo -> grupo.sublotes.map { it.sublote.sublote.id } }
        assertEquals(listOf(3L, 1L, 2L), ids)
        assertEquals("0 de 3 seleccionados", estado.textoSeleccionarTodo)
    }
}
